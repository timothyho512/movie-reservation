import Link from "next/link";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { buttonVariants } from "@/components/ui/button";
import { MovieCardResponse } from "@/types/api";
import { Film } from "lucide-react";
import { cn } from "@/lib/utils";

export function MovieCard({ movie }: { movie: MovieCardResponse }) {
  return (
    <Card className="flex flex-col h-full hover:shadow-md transition-shadow">
      <div className="aspect-[2/3] bg-muted rounded-t-lg flex items-center justify-center">
        <Film className="h-12 w-12 text-muted-foreground/40" />
      </div>
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
