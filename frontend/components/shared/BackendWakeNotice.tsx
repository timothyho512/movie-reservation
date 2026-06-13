"use client";

import { useIsFetching } from "@tanstack/react-query";
import { LoaderCircle, Server } from "lucide-react";
import { useEffect, useState } from "react";

const WAKE_NOTICE_DELAY_MS = 4_000;

export function BackendWakeNotice() {
  const activeRequests = useIsFetching();

  return activeRequests > 0 ? <DelayedWakeNotice /> : null;
}

function DelayedWakeNotice() {
  const [showNotice, setShowNotice] = useState(false);

  useEffect(() => {
    const timer = window.setTimeout(() => setShowNotice(true), WAKE_NOTICE_DELAY_MS);
    return () => window.clearTimeout(timer);
  }, []);

  if (!showNotice) return null;

  return (
    <div
      role="status"
      aria-live="polite"
      className="fixed bottom-4 left-1/2 z-50 flex w-[calc(100%-2rem)] max-w-md -translate-x-1/2 gap-3 rounded-lg border bg-background p-4 shadow-lg"
    >
      <div className="relative mt-0.5 shrink-0">
        <Server className="size-5 text-primary" />
        <LoaderCircle className="absolute -bottom-1 -right-1 size-3 animate-spin text-primary" />
      </div>
      <div>
        <p className="text-sm font-medium">The demo server is waking up</p>
        <p className="mt-1 text-xs leading-5 text-muted-foreground">
          This project uses a free backend that sleeps when inactive. Loading may
          take a few minutes, so please keep this page open.
        </p>
      </div>
    </div>
  );
}
