"use client";

import { LoaderCircle, Server } from "lucide-react";
import { useEffect, useState } from "react";

const WAKE_MESSAGE_DELAY_MS = 4_000;

export function PageLoadingState() {
  const [showWakeMessage, setShowWakeMessage] = useState(false);

  useEffect(() => {
    const timer = window.setTimeout(() => setShowWakeMessage(true), WAKE_MESSAGE_DELAY_MS);
    return () => window.clearTimeout(timer);
  }, []);

  return (
    <div className="mx-auto flex min-h-[60vh] max-w-lg items-center justify-center px-6 text-center">
      <div role="status" aria-live="polite">
        {showWakeMessage ? (
          <Server className="mx-auto size-9 text-primary" />
        ) : (
          <LoaderCircle className="mx-auto size-9 animate-spin text-primary" />
        )}
        <p className="mt-4 font-medium">
          {showWakeMessage ? "The demo server is waking up" : "Loading CineBook"}
        </p>
        <p className="mt-2 text-sm leading-6 text-muted-foreground">
          {showWakeMessage
            ? "The free backend sleeps when inactive. It may take a few minutes to start, so please keep this page open."
            : "Fetching the latest cinema information…"}
        </p>
      </div>
    </div>
  );
}
