import Image from "next/image";

export function SiteFooter() {
  return (
    <footer className="mt-auto border-t bg-background px-4 py-5 text-xs text-muted-foreground">
      <div className="mx-auto flex max-w-7xl flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <span>Copyright © 2026 Timothy Ho.</span>
        <div className="flex max-w-xl items-center gap-3 sm:justify-end">
          <a href="https://www.themoviedb.org" target="_blank" rel="noreferrer" aria-label="The Movie Database">
            <Image src="/tmdb-logo.svg" alt="TMDB" width={109} height={14} />
          </a>
          <span>
            This product uses the TMDB API but is not endorsed or certified by TMDB.
          </span>
        </div>
      </div>
    </footer>
  );
}
