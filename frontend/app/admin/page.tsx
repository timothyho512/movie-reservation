"use client";

import Link from "next/link";
import { useQueries, useQuery } from "@tanstack/react-query";
import { ArrowRight, Building2, CalendarDays, Clapperboard, MonitorPlay } from "lucide-react";
import { adminApi } from "@/lib/api/admin";
import { formatDate, formatPrice, formatTime } from "@/lib/format";
import { PageHeader, ShowtimeStatusBadge } from "@/components/admin/AdminPrimitives";
import { Button } from "@/components/ui/button";

export default function AdminOverviewPage() {
  const [movies, theatres, screens] = useQueries({
    queries: [
      { queryKey: ["admin", "overview", "movies"], queryFn: () => adminApi.movies({ active: true, size: 1 }) },
      { queryKey: ["admin", "overview", "theatres"], queryFn: () => adminApi.theatres({ active: true, size: 1 }) },
      { queryKey: ["admin", "overview", "screens"], queryFn: () => adminApi.screens({ active: true, size: 1 }) },
    ],
  });
  const showtimes = useQuery({ queryKey: ["admin", "overview", "showtimes"], queryFn: () => adminApi.showtimes({ from: new Date().toISOString().slice(0, 19), size: 6, sort: "startTime,asc" }) });
  const metrics = [
    { label: "Active movies", value: movies.data?.totalElements ?? "—", icon: Clapperboard, href: "/admin/movies" },
    { label: "Active theatres", value: theatres.data?.totalElements ?? "—", icon: Building2, href: "/admin/theatres" },
    { label: "Active screens", value: screens.data?.totalElements ?? "—", icon: MonitorPlay, href: "/admin/screens" },
    { label: "Upcoming showtimes", value: showtimes.data?.totalElements ?? "—", icon: CalendarDays, href: "/admin/showtimes" },
  ];
  return <>
    <PageHeader title="Admin Overview" description="Cinema operations, catalogue health and upcoming screenings." />
    <section className="grid gap-px overflow-hidden rounded-lg border bg-border sm:grid-cols-2 xl:grid-cols-4">
      {metrics.map((metric) => <Link key={metric.label} href={metric.href} className="bg-background p-4 hover:bg-muted/40"><div className="flex items-center justify-between"><metric.icon className="size-4 text-muted-foreground" /><ArrowRight className="size-4 text-muted-foreground" /></div><p className="mt-6 text-2xl font-semibold">{metric.value}</p><p className="mt-1 text-xs text-muted-foreground">{metric.label}</p></Link>)}
    </section>
    <section className="mt-6 overflow-hidden rounded-lg border bg-background">
      <div className="flex items-center justify-between border-b px-4 py-3"><div><h2 className="text-sm font-semibold">Upcoming showtimes</h2><p className="text-xs text-muted-foreground">Next scheduled screenings across all theatres.</p></div><Button render={<Link href="/admin/showtimes" />} nativeButton={false} variant="outline" size="sm">View all</Button></div>
      <div className="overflow-x-auto"><table className="w-full text-sm"><thead className="bg-muted/60 text-left text-xs text-muted-foreground"><tr><th className="px-4 py-3 font-medium">Movie</th><th className="px-4 py-3 font-medium">Schedule</th><th className="px-4 py-3 font-medium">Venue</th><th className="px-4 py-3 font-medium">Price</th><th className="px-4 py-3 font-medium">Status</th></tr></thead>
        <tbody className="divide-y">{showtimes.data?.content.map((showtime) => <tr key={showtime.id}><td className="px-4 py-3 font-medium">{showtime.movieTitle}</td><td className="px-4 py-3">{formatDate(showtime.startTime)} · {formatTime(showtime.startTime)}</td><td className="px-4 py-3">{showtime.theatreName} · {showtime.screenName}</td><td className="px-4 py-3">{formatPrice(showtime.basePrice)}</td><td className="px-4 py-3"><ShowtimeStatusBadge status={showtime.status} /></td></tr>)}</tbody>
      </table></div>
      {!showtimes.isLoading && showtimes.data?.content.length === 0 ? <p className="p-8 text-center text-sm text-muted-foreground">No upcoming showtimes.</p> : null}
    </section>
  </>;
}
