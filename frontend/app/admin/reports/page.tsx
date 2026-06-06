"use client";

import { useMemo, useState } from "react";
import { useQueries, useQuery } from "@tanstack/react-query";
import { Banknote, CircleGauge, CreditCard, LogOut, TicketX } from "lucide-react";
import { adminApi } from "@/lib/api/admin";
import { formatDate, formatPrice, formatTime } from "@/lib/format";
import { PageHeader, Pagination, TableState } from "@/components/admin/AdminPrimitives";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

type ReportKey = "occupancy" | "revenue" | "cancelled" | "popular" | "conversion";
type ReportState = Record<ReportKey, { page: number; sort: string }>;

const initialReportState: ReportState = {
  occupancy: { page: 0, sort: "startTime,asc" },
  revenue: { page: 0, sort: "revenue,desc" },
  cancelled: { page: 0, sort: "cancelledAt,desc" },
  popular: { page: 0, sort: "bookingCount,desc" },
  conversion: { page: 0, sort: "checkoutCount,desc" },
};

const sortOptions: Record<ReportKey, { label: string; value: string }[]> = {
  occupancy: [
    { label: "Start time", value: "startTime,asc" },
    { label: "Highest occupancy", value: "occupancyRate,desc" },
    { label: "Most reserved seats", value: "reservedSeats,desc" },
  ],
  revenue: [
    { label: "Highest revenue", value: "revenue,desc" },
    { label: "Most tickets", value: "ticketsSold,desc" },
    { label: "Movie title", value: "movieTitle,asc" },
  ],
  cancelled: [
    { label: "Recently cancelled", value: "cancelledAt,desc" },
    { label: "Highest value", value: "totalPrice,desc" },
    { label: "Showtime date", value: "showtimeStartTime,asc" },
  ],
  popular: [
    { label: "Most booked", value: "bookingCount,desc" },
    { label: "Highest revenue", value: "revenue,desc" },
    { label: "Seat row", value: "rowLabel,asc" },
  ],
  conversion: [
    { label: "Most checkouts", value: "checkoutCount,desc" },
    { label: "Highest conversion", value: "conversionRate,desc" },
    { label: "Highest abandonment", value: "abandonedRate,desc" },
  ],
};

function dateDaysAgo(days: number) {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return date.toISOString().slice(0, 10);
}

