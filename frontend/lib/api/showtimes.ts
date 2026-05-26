import { apiFetch } from "@/lib/api-client";
import { SeatMapResponse, ShowtimeSummaryResponse } from "@/types/api";

export function getShowtimes(): Promise<ShowtimeSummaryResponse[]> {
  return apiFetch<ShowtimeSummaryResponse[]>("/api/showtimes");
}

export function getShowtime(id: number): Promise<ShowtimeSummaryResponse> {
  return apiFetch<ShowtimeSummaryResponse>(`/api/showtimes/${id}`);
}

export function getSeatMap(showtimeId: number): Promise<SeatMapResponse> {
  return apiFetch<SeatMapResponse>(`/api/showtimes/${showtimeId}/seat-map`);
}
