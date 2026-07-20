import type { Metadata } from "next";
import { getMovies } from "@/lib/api/movies";
import { MovieCard } from "@/components/movies/MovieCard";

export const dynamic = "force-dynamic";
export const metadata: Metadata = { title: "Movies" };

export default async function MoviesPage() {
  const movies = await getMovies().catch(() => null);

  return (
    <div className="mx-auto max-w-7xl px-4 py-10">
      <h1 className="text-3xl font-bold mb-8">All Movies</h1>
      {movies === null ? (
        <div className="rounded-lg border bg-card p-6 text-card-foreground">
          <p className="font-medium">The demo server is waking up</p>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-muted-foreground">
            The free backend sleeps when inactive. It may take a few minutes to
            start, so please refresh this page shortly.
          </p>
        </div>
      ) : movies.length === 0 ? (
        <p className="text-muted-foreground">No movies available.</p>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
          {movies.map((movie) => (
            <MovieCard key={movie.id} movie={movie} />
          ))}
        </div>
      )}
    </div>
  );
}
