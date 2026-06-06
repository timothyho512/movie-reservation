"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Pencil, Plus } from "lucide-react";
import { toast } from "sonner";
import { adminApi } from "@/lib/api/admin";
import type { AdminShowtimeResponse, ShowtimeStatus } from "@/types/api";
import { formatDate, formatPrice, formatTime } from "@/lib/format";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { ConfirmActionDialog, PageHeader, Pagination, ShowtimeStatusBadge, TableState } from "@/components/admin/AdminPrimitives";

const statuses: ShowtimeStatus[] = ["UPCOMING", "ONGOING", "COMPLETED", "CANCELLED"];

export default function AdminShowtimesPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [movieId, setMovieId] = useState("");
  const [screenId, setScreenId] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [editing, setEditing] = useState<AdminShowtimeResponse | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [cancelTarget, setCancelTarget] = useState<AdminShowtimeResponse | null>(null);
  const params = { page, size: 12, movieId: movieId || undefined, screenId: screenId || undefined, status: statusFilter || undefined, sort: "startTime,asc" };
  const result = useQuery({ queryKey: ["admin", "showtimes", params], queryFn: () => adminApi.showtimes(params) });
  const movies = useQuery({ queryKey: ["admin", "movies", "active-options"], queryFn: () => adminApi.movies({ active: true, size: 100, sort: "title,asc" }) });
  const screens = useQuery({ queryKey: ["admin", "screens", "active-options"], queryFn: () => adminApi.screens({ active: true, size: 100, sort: "name,asc" }) });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ["admin", "showtimes"] });
  const save = useMutation({
    mutationFn: (data: Record<string, unknown>) => editing ? adminApi.updateShowtime(editing.id, data) : adminApi.createShowtime(data),
    onSuccess: () => { toast.success(editing ? "Showtime updated" : "Showtime created"); setDialogOpen(false); setEditing(null); refresh(); },
    onError: (error: Error) => toast.error(error.message),
  });
  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: ShowtimeStatus }) => adminApi.setShowtimeStatus(id, status),
    onSuccess: () => { toast.success("Showtime status updated"); setCancelTarget(null); refresh(); },
    onError: (error: Error) => toast.error(error.message),
  });
  return <>
    <PageHeader title="Showtimes" description="Schedule screenings and manage their operational lifecycle." action={<Button onClick={() => { setEditing(null); setDialogOpen(true); }}><Plus data-icon="inline-start" />Add showtime</Button>} />
    <div className="overflow-hidden rounded-lg border bg-background">
      <div className="flex flex-wrap gap-3 border-b p-4">
        <Filter value={movieId} onChange={(value) => { setMovieId(value); setPage(0); }} label="All movies" options={movies.data?.content.map((movie) => [String(movie.id), movie.title]) ?? []} />
        <Filter value={screenId} onChange={(value) => { setScreenId(value); setPage(0); }} label="All screens" options={screens.data?.content.map((screen) => [String(screen.id), `${screen.theatreName} · ${screen.name}`]) ?? []} />
        <Filter value={statusFilter} onChange={(value) => { setStatusFilter(value); setPage(0); }} label="All statuses" options={statuses.map((status) => [status, status])} />
      </div>
      <TableState loading={result.isLoading} error={result.isError} empty={result.data?.content.length === 0} />
      {result.data?.content.length ? <div className="overflow-x-auto"><table className="w-full text-sm"><thead className="bg-muted/60 text-left text-xs text-muted-foreground"><tr><th className="px-4 py-3 font-medium">Movie</th><th className="px-4 py-3 font-medium">Date and time</th><th className="px-4 py-3 font-medium">Screen</th><th className="px-4 py-3 font-medium">Sales</th><th className="px-4 py-3 font-medium">Status</th><th className="px-4 py-3 text-right font-medium">Actions</th></tr></thead>
        <tbody className="divide-y">{result.data.content.map((showtime) => <tr key={showtime.id} className="hover:bg-muted/30"><td className="px-4 py-3 font-medium">{showtime.movieTitle}</td><td className="px-4 py-3"><p>{formatDate(showtime.startTime)}</p><p className="text-xs text-muted-foreground">{formatTime(showtime.startTime)}–{formatTime(showtime.endTime)}</p></td><td className="px-4 py-3"><p>{showtime.screenName}</p><p className="text-xs text-muted-foreground">{showtime.theatreName} · layout v{showtime.layoutVersion ?? "—"}</p></td><td className="px-4 py-3"><p>{formatPrice(showtime.basePrice)}</p><p className="text-xs text-muted-foreground">{showtime.availableSeats}/{showtime.totalSeats} available</p></td><td className="px-4 py-3"><ShowtimeStatusBadge status={showtime.status} /></td><td className="px-4 py-3"><div className="flex justify-end gap-1"><Button variant="ghost" size="icon-sm" onClick={() => { setEditing(showtime); setDialogOpen(true); }}><Pencil /></Button><select aria-label="Change showtime status" value={showtime.status} onChange={(e) => { const nextStatus = e.target.value as ShowtimeStatus; if (nextStatus === "CANCELLED") setCancelTarget(showtime); else statusMutation.mutate({ id: showtime.id, status: nextStatus }); }} className="h-7 rounded-md border bg-background px-2 text-xs">{statuses.map((status) => <option key={status}>{status}</option>)}</select></div></td></tr>)}</tbody>
      </table></div> : null}
      {result.data ? <Pagination page={page} totalPages={result.data.totalPages} totalElements={result.data.totalElements} onPageChange={setPage} /> : null}
    </div>
    <ShowtimeDialog open={dialogOpen} showtime={editing} movies={movies.data?.content ?? []} screens={screens.data?.content ?? []} pending={save.isPending} onOpenChange={setDialogOpen} onSubmit={(data) => save.mutate(data)} />
    <ConfirmActionDialog open={cancelTarget !== null} title="Cancel showtime?" description={`${cancelTarget?.movieTitle ?? "This showtime"} at ${cancelTarget ? `${formatDate(cancelTarget.startTime)} ${formatTime(cancelTarget.startTime)}` : ""} will be cancelled. Existing booking history remains available for reporting.`} confirmLabel="Cancel showtime" pending={statusMutation.isPending} onOpenChange={(open) => { if (!open) setCancelTarget(null); }} onConfirm={() => { if (cancelTarget) statusMutation.mutate({ id: cancelTarget.id, status: "CANCELLED" }); }} />
  </>;
}

