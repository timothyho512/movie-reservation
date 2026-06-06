"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Pencil, Plus, Power } from "lucide-react";
import { toast } from "sonner";
import { adminApi } from "@/lib/api/admin";
import type { AdminMovieResponse } from "@/types/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle,
} from "@/components/ui/dialog";
import { ConfirmActionDialog, PageHeader, Pagination, SearchInput, StatusBadge, TableState } from "@/components/admin/AdminPrimitives";

export default function AdminMoviesPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [active, setActive] = useState<string>("all");
  const [editing, setEditing] = useState<AdminMovieResponse | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [statusTarget, setStatusTarget] = useState<AdminMovieResponse | null>(null);
  const params = { page, size: 12, search, active: active === "all" ? undefined : active === "active", sort: "title,asc" };
  const result = useQuery({ queryKey: ["admin", "movies", params], queryFn: () => adminApi.movies(params) });

  const save = useMutation({
    mutationFn: async (data: { title: string; director: string }) =>
      editing ? adminApi.updateMovie(editing.id, data) : adminApi.createMovie(data),
    onSuccess: () => {
      toast.success(editing ? "Movie updated" : "Movie created");
      setDialogOpen(false);
      setEditing(null);
      queryClient.invalidateQueries({ queryKey: ["admin", "movies"] });
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const status = useMutation({
    mutationFn: ({ id, value }: { id: number; value: boolean }) => adminApi.setMovieActive(id, value),
    onSuccess: (_, variables) => {
      toast.success(variables.value ? "Movie reactivated" : "Movie deactivated");
      setStatusTarget(null);
      queryClient.invalidateQueries({ queryKey: ["admin", "movies"] });
    },
    onError: (error: Error) => toast.error(error.message),
  });

  function openForm(movie?: AdminMovieResponse) {
    setEditing(movie ?? null);
    setDialogOpen(true);
  }

  return (
    <>
      <PageHeader
        title="Movies"
        description="Manage the catalogue available for showtime scheduling."
        action={<Button onClick={() => openForm()}><Plus data-icon="inline-start" />Add movie</Button>}
      />
      <div className="overflow-hidden rounded-lg border bg-background">
        <div className="flex flex-col gap-3 border-b p-4 sm:flex-row">
          <SearchInput value={search} onChange={(value) => { setSearch(value); setPage(0); }} placeholder="Search title or director" />
          <select className="h-8 rounded-lg border bg-background px-2.5 text-sm" value={active} onChange={(e) => { setActive(e.target.value); setPage(0); }}>
            <option value="all">All statuses</option>
            <option value="active">Active</option>
            <option value="inactive">Inactive</option>
          </select>
        </div>
        <TableState loading={result.isLoading} error={result.isError} empty={result.data?.content.length === 0} />
        {result.data?.content.length ? (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-muted/60 text-left text-xs text-muted-foreground">
                <tr><th className="px-4 py-3 font-medium">Title</th><th className="px-4 py-3 font-medium">Director</th><th className="px-4 py-3 font-medium">Status</th><th className="px-4 py-3 text-right font-medium">Actions</th></tr>
              </thead>
              <tbody className="divide-y">
                {result.data.content.map((movie) => (
                  <tr key={movie.id} className="hover:bg-muted/30">
                    <td className="px-4 py-3 font-medium">{movie.title}</td>
                    <td className="px-4 py-3 text-muted-foreground">{movie.director}</td>
                    <td className="px-4 py-3"><StatusBadge active={movie.active} /></td>
                    <td className="px-4 py-3">
                      <div className="flex justify-end gap-1">
                        <Button variant="ghost" size="icon-sm" onClick={() => openForm(movie)} aria-label={`Edit ${movie.title}`}><Pencil /></Button>
                        <Button variant="ghost" size="icon-sm" onClick={() => setStatusTarget(movie)} aria-label={movie.active ? "Deactivate movie" : "Reactivate movie"}><Power /></Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
        {result.data ? <Pagination page={page} totalPages={result.data.totalPages} totalElements={result.data.totalElements} onPageChange={setPage} /> : null}
      </div>
      <MovieDialog open={dialogOpen} movie={editing} pending={save.isPending} onOpenChange={setDialogOpen} onSubmit={(data) => save.mutate(data)} />
      <ConfirmActionDialog open={statusTarget !== null} title={`${statusTarget?.active ? "Deactivate" : "Reactivate"} movie?`} description={`${statusTarget?.title ?? "This movie"} will ${statusTarget?.active ? "no longer be available for new showtimes" : "be available for scheduling again"}.`} confirmLabel={statusTarget?.active ? "Deactivate movie" : "Reactivate movie"} pending={status.isPending} onOpenChange={(open) => { if (!open) setStatusTarget(null); }} onConfirm={() => { if (statusTarget) status.mutate({ id: statusTarget.id, value: !statusTarget.active }); }} />
    </>
  );
}

function MovieDialog({ open, movie, pending, onOpenChange, onSubmit }: {
  open: boolean; movie: AdminMovieResponse | null; pending: boolean;
  onOpenChange: (open: boolean) => void; onSubmit: (data: { title: string; director: string }) => void;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader><DialogTitle>{movie ? "Edit movie" : "Add movie"}</DialogTitle><DialogDescription>Movie details used in listings and showtimes.</DialogDescription></DialogHeader>
        <form id="movie-form" className="flex flex-col gap-4" onSubmit={(event) => {
          event.preventDefault();
          const data = new FormData(event.currentTarget);
          onSubmit({ title: String(data.get("title")), director: String(data.get("director")) });
        }}>
          <div className="flex flex-col gap-1.5"><Label htmlFor="movie-title">Title</Label><Input id="movie-title" name="title" defaultValue={movie?.title} required /></div>
          <div className="flex flex-col gap-1.5"><Label htmlFor="movie-director">Director</Label><Input id="movie-director" name="director" defaultValue={movie?.director} required /></div>
        </form>
        <DialogFooter showCloseButton><Button type="submit" form="movie-form" disabled={pending}>{pending ? "Saving…" : "Save movie"}</Button></DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
