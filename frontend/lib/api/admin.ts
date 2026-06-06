import { clientFetch } from "@/lib/api-client";
import type {
  AdminMovieResponse,
  AdminScreenResponse,
  AdminSeatDefinition,
  AdminSeatLayoutResponse,
  AdminShowtimeResponse,
  AdminTheatreResponse,
  CancelledBookingReportRow,
  CheckoutConversionReportRow,
  MovieRevenueReportRow,
  PageResponse,
  PopularSeatReportRow,
  ScreenType,
  ShowtimeOccupancyReportRow,
  ShowtimeStatus,
} from "@/types/api";

type QueryValue = string | number | boolean | null | undefined;

function query(params: Record<string, QueryValue>) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      search.set(key, String(value));
    }
  });
  const value = search.toString();
  return value ? `?${value}` : "";
}

export const adminApi = {
  movies: (params: Record<string, QueryValue> = {}) =>
    clientFetch<PageResponse<AdminMovieResponse>>(`/api/admin/movies${query(params)}`),
  createMovie: (data: { title: string; director: string }) =>
    clientFetch("/api/movies", { method: "POST", body: JSON.stringify(data) }),
  updateMovie: (id: number, data: { title: string; director: string }) =>
    clientFetch(`/api/movies/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  setMovieActive: (id: number, active: boolean) =>
    clientFetch<AdminMovieResponse>(`/api/admin/movies/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ active }),
    }),

  theatres: (params: Record<string, QueryValue> = {}) =>
    clientFetch<PageResponse<AdminTheatreResponse>>(`/api/admin/theatres${query(params)}`),
  createTheatre: (data: Record<string, unknown>) =>
    clientFetch("/api/theatres", { method: "POST", body: JSON.stringify(data) }),
  updateTheatre: (id: number, data: Record<string, unknown>) =>
    clientFetch(`/api/theatres/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  setTheatreActive: (id: number, active: boolean) =>
    clientFetch<AdminTheatreResponse>(`/api/admin/theatres/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ active }),
    }),

  screens: (params: Record<string, QueryValue> = {}) =>
    clientFetch<PageResponse<AdminScreenResponse>>(`/api/admin/screens${query(params)}`),
  createScreen: (data: { name: string; theatreId: number; screenType: ScreenType }) =>
    clientFetch("/api/screens", { method: "POST", body: JSON.stringify(data) }),
  updateScreen: (id: number, data: { name: string; theatreId: number; screenType: ScreenType }) =>
    clientFetch(`/api/screens/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  setScreenActive: (id: number, active: boolean) =>
    clientFetch<AdminScreenResponse>(`/api/admin/screens/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ active }),
    }),
  currentLayout: (screenId: number) =>
    clientFetch<AdminSeatLayoutResponse>(`/api/admin/screens/${screenId}/seat-layout`),
  replaceLayout: (screenId: number, seats: AdminSeatDefinition[]) =>
    clientFetch<AdminSeatLayoutResponse>(`/api/admin/screens/${screenId}/seat-layout`, {
      method: "POST",
      body: JSON.stringify({ seats }),
    }),

  showtimes: (params: Record<string, QueryValue> = {}) =>
    clientFetch<PageResponse<AdminShowtimeResponse>>(`/api/admin/showtimes${query(params)}`),
  createShowtime: (data: Record<string, unknown>) =>
    clientFetch("/api/showtimes", { method: "POST", body: JSON.stringify(data) }),
  updateShowtime: (id: number, data: Record<string, unknown>) =>
    clientFetch(`/api/showtimes/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  setShowtimeStatus: (id: number, status: ShowtimeStatus) =>
    clientFetch<AdminShowtimeResponse>(`/api/admin/showtimes/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status }),
    }),

  occupancy: (params: Record<string, QueryValue>) =>
    clientFetch<PageResponse<ShowtimeOccupancyReportRow>>(`/api/admin/reports/showtimes/occupancy${query(params)}`),
  revenue: (params: Record<string, QueryValue>) =>
    clientFetch<PageResponse<MovieRevenueReportRow>>(`/api/admin/reports/movies/revenue${query(params)}`),
  cancelled: (params: Record<string, QueryValue>) =>
    clientFetch<PageResponse<CancelledBookingReportRow>>(`/api/admin/reports/bookings/cancelled${query(params)}`),
  popularSeats: (params: Record<string, QueryValue>) =>
    clientFetch<PageResponse<PopularSeatReportRow>>(`/api/admin/reports/seats/popular${query(params)}`),
  conversion: (params: Record<string, QueryValue>) =>
    clientFetch<PageResponse<CheckoutConversionReportRow>>(`/api/admin/reports/checkout/conversion${query(params)}`),
};