export default function AdminReportsPage() {
  const [from, setFrom] = useState(dateDaysAgo(30));
  const [to, setTo] = useState(new Date().toISOString().slice(0, 10));
  const [movieId, setMovieId] = useState("");
  const [theatreId, setTheatreId] = useState("");
  const [screenId, setScreenId] = useState("");
  const [showtimeId, setShowtimeId] = useState("");
  const [reports, setReports] = useState(initialReportState);

  const baseParams = useMemo(() => ({
    from: `${from}T00:00:00`,
    to: `${to}T23:59:59`,
    movieId: movieId || undefined,
    theatreId: theatreId || undefined,
    screenId: screenId || undefined,
    showtimeId: showtimeId || undefined,
    size: 20,
  }), [from, to, movieId, theatreId, screenId, showtimeId]);

  const paramsFor = (key: ReportKey) => ({ ...baseParams, ...reports[key] });
  const [occupancy, revenue, cancelled, popular, conversion] = useQueries({
    queries: [
      { queryKey: ["admin", "reports", "occupancy", paramsFor("occupancy")], queryFn: () => adminApi.occupancy(paramsFor("occupancy")) },
      { queryKey: ["admin", "reports", "revenue", paramsFor("revenue")], queryFn: () => adminApi.revenue(paramsFor("revenue")) },
      { queryKey: ["admin", "reports", "cancelled", paramsFor("cancelled")], queryFn: () => adminApi.cancelled(paramsFor("cancelled")) },
      { queryKey: ["admin", "reports", "popular", paramsFor("popular")], queryFn: () => adminApi.popularSeats(paramsFor("popular")) },
      { queryKey: ["admin", "reports", "conversion", paramsFor("conversion")], queryFn: () => adminApi.conversion(paramsFor("conversion")) },
    ],
  });

  const movies = useQuery({ queryKey: ["admin", "reports", "movie-options"], queryFn: () => adminApi.movies({ size: 100, sort: "title,asc" }) });
  const theatres = useQuery({ queryKey: ["admin", "reports", "theatre-options"], queryFn: () => adminApi.theatres({ size: 100, sort: "name,asc" }) });
  const screens = useQuery({
    queryKey: ["admin", "reports", "screen-options", theatreId],
    queryFn: () => adminApi.screens({ size: 100, sort: "name,asc", theatreId: theatreId || undefined }),
  });
  const showtimes = useQuery({
    queryKey: ["admin", "reports", "showtime-options", movieId, theatreId, screenId, from, to],
    queryFn: () => adminApi.showtimes({
      size: 100,
      sort: "startTime,asc",
      movieId: movieId || undefined,
      theatreId: theatreId || undefined,
      screenId: screenId || undefined,
      from: `${from}T00:00:00`,
      to: `${to}T23:59:59`,
    }),
  });

  const resetPages = () => setReports((current) => Object.fromEntries(
    Object.entries(current).map(([key, value]) => [key, { ...value, page: 0 }]),
  ) as ReportState);
  const updateReport = (key: ReportKey, value: Partial<ReportState[ReportKey]>) =>
    setReports((current) => ({ ...current, [key]: { ...current[key], ...value } }));

  const totalRevenue = revenue.data?.content.reduce((sum, row) => sum + Number(row.revenue), 0) ?? 0;
  const averageOccupancy = occupancy.data?.content.length
    ? occupancy.data.content.reduce((sum, row) => sum + Number(row.occupancyRate), 0) / occupancy.data.content.length
    : 0;
  const totalCheckouts = conversion.data?.content.reduce((sum, row) => sum + row.checkoutCount, 0) ?? 0;
  const paidCheckouts = conversion.data?.content.reduce((sum, row) => sum + row.paidCheckoutCount, 0) ?? 0;
  const abandonedCheckouts = conversion.data?.content.reduce((sum, row) => sum + row.abandonedCheckoutCount, 0) ?? 0;
  const metrics = [
    { label: "Revenue", value: formatPrice(totalRevenue), icon: Banknote },
    { label: "Average occupancy", value: `${Math.round(averageOccupancy * 100)}%`, icon: CircleGauge },
    { label: "Checkout conversion", value: `${totalCheckouts ? Math.round((paidCheckouts / totalCheckouts) * 100) : 0}%`, icon: CreditCard },
    { label: "Abandoned checkout", value: `${totalCheckouts ? Math.round((abandonedCheckouts / totalCheckouts) * 100) : 0}%`, icon: LogOut },
    { label: "Cancelled bookings", value: cancelled.data?.totalElements ?? "—", icon: TicketX },
  ];

  return (
    <>
      <PageHeader title="Reports" description="Business performance calculated from live booking and checkout data." />
      <section className="mb-6 grid gap-4 rounded-lg border bg-background p-4 sm:grid-cols-2 xl:grid-cols-6">
        <Filter label="From"><Input id="report-from" type="date" value={from} onChange={(event) => { setFrom(event.target.value); resetPages(); }} /></Filter>
        <Filter label="To"><Input id="report-to" type="date" value={to} onChange={(event) => { setTo(event.target.value); resetPages(); }} /></Filter>
        <Filter label="Movie"><select id="report-movie" value={movieId} onChange={(event) => { setMovieId(event.target.value); setShowtimeId(""); resetPages(); }}><option value="">All movies</option>{movies.data?.content.map((movie) => <option key={movie.id} value={movie.id}>{movie.title}</option>)}</select></Filter>
        <Filter label="Theatre"><select id="report-theatre" value={theatreId} onChange={(event) => { setTheatreId(event.target.value); setScreenId(""); setShowtimeId(""); resetPages(); }}><option value="">All theatres</option>{theatres.data?.content.map((theatre) => <option key={theatre.id} value={theatre.id}>{theatre.name}</option>)}</select></Filter>
        <Filter label="Screen"><select id="report-screen" value={screenId} onChange={(event) => { setScreenId(event.target.value); setShowtimeId(""); resetPages(); }}><option value="">All screens</option>{screens.data?.content.map((screen) => <option key={screen.id} value={screen.id}>{screen.name}</option>)}</select></Filter>
        <Filter label="Showtime"><select id="report-showtime" value={showtimeId} onChange={(event) => { setShowtimeId(event.target.value); resetPages(); }}><option value="">All showtimes</option>{showtimes.data?.content.map((showtime) => <option key={showtime.id} value={showtime.id}>{showtime.movieTitle} · {formatDate(showtime.startTime)} {formatTime(showtime.startTime)}</option>)}</select></Filter>
      </section>

      <section className="grid gap-px overflow-hidden rounded-lg border bg-border sm:grid-cols-2 xl:grid-cols-5">
        {metrics.map((metric) => <div key={metric.label} className="bg-background p-4"><metric.icon className="size-4 text-muted-foreground" /><p className="mt-5 text-2xl font-semibold">{metric.value}</p><p className="mt-1 text-xs text-muted-foreground">{metric.label}</p></div>)}
      </section>

      <div className="mt-6 flex flex-col gap-6">
        <ReportTable reportKey="occupancy" title="Showtime occupancy" state={reports.occupancy} result={occupancy} onChange={updateReport} headers={["Showtime", "Venue", "Seats", "Occupancy"]} rows={occupancy.data?.content.map((row) => [<span key="movie" className="font-medium">{row.movieTitle}<small className="block font-normal text-muted-foreground">{formatDate(row.startTime)} · {formatTime(row.startTime)}</small></span>, `${row.theatreName} · ${row.screenName}`, `${row.reservedSeats}/${row.totalSeats}`, `${Math.round(Number(row.occupancyRate) * 100)}%`])} />
        <ReportTable reportKey="revenue" title="Revenue per movie" state={reports.revenue} result={revenue} onChange={updateReport} headers={["Movie", "Bookings", "Tickets", "Revenue"]} rows={revenue.data?.content.map((row) => [row.movieTitle, row.reservationCount, row.ticketsSold, formatPrice(row.revenue)])} />
        <ReportTable reportKey="cancelled" title="Cancelled bookings" state={reports.cancelled} result={cancelled} onChange={updateReport} headers={["Reference", "Movie", "Cancelled", "Value"]} rows={cancelled.data?.content.map((row) => [row.bookingReference, row.movieTitle, formatDate(row.cancelledAt), formatPrice(row.totalPrice)])} />
        <ReportTable reportKey="popular" title="Popular seats" state={reports.popular} result={popular} onChange={updateReport} headers={["Seat", "Venue", "Bookings", "Revenue"]} rows={popular.data?.content.map((row) => [`${row.rowLabel}${row.seatNumber} · ${row.seatType}`, `${row.theatreName} · ${row.screenName}`, row.bookingCount, formatPrice(row.revenue)])} />
        <ReportTable reportKey="conversion" title="Checkout conversion" state={reports.conversion} result={conversion} onChange={updateReport} headers={["Movie", "Checkouts", "Conversion", "Abandoned"]} rows={conversion.data?.content.map((row) => [row.movieTitle, row.checkoutCount, `${Math.round(Number(row.conversionRate) * 100)}%`, `${row.abandonedCheckoutCount} (${Math.round(Number(row.abandonedRate) * 100)}%)`])} />
      </div>
    </>
  );
}

