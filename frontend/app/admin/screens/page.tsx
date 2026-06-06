"use client";

import Link from "next/link";
import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Grid3X3, Pencil, Plus, Power } from "lucide-react";
import { toast } from "sonner";
import { adminApi } from "@/lib/api/admin";
import type { AdminScreenResponse, ScreenType } from "@/types/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { ConfirmActionDialog, PageHeader, Pagination, SearchInput, StatusBadge, TableState } from "@/components/admin/AdminPrimitives";

const screenTypes: ScreenType[] = ["STANDARD", "IMAX", "DOLBY_ATMOS", "THREE_D", "FOUR_DX", "VIP"];

export default function AdminScreensPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [theatreId, setTheatreId] = useState("");
  const [editing, setEditing] = useState<AdminScreenResponse | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [statusTarget, setStatusTarget] = useState<AdminScreenResponse | null>(null);
  const params = { page, size: 12, search, theatreId: theatreId || undefined, sort: "name,asc" };
  const result = useQuery({ queryKey: ["admin", "screens", params], queryFn: () => adminApi.screens(params) });
  const theatres = useQuery({ queryKey: ["admin", "theatres", "active-options"], queryFn: () => adminApi.theatres({ active: true, size: 100, sort: "name,asc" }) });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ["admin", "screens"] });
  const save = useMutation({
    mutationFn: (data: { name: string; theatreId: number; screenType: ScreenType }) => editing ? adminApi.updateScreen(editing.id, data) : adminApi.createScreen(data),
    onSuccess: () => { toast.success(editing ? "Screen updated" : "Screen created"); setDialogOpen(false); setEditing(null); refresh(); },
    onError: (error: Error) => toast.error(error.message),
  });
  const status = useMutation({
    mutationFn: ({ id, value }: { id: number; value: boolean }) => adminApi.setScreenActive(id, value),
    onSuccess: (_, variables) => { toast.success(variables.value ? "Screen reactivated" : "Screen deactivated"); setStatusTarget(null); refresh(); },
    onError: (error: Error) => toast.error(error.message),
  });
  return <>
    <PageHeader title="Screens" description="Manage auditoriums, formats, capacity and versioned seat layouts." action={<Button onClick={() => { setEditing(null); setDialogOpen(true); }}><Plus data-icon="inline-start" />Add screen</Button>} />
    <div className="overflow-hidden rounded-lg border bg-background">
      <div className="flex flex-col gap-3 border-b p-4 sm:flex-row">
        <SearchInput value={search} onChange={(value) => { setSearch(value); setPage(0); }} placeholder="Search screen or theatre" />
        <select className="h-8 rounded-lg border bg-background px-2.5 text-sm" value={theatreId} onChange={(e) => { setTheatreId(e.target.value); setPage(0); }}>
          <option value="">All theatres</option>{theatres.data?.content.map((theatre) => <option key={theatre.id} value={theatre.id}>{theatre.name}</option>)}
        </select>
      </div>
      <TableState loading={result.isLoading} error={result.isError} empty={result.data?.content.length === 0} />
      {result.data?.content.length ? <div className="overflow-x-auto"><table className="w-full text-sm"><thead className="bg-muted/60 text-left text-xs text-muted-foreground"><tr><th className="px-4 py-3 font-medium">Screen</th><th className="px-4 py-3 font-medium">Theatre</th><th className="px-4 py-3 font-medium">Layout</th><th className="px-4 py-3 font-medium">Status</th><th className="px-4 py-3 text-right font-medium">Actions</th></tr></thead>
        <tbody className="divide-y">{result.data.content.map((screen) => <tr key={screen.id} className="hover:bg-muted/30"><td className="px-4 py-3"><p className="font-medium">{screen.name}</p><p className="text-xs text-muted-foreground">{screen.screenType.replaceAll("_", " ")}</p></td><td className="px-4 py-3">{screen.theatreName}</td><td className="px-4 py-3"><p>{screen.totalSeats} seats</p><p className="text-xs text-muted-foreground">Version {screen.currentLayoutVersion ?? "—"}</p></td><td className="px-4 py-3"><StatusBadge active={screen.active} /></td><td className="px-4 py-3"><div className="flex justify-end gap-1"><Button render={<Link href={`/admin/screens/${screen.id}/layout`} />} nativeButton={false} variant="ghost" size="icon-sm" aria-label="Edit seat layout"><Grid3X3 /></Button><Button variant="ghost" size="icon-sm" onClick={() => { setEditing(screen); setDialogOpen(true); }}><Pencil /></Button><Button variant="ghost" size="icon-sm" onClick={() => setStatusTarget(screen)}><Power /></Button></div></td></tr>)}</tbody>
      </table></div> : null}
      {result.data ? <Pagination page={page} totalPages={result.data.totalPages} totalElements={result.data.totalElements} onPageChange={setPage} /> : null}
    </div>
    <ScreenDialog open={dialogOpen} screen={editing} theatres={theatres.data?.content ?? []} pending={save.isPending} onOpenChange={setDialogOpen} onSubmit={(data) => save.mutate(data)} />
    <ConfirmActionDialog open={statusTarget !== null} title={`${statusTarget?.active ? "Deactivate" : "Reactivate"} screen?`} description={`${statusTarget?.name ?? "This screen"} will ${statusTarget?.active ? "be unavailable for new showtimes" : "be available for scheduling again"}. Historical bookings remain unchanged.`} confirmLabel={statusTarget?.active ? "Deactivate screen" : "Reactivate screen"} pending={status.isPending} onOpenChange={(open) => { if (!open) setStatusTarget(null); }} onConfirm={() => { if (statusTarget) status.mutate({ id: statusTarget.id, value: !statusTarget.active }); }} />
  </>;
}

