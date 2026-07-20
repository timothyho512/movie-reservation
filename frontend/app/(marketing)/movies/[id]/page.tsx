import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { getMovie } from "@/lib/api/movies";
import { ShowtimePicker } from "@/components/movies/ShowtimePicker";
import { ApiError } from "@/lib/api-client";
import { MoviePoster } from "@/components/movies/MoviePoster";

type Props = { params: Promise<{ id: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id } = await params;
  try {
    const movie = await getMovie(Number(id));
    return { title: movie.title };
  } catch {
    return { title: "Movie" };
  }
}

export default async function MovieDetailPage({ params }: Props) {
  const { id } = await params;
  let movie;

  try {
    movie = await getMovie(Number(id));
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) notFound();
    throw e;
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-10">
      <div className="mb-10 flex gap-8">
        <MoviePoster
          title={movie.title}
          posterPath={movie.posterPath}
          className="hidden aspect-[2/3] w-40 shrink-0 rounded-lg sm:block"
          sizes="160px"
        />
        <div className="max-w-2xl">
          <h1 className="text-3xl font-bold mb-1">{movie.title}</h1>
          <p className="text-muted-foreground mb-4">Dir. {movie.director}</p>
          {movie.runtimeMinutes ? (
            <p className="mb-4 text-sm text-muted-foreground">{movie.runtimeMinutes} minutes</p>
          ) : null}
          {movie.overview ? <p className="leading-7 text-muted-foreground">{movie.overview}</p> : null}
        </div>
      </div>

      <section>
        <h2 className="text-xl font-semibold mb-4">Showtimes</h2>
        <ShowtimePicker showtimes={movie.showtimes ?? []} />
      </section>
    </div>
  );
}
