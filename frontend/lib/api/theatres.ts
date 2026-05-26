import { apiFetch } from "@/lib/api-client";
import { TheatreDetailResponse, TheatreSummaryResponse } from "@/types/api";

export function getTheatres(): Promise<TheatreSummaryResponse[]> {
  return apiFetch<TheatreSummaryResponse[]>("/api/theatres", {
    next: { revalidate: 300 },
  } as RequestInit);
}

export function getTheatre(id: number): Promise<TheatreDetailResponse> {
  return apiFetch<TheatreDetailResponse>(`/api/theatres/${id}`, {
    next: { revalidate: 300 },
  } as RequestInit);
}
