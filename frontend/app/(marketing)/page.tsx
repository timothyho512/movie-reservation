export const dynamic = "force-dynamic";

import Link from "next/link";
import { getMovies } from "@/lib/api/movies";
import { getShowtimes } from "@/lib/api/showtimes";
import { MovieCard } from "@/components/movies/MovieCard";
import { buttonVariants } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { bookingCutoffEpoch, formatDate, formatTime, formatPrice } from "@/lib/format";
import { ChevronRight, Film, MapPin } from "lucide-react";
import { cn } from "@/lib/utils";

export default async function HomePage() {
  const [movies, showtimes] = await Promise.all([
    getMovies().catch(() => []),
    getShowtimes().catch(() => []),
  ]);

  const featured = movies.slice(0, 5);
  const upcoming = showtimes
    .filter(
      (s) =>
        s.status === "UPCOMING" &&
        new Date(s.startTime).getTime() > bookingCutoffEpoch()
    )
    .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime())
    .slice(0, 8);

  return (
    <div>
      {/* Hero */}
      <section className="bg-gradient-to-b from-primary/10 to-background py-20 px-4 text-center">
        <h1 className="text-4xl sm:text-5xl font-bold tracking-tight mb-4">
          Book Your Perfect Movie Night
        </h1>
        <p className="text-muted-foreground text-lg mb-8 max-w-xl mx-auto">
          Browse the latest films, pick your seats, and pay securely — all in a
          few clicks.
        </p>
        <Link href="/movies" className={cn(buttonVariants({ size: "lg" }))}>
          Browse All Movies
        </Link>
      </section>

      <div className="mx-auto max-w-7xl px-4 py-12 space-y-14">
        {/* Featured movies */}
        <section>
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-2xl font-semibold">Now Showing</h2>
            <Link
              href="/movies"
              className={cn(
                buttonVariants({ variant: "ghost", size: "sm" }),
                "flex items-center gap-1"
              )}
            >
              All movies <ChevronRight className="h-4 w-4" />
            </Link>
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
            {featured.map((movie) => (
              <MovieCard key={movie.id} movie={movie} />
            ))}
          </div>
        </section>

        {/* Upcoming showtimes */}
        {upcoming.length > 0 && (
          <section>
            <h2 className="text-2xl font-semibold mb-6">Upcoming Showtimes</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
              {upcoming.map((s) => (
                <Link
                  key={s.id}
                  href={`/showtimes/${s.id}/seats`}
                  className="group border rounded-lg p-4 hover:shadow-md hover:border-primary/40 transition-all"
                >
                  <div className="flex items-start gap-3">
                    <div className="p-2 rounded-md bg-primary/10">
                      <Film className="h-4 w-4 text-primary" />
                    </div>
                    <div className="min-w-0">
                      <p className="font-medium text-sm line-clamp-1 group-hover:text-primary transition-colors">
                        {s.movie.title}
                      </p>
                      <p className="text-xs text-muted-foreground flex items-center gap-1 mt-0.5">
                        <MapPin className="h-3 w-3" />
                        {s.theatre.name}
                      </p>
                      <div className="flex items-center gap-2 mt-1.5">
                        <span className="text-xs font-medium">
                          {formatDate(s.startTime)}
                        </span>
                        <span className="text-xs text-muted-foreground">
                          {formatTime(s.startTime)}
                        </span>
                      </div>
                      <div className="flex items-center gap-2 mt-1">
                        <Badge variant="secondary" className="text-[10px]">
                          {s.screen.screenType}
                        </Badge>
                        <span className="text-xs text-muted-foreground">
                          {formatPrice(s.basePrice)} · {s.availableSeats} left
                        </span>
                      </div>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          </section>
        )}
      </div>
    </div>
  );
}
