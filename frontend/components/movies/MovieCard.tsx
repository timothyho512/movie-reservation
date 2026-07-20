import Link from "next/link";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { buttonVariants } from "@/components/ui/button";
import { MovieCardResponse } from "@/types/api";
import { cn } from "@/lib/utils";
import { MoviePoster } from "@/components/movies/MoviePoster";

export function MovieCard({ movie }: { movie: MovieCardResponse }) {
  return (
    <Card className="group flex h-full flex-col overflow-hidden transition-shadow hover:shadow-lg">
      <MoviePoster
        title={movie.title}
        posterPath={movie.posterPath}
        className="aspect-[2/3] w-full"
      />
      <CardHeader className="pb-2">
        <h3 className="font-semibold text-base leading-tight line-clamp-2">
          {movie.title}
        </h3>
        <p className="text-sm text-muted-foreground">Dir. {movie.director}</p>
      </CardHeader>
      <CardContent className="mt-auto">
        <Link
          href={`/movies/${movie.id}`}
          className={cn(buttonVariants({ size: "sm" }), "w-full")}
        >
          View Showtimes
        </Link>
      </CardContent>
    </Card>
  );
}
