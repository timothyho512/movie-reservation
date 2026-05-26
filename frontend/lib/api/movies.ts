import { apiFetch } from "@/lib/api-client";
import { MovieCardResponse, MovieDetailResponse } from "@/types/api";

export function getMovies(): Promise<MovieCardResponse[]> {
  return apiFetch<MovieCardResponse[]>("/api/movies");
}

export function getMovie(id: number): Promise<MovieDetailResponse> {
  return apiFetch<MovieDetailResponse>(`/api/movies/${id}`);
}
