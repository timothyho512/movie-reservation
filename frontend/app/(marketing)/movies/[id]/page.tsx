import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { getMovie } from "@/lib/api/movies";
import { ShowtimePicker } from "@/components/movies/ShowtimePicker";
import { ApiError } from "@/lib/api-client";
import { Film } from "lucide-react";

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
      <div className="flex gap-8 mb-10">
        <div className="hidden sm:flex w-40 shrink-0 aspect-[2/3] bg-muted rounded-lg items-center justify-center">
          <Film className="h-12 w-12 text-muted-foreground/40" />
        </div>
        <div>
          <h1 className="text-3xl font-bold mb-1">{movie.title}</h1>
          <p className="text-muted-foreground mb-4">Dir. {movie.director}</p>
        </div>
      </div>

      <section>
        <h2 className="text-xl font-semibold mb-4">Showtimes</h2>
        <ShowtimePicker showtimes={movie.showtimes ?? []} />
      </section>
    </div>
  );
}
