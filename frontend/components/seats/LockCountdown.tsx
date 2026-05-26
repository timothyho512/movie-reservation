"use client";

import { useLockCountdown } from "@/hooks/useLockCountdown";
import { Clock } from "lucide-react";
import { cn } from "@/lib/utils";

export function LockCountdown({ expiresAt }: { expiresAt: string }) {
  const { display, secondsLeft } = useLockCountdown(expiresAt);
  const isUrgent = secondsLeft > 0 && secondsLeft <= 60;

  return (
    <div
      className={cn(
        "flex items-center gap-1.5 text-sm font-medium",
        isUrgent ? "text-destructive" : "text-muted-foreground"
      )}
    >
      <Clock className="h-4 w-4" />
      <span>Seats held for {display}</span>
    </div>
  );
}
