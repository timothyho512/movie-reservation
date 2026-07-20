"use client";

import { useState } from "react";
import Image from "next/image";
import { Film } from "lucide-react";
import { cn } from "@/lib/utils";

export function MoviePoster({
  title,
  posterPath,
  className,
  sizes = "(max-width: 640px) 50vw, (max-width: 1024px) 25vw, 20vw",
}: {
  title: string;
  posterPath: string | null;
  className?: string;
  sizes?: string;
}) {
  const [failed, setFailed] = useState(false);
  const showImage = Boolean(posterPath) && !failed;

  return (
    <div
      className={cn(
        "relative overflow-hidden bg-gradient-to-br from-slate-900 via-slate-800 to-primary/60",
        className
      )}
    >
      {showImage ? (
        <Image
          src={`https://image.tmdb.org/t/p/w500${posterPath}`}
          alt={`${title} poster`}
          fill
          sizes={sizes}
          className="object-cover transition-transform duration-300 group-hover:scale-[1.03]"
          onError={() => setFailed(true)}
        />
      ) : (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 p-4 text-center text-white/75">
          <Film className="h-12 w-12" aria-hidden="true" />
          <span className="text-sm font-medium leading-tight">{title}</span>
        </div>
      )}
    </div>
  );
}
