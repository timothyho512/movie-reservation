import type { Metadata } from "next";
import { getMovies } from "@/lib/api/movies";
import { MovieCard } from "@/components/movies/MovieCard";

export const dynamic = "force-dynamic";
export const metadata: Metadata = { title: "Movies" };

export default async function MoviesPage() {
  const movies = await getMovies();

  return (
    <div className="mx-auto max-w-7xl px-4 py-10">
      <h1 className="text-3xl font-bold mb-8">All Movies</h1>
      {movies.length === 0 ? (
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