function ScreenDialog({ open, screen, theatres, pending, onOpenChange, onSubmit }: {
  open: boolean; screen: AdminScreenResponse | null; theatres: Array<{ id: number; name: string }>; pending: boolean; onOpenChange: (open: boolean) => void; onSubmit: (data: { name: string; theatreId: number; screenType: ScreenType }) => void;
}) {
  return <Dialog open={open} onOpenChange={onOpenChange}><DialogContent><DialogHeader><DialogTitle>{screen ? "Edit screen" : "Add screen"}</DialogTitle><DialogDescription>Set the auditorium identity. Seat capacity is calculated from its current layout.</DialogDescription></DialogHeader>
    <form id="screen-form" className="flex flex-col gap-4" onSubmit={(event) => { event.preventDefault(); const data = new FormData(event.currentTarget); onSubmit({ name: String(data.get("name")), theatreId: Number(data.get("theatreId")), screenType: String(data.get("screenType")) as ScreenType }); }}>
      <div className="flex flex-col gap-1.5"><Label htmlFor="screen-name">Name</Label><Input id="screen-name" name="name" defaultValue={screen?.name} required /></div>
      <div className="flex flex-col gap-1.5"><Label htmlFor="screen-theatre">Theatre</Label><select id="screen-theatre" name="theatreId" defaultValue={screen?.theatreId} className="h-8 rounded-lg border bg-background px-2.5 text-sm" required>{theatres.map((theatre) => <option key={theatre.id} value={theatre.id}>{theatre.name}</option>)}</select></div>
      <div className="flex flex-col gap-1.5"><Label htmlFor="screen-type">Format</Label><select id="screen-type" name="screenType" defaultValue={screen?.screenType ?? "STANDARD"} className="h-8 rounded-lg border bg-background px-2.5 text-sm">{screenTypes.map((type) => <option key={type} value={type}>{type.replaceAll("_", " ")}</option>)}</select></div>
    </form><DialogFooter showCloseButton><Button type="submit" form="screen-form" disabled={pending}>{pending ? "Saving…" : "Save screen"}</Button></DialogFooter></DialogContent></Dialog>;
}