function Filter({ label, children }: { label: string; children: React.ReactElement<{ id?: string; className?: string }> }) {
  return <div className="flex min-w-0 flex-col gap-1.5"><Label htmlFor={children.props.id}>{label}</Label><div className="[&_select]:h-8 [&_select]:w-full [&_select]:rounded-lg [&_select]:border [&_select]:bg-background [&_select]:px-2.5 [&_select]:text-sm">{children}</div></div>;
}

function ReportTable({ reportKey, title, state, result, onChange, headers, rows = [] }: {
  reportKey: ReportKey;
  title: string;
  state: ReportState[ReportKey];
  result: { isLoading: boolean; isError: boolean; data?: { totalPages: number; totalElements: number } };
  onChange: (key: ReportKey, value: Partial<ReportState[ReportKey]>) => void;
  headers: string[];
  rows?: React.ReactNode[][];
}) {
  return (
    <section className="overflow-hidden rounded-lg border bg-background">
      <div className="flex flex-col gap-2 border-b px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
        <h2 className="text-sm font-semibold">{title}</h2>
        <select className="h-8 rounded-lg border bg-background px-2.5 text-xs" aria-label={`Sort ${title}`} value={state.sort} onChange={(event) => onChange(reportKey, { page: 0, sort: event.target.value })}>
          {sortOptions[reportKey].map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
        </select>
      </div>
      <TableState loading={result.isLoading} error={result.isError} empty={rows.length === 0} />
      {rows.length ? <div className="overflow-x-auto"><table className="w-full text-sm"><thead className="bg-muted/60 text-left text-xs text-muted-foreground"><tr>{headers.map((header) => <th key={header} className="px-4 py-3 font-medium">{header}</th>)}</tr></thead><tbody className="divide-y">{rows.map((row, index) => <tr key={index}>{row.map((cell, cellIndex) => <td key={cellIndex} className="px-4 py-3">{cell}</td>)}</tr>)}</tbody></table></div> : null}
      {result.data ? <Pagination page={state.page} totalPages={result.data.totalPages} totalElements={result.data.totalElements} onPageChange={(page) => onChange(reportKey, { page })} /> : null}
    </section>
  );
}
