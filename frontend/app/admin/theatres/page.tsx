"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Pencil, Plus, Power } from "lucide-react";
import { toast } from "sonner";
import { adminApi } from "@/lib/api/admin";
import type { AdminTheatreResponse } from "@/types/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { ConfirmActionDialog, PageHeader, Pagination, SearchInput, StatusBadge, TableState } from "@/components/admin/AdminPrimitives";

export default function AdminTheatresPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [editing, setEditing] = useState<AdminTheatreResponse | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [statusTarget, setStatusTarget] = useState<AdminTheatreResponse | null>(null);
  const params = { page, size: 12, search, sort: "name,asc" };
  const result = useQuery({ queryKey: ["admin", "theatres", params], queryFn: () => adminApi.theatres(params) });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ["admin", "theatres"] });
  const save = useMutation({
    mutationFn: (data: Record<string, unknown>) => editing ? adminApi.updateTheatre(editing.id, data) : adminApi.createTheatre(data),
    onSuccess: () => { toast.success(editing ? "Theatre updated" : "Theatre created"); setDialogOpen(false); setEditing(null); refresh(); },
    onError: (error: Error) => toast.error(error.message),
  });
  const status = useMutation({
    mutationFn: ({ id, value }: { id: number; value: boolean }) => adminApi.setTheatreActive(id, value),
    onSuccess: (_, variables) => { toast.success(variables.value ? "Theatre reactivated" : "Theatre deactivated"); setStatusTarget(null); refresh(); },
    onError: (error: Error) => toast.error(error.message),
  });

  return (
    <>
      <PageHeader title="Theatres" description="Manage cinema locations and their operational status." action={<Button onClick={() => { setEditing(null); setDialogOpen(true); }}><Plus data-icon="inline-start" />Add theatre</Button>} />
      <div className="overflow-hidden rounded-lg border bg-background">
        <div className="border-b p-4"><SearchInput value={search} onChange={(value) => { setSearch(value); setPage(0); }} placeholder="Search name, city or country" /></div>
        <TableState loading={result.isLoading} error={result.isError} empty={result.data?.content.length === 0} />
        {result.data?.content.length ? <div className="overflow-x-auto"><table className="w-full text-sm">
          <thead className="bg-muted/60 text-left text-xs text-muted-foreground"><tr><th className="px-4 py-3 font-medium">Theatre</th><th className="px-4 py-3 font-medium">Location</th><th className="px-4 py-3 font-medium">Capacity</th><th className="px-4 py-3 font-medium">Status</th><th className="px-4 py-3 text-right font-medium">Actions</th></tr></thead>
          <tbody className="divide-y">{result.data.content.map((theatre) => <tr key={theatre.id} className="hover:bg-muted/30">
            <td className="px-4 py-3"><p className="font-medium">{theatre.name}</p><p className="text-xs text-muted-foreground">{theatre.address}</p></td>
            <td className="px-4 py-3 text-muted-foreground">{theatre.city}, {theatre.country}</td>
            <td className="px-4 py-3">{theatre.totalScreens ?? 0} screens · {theatre.totalSeats ?? 0} seats</td>
            <td className="px-4 py-3"><StatusBadge active={theatre.active} /></td>
            <td className="px-4 py-3"><div className="flex justify-end gap-1"><Button variant="ghost" size="icon-sm" onClick={() => { setEditing(theatre); setDialogOpen(true); }}><Pencil /></Button><Button variant="ghost" size="icon-sm" onClick={() => setStatusTarget(theatre)}><Power /></Button></div></td>
          </tr>)}</tbody>
        </table></div> : null}
        {result.data ? <Pagination page={page} totalPages={result.data.totalPages} totalElements={result.data.totalElements} onPageChange={setPage} /> : null}
      </div>
      <TheatreDialog open={dialogOpen} theatre={editing} pending={save.isPending} onOpenChange={setDialogOpen} onSubmit={(data) => save.mutate(data)} />
      <ConfirmActionDialog open={statusTarget !== null} title={`${statusTarget?.active ? "Deactivate" : "Reactivate"} theatre?`} description={`${statusTarget?.name ?? "This theatre"} and its screens will ${statusTarget?.active ? "be unavailable for new showtimes" : "be available for operations again"}.`} confirmLabel={statusTarget?.active ? "Deactivate theatre" : "Reactivate theatre"} pending={status.isPending} onOpenChange={(open) => { if (!open) setStatusTarget(null); }} onConfirm={() => { if (statusTarget) status.mutate({ id: statusTarget.id, value: !statusTarget.active }); }} />
    </>
  );
}

function TheatreDialog({ open, theatre, pending, onOpenChange, onSubmit }: {
  open: boolean; theatre: AdminTheatreResponse | null; pending: boolean; onOpenChange: (open: boolean) => void; onSubmit: (data: Record<string, unknown>) => void;
}) {
  const fields: Array<[string, string, string]> = [
    ["name", "Name", theatre?.name ?? ""], ["address", "Address", theatre?.address ?? ""],
    ["city", "City", theatre?.city ?? ""], ["state", "State / region", theatre?.state ?? ""],
    ["country", "Country", theatre?.country ?? ""], ["postalCode", "Postal code", theatre?.postalCode ?? ""],
    ["phoneNumber", "Phone number", theatre?.phoneNumber ?? ""],
  ];
  return <Dialog open={open} onOpenChange={onOpenChange}><DialogContent className="sm:max-w-xl"><DialogHeader><DialogTitle>{theatre ? "Edit theatre" : "Add theatre"}</DialogTitle><DialogDescription>Location details shown to customers and cinema operators.</DialogDescription></DialogHeader>
    <form id="theatre-form" className="grid gap-4 sm:grid-cols-2" onSubmit={(event) => { event.preventDefault(); const data = Object.fromEntries(new FormData(event.currentTarget)); onSubmit(data); }}>
      {fields.map(([name, label, value], index) => <div key={name} className={index < 2 ? "flex flex-col gap-1.5 sm:col-span-2" : "flex flex-col gap-1.5"}><Label htmlFor={`theatre-${name}`}>{label}</Label><Input id={`theatre-${name}`} name={name} defaultValue={value} required={name !== "phoneNumber"} /></div>)}
    </form><DialogFooter showCloseButton><Button type="submit" form="theatre-form" disabled={pending}>{pending ? "Saving…" : "Save theatre"}</Button></DialogFooter></DialogContent></Dialog>;
}