function Filter({ value, onChange, label, options }: { value: string; onChange: (value: string) => void; label: string; options: string[][] }) {
  return <select className="h-8 min-w-40 rounded-lg border bg-background px-2.5 text-sm" value={value} onChange={(e) => onChange(e.target.value)}><option value="">{label}</option>{options.map(([value, name]) => <option key={value} value={value}>{name}</option>)}</select>;
}

function ShowtimeDialog({ open, showtime, movies, screens, pending, onOpenChange, onSubmit }: {
  open: boolean; showtime: AdminShowtimeResponse | null; movies: Array<{ id: number; title: string }>; screens: Array<{ id: number; name: string; theatreName: string }>; pending: boolean; onOpenChange: (open: boolean) => void; onSubmit: (data: Record<string, unknown>) => void;
}) {
  return <Dialog open={open} onOpenChange={onOpenChange}><DialogContent className="sm:max-w-xl"><DialogHeader><DialogTitle>{showtime ? "Edit showtime" : "Add showtime"}</DialogTitle><DialogDescription>Only active movies and screens are available for scheduling.</DialogDescription></DialogHeader>
    <form id="showtime-form" className="grid gap-4 sm:grid-cols-2" onSubmit={(event) => { event.preventDefault(); const data = new FormData(event.currentTarget); onSubmit({ movieId: Number(data.get("movieId")), screenId: Number(data.get("screenId")), startTime: data.get("startTime"), endTime: data.get("endTime"), basePrice: Number(data.get("basePrice")), status: data.get("status") }); }}>
      <div className="flex flex-col gap-1.5 sm:col-span-2"><Label htmlFor="showtime-movie">Movie</Label><select id="showtime-movie" name="movieId" defaultValue={showtime?.movieId} className="h-8 rounded-lg border bg-background px-2.5 text-sm" required>{movies.map((movie) => <option key={movie.id} value={movie.id}>{movie.title}</option>)}</select></div>
      <div className="flex flex-col gap-1.5 sm:col-span-2"><Label htmlFor="showtime-screen">Screen</Label><select id="showtime-screen" name="screenId" defaultValue={showtime?.screenId} className="h-8 rounded-lg border bg-background px-2.5 text-sm" required>{screens.map((screen) => <option key={screen.id} value={screen.id}>{screen.theatreName} · {screen.name}</option>)}</select></div>
      <div className="flex flex-col gap-1.5"><Label htmlFor="showtime-start">Starts</Label><Input id="showtime-start" name="startTime" type="datetime-local" defaultValue={localInput(showtime?.startTime)} required /></div>
      <div className="flex flex-col gap-1.5"><Label htmlFor="showtime-end">Ends</Label><Input id="showtime-end" name="endTime" type="datetime-local" defaultValue={localInput(showtime?.endTime)} required /></div>
      <div className="flex flex-col gap-1.5"><Label htmlFor="showtime-price">Base price</Label><Input id="showtime-price" name="basePrice" type="number" min="0" step="0.5" defaultValue={showtime?.basePrice ?? "12.50"} required /></div>
      <div className="flex flex-col gap-1.5"><Label htmlFor="showtime-status">Status</Label><select id="showtime-status" name="status" defaultValue={showtime?.status ?? "UPCOMING"} className="h-8 rounded-lg border bg-background px-2.5 text-sm">{statuses.map((status) => <option key={status}>{status}</option>)}</select></div>
    </form><DialogFooter showCloseButton><Button type="submit" form="showtime-form" disabled={pending}>{pending ? "Saving…" : "Save showtime"}</Button></DialogFooter></DialogContent></Dialog>;
}

function localInput(value?: string) { return value ? value.slice(0, 16) : ""; }
