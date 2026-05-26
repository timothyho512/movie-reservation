import { clientFetch } from "@/lib/api-client";
import { ReservationResponse } from "@/types/api";

export function getReservations(): Promise<ReservationResponse[]> {
  return clientFetch<ReservationResponse[]>("/api/reservations");
}

export function getReservation(id: number): Promise<ReservationResponse> {
  return clientFetch<ReservationResponse>(`/api/reservations/${id}`);
}
