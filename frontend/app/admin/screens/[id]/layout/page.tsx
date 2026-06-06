"use client";

import Link from "next/link";
import { use, useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Plus, Save, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { adminApi } from "@/lib/api/admin";
import type { AdminSeatDefinition, SeatType } from "@/types/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { PageHeader } from "@/components/admin/AdminPrimitives";
import { cn } from "@/lib/utils";

const seatTypes: SeatType[] = ["REGULAR", "VIP", "WHEELCHAIR"];

export default function SeatLayoutEditor({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const screenId = Number(id);
  const queryClient = useQueryClient();
  const [draftSeats, setDraftSeats] = useState<AdminSeatDefinition[] | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const layout = useQuery({ queryKey: ["admin", "layout", screenId], queryFn: () => adminApi.currentLayout(screenId) });
  const screen = useQuery({ queryKey: ["admin", "screen", screenId], queryFn: async () => (await adminApi.screens({ size: 100 })).content.find((item) => item.id === screenId) });

  const savedSeats = useMemo(() => layout.data?.seats.map((seat) => ({
    rowLabel: seat.rowLabel,
    seatNumber: seat.seatNumber,
    seatType: seat.seatType,
    basePrice: Number(seat.basePrice),
  })) ?? [], [layout.data]);
  const seats = draftSeats ?? savedSeats;
  const dirty = draftSeats !== null;
  useEffect(() => {
    const listener = (event: BeforeUnloadEvent) => { if (dirty) event.preventDefault(); };
    window.addEventListener("beforeunload", listener);
    return () => window.removeEventListener("beforeunload", listener);
  }, [dirty]);

  const rows = useMemo(() => {
    const grouped = new Map<string, AdminSeatDefinition[]>();
    seats
      .toSorted((a, b) => a.rowLabel.localeCompare(b.rowLabel) || a.seatNumber - b.seatNumber)
      .forEach((seat) => grouped.set(seat.rowLabel, [...(grouped.get(seat.rowLabel) ?? []), seat]));
    return grouped;
  }, [seats]);
  const selectedSeat = seats.find((seat) => keyOf(seat) === selected);
  const save = useMutation({
    mutationFn: () => adminApi.replaceLayout(screenId, seats),
    onSuccess: (data) => {
      toast.success(`Layout version ${data.versionNumber} created`);
      queryClient.setQueryData(["admin", "layout", screenId], data);
      setDraftSeats(null);
      setConfirmOpen(false);
      queryClient.invalidateQueries({ queryKey: ["admin", "screens"] });
    },
    onError: (error: Error) => toast.error(error.message),
  });

  function update(next: AdminSeatDefinition[]) { setDraftSeats(next); }
  function addRow() {
    const labels = [...rows.keys()];
    const nextLabel = String.fromCharCode((labels.at(-1)?.charCodeAt(0) ?? 64) + 1);
    update([...seats, ...Array.from({ length: 6 }, (_, index) => ({ rowLabel: nextLabel, seatNumber: index + 1, seatType: "REGULAR" as SeatType, basePrice: 12.5 }))]);
  }
  function addSeat(rowLabel: string) {
    const row = rows.get(rowLabel) ?? [];
    update([...seats, { rowLabel, seatNumber: Math.max(0, ...row.map((seat) => seat.seatNumber)) + 1, seatType: "REGULAR", basePrice: 12.5 }]);
  }
  function removeSeat(target: AdminSeatDefinition) {
    update(seats.filter((seat) => keyOf(seat) !== keyOf(target)));
    setSelected(null);
  }
  function updateSelected(patch: Partial<AdminSeatDefinition>) {
    if (!selectedSeat) return;
    update(seats.map((seat) => keyOf(seat) === selected ? { ...seat, ...patch } : seat));
  }

  return <>
    <Link href="/admin/screens" className="mb-4 inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground"><ArrowLeft className="size-4" />Back to screens</Link>
    <PageHeader title={`${screen.data?.name ?? "Screen"} seat layout`} description={`Current version ${layout.data?.versionNumber ?? "—"}. Saving creates a new version for future showtimes.`} action={<Button onClick={() => setConfirmOpen(true)} disabled={!dirty || seats.length === 0}><Save data-icon="inline-start" />Save new version</Button>} />
    <div className="grid gap-6 xl:grid-cols-[1fr_280px]">
      <section className="min-w-0 rounded-lg border bg-background p-4 sm:p-6">
        <div className="mx-auto mb-8 h-2 max-w-xl rounded-full bg-foreground/80" aria-label="Cinema screen" />
        <div className="flex min-w-max flex-col gap-3 overflow-x-auto pb-2">
          {[...rows.entries()].map(([rowLabel, rowSeats]) => <div key={rowLabel} className="flex items-center gap-2">
            <span className="w-6 shrink-0 text-center text-xs font-medium text-muted-foreground">{rowLabel}</span>
            {rowSeats.map((seat) => <button key={keyOf(seat)} type="button" onClick={() => setSelected(keyOf(seat))} className={cn("flex size-9 shrink-0 items-center justify-center rounded-md border text-xs font-medium transition-colors", selected === keyOf(seat) ? "border-primary bg-primary text-primary-foreground" : seat.seatType === "VIP" ? "bg-muted" : seat.seatType === "WHEELCHAIR" ? "border-dashed" : "bg-background hover:bg-muted")}>{seat.seatNumber}</button>)}
            <Button variant="ghost" size="icon-sm" onClick={() => addSeat(rowLabel)} aria-label={`Add seat to row ${rowLabel}`}><Plus /></Button>
          </div>)}
        </div>
        <Button variant="outline" className="mt-6" onClick={addRow}><Plus data-icon="inline-start" />Add row</Button>
      </section>
      <aside className="rounded-lg border bg-background p-4">
        <h2 className="text-sm font-semibold">Layout summary</h2>
        <dl className="mt-4 grid grid-cols-2 gap-3 text-sm"><div><dt className="text-xs text-muted-foreground">Seats</dt><dd className="font-medium">{seats.length}</dd></div><div><dt className="text-xs text-muted-foreground">Rows</dt><dd className="font-medium">{rows.size}</dd></div></dl>
        {selectedSeat ? <div className="mt-6 flex flex-col gap-4 border-t pt-4"><div><p className="text-sm font-medium">Seat {selectedSeat.rowLabel}{selectedSeat.seatNumber}</p><p className="text-xs text-muted-foreground">Configure the selected seat.</p></div>
          <div className="flex flex-col gap-1.5"><Label htmlFor="seat-type">Type</Label><select id="seat-type" className="h-8 rounded-lg border bg-background px-2.5 text-sm" value={selectedSeat.seatType} onChange={(e) => updateSelected({ seatType: e.target.value as SeatType })}>{seatTypes.map((type) => <option key={type}>{type}</option>)}</select></div>
          <div className="flex flex-col gap-1.5"><Label htmlFor="seat-price">Base price</Label><Input id="seat-price" type="number" min="0" step="0.5" value={selectedSeat.basePrice} onChange={(e) => updateSelected({ basePrice: Number(e.target.value) })} /></div>
          <Button variant="destructive" onClick={() => removeSeat(selectedSeat)}><Trash2 data-icon="inline-start" />Remove seat</Button>
        </div> : <p className="mt-6 border-t pt-4 text-sm text-muted-foreground">Select a seat to change its type or price.</p>}
      </aside>
    </div>
    <Dialog open={confirmOpen} onOpenChange={setConfirmOpen}><DialogContent><DialogHeader><DialogTitle>Create a new layout version?</DialogTitle><DialogDescription>Existing showtimes keep their original seat layout. Only newly created showtimes will use these {seats.length} seats.</DialogDescription></DialogHeader><DialogFooter showCloseButton><Button onClick={() => save.mutate()} disabled={save.isPending}>{save.isPending ? "Saving…" : "Create version"}</Button></DialogFooter></DialogContent></Dialog>
  </>;
}

function keyOf(seat: AdminSeatDefinition) { return `${seat.rowLabel}:${seat.seatNumber}`; }
