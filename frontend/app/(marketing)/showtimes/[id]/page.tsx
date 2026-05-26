import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { getShowtime } from "@/lib/api/showtimes";
import { ApiError } from "@/lib/api-client";
import { Badge } from "@/components/ui/badge";
import { buttonVariants } from "@/components/ui/button";
import { formatDate, formatPrice, formatTime } from "@/lib/format";
import { Clock, MapPin } from "lucide-react";
import { cn } from "@/lib/utils";

type Props = { params: Promise<{ id: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id } = await params;
  try {
    const s = await getShowtime(Number(id));
    return { title: `${s.movie.title} — ${formatDate(s.startTime)}` };
  } catch {
    return { title: "Showtime" };
  }
}

export default async function ShowtimeDetailPage({ params }: Props) {
  const { id } = await params;
  let showtime;

  try {
    showtime = await getShowtime(Number(id));
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) notFound();
    throw e;
  }

  const isCancelled = showtime.status === "CANCELLED";

  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <div className="mb-2">
        <Link
          href={`/movies/${showtime.movie.id}`}
          className="text-sm text-muted-foreground hover:underline"
        >
          ← Back to {showtime.movie.title}
        </Link>
      </div>
      <h1 className="text-3xl font-bold mb-1">{showtime.movie.title}</h1>
      <p className="text-muted-foreground mb-6">Dir. {showtime.movie.director}</p>

      <div className="border rounded-lg p-6 space-y-4 mb-8">
        <div className="flex flex-wrap gap-4 text-sm">
          <div className="flex items-center gap-1.5 text-muted-foreground">
            <Clock className="h-4 w-4" />
            <span>
              {formatDate(showtime.startTime)} · {formatTime(showtime.startTime)}
              {" → "}
              {formatTime(showtime.endTime)}
            </span>
          </div>
          <div className="flex items-center gap-1.5 text-muted-foreground">
            <MapPin className="h-4 w-4" />
            <Link
              href={`/theatres/${showtime.theatre.id}`}
              className="hover:underline"
            >
              {showtime.theatre.name}, {showtime.theatre.city}
            </Link>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <Badge variant="secondary">{showtime.screen.screenType}</Badge>
          <Badge variant="secondary">{showtime.screen.name}</Badge>
          <Badge variant={isCancelled ? "destructive" : "default"}>
            {showtime.status}
          </Badge>
        </div>

        <div className="flex items-center justify-between pt-2 border-t">
          <div>
            <p className="text-sm text-muted-foreground">From</p>
            <p className="text-2xl font-bold">{formatPrice(showtime.basePrice)}</p>
          </div>
          <div className="text-right">
            <p className="text-sm text-muted-foreground">
              {showtime.availableSeats} of {showtime.totalSeats} seats available
            </p>
            {!isCancelled && (
              <Link
                href={`/showtimes/${showtime.id}/seats`}
                className={cn(buttonVariants(), "mt-2 inline-flex")}
              >
                Select Seats
              </Link>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
