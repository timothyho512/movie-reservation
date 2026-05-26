"use client";

import { useEffect, useState } from "react";
import { formatCountdown, secondsUntil } from "@/lib/format";

export function useLockCountdown(expiresAt: string | null): {
  display: string;
  isExpired: boolean;
  secondsLeft: number;
} {
  // Use a tick counter to force re-renders each second; derive secondsLeft
  // directly from expiresAt on every render so there is no stale-state
  // window where isExpired is incorrectly true right after the lock is set.
  const [, setTick] = useState(0);

  useEffect(() => {
    if (!expiresAt) return;

    const interval = setInterval(() => {
      setTick((n) => n + 1);
    }, 1000);

    return () => clearInterval(interval);
  }, [expiresAt]);

  const secondsLeft = expiresAt ? Math.max(0, secondsUntil(expiresAt)) : 0;

  return {
    display: formatCountdown(secondsLeft),
    isExpired: !!expiresAt && secondsLeft <= 0,
    secondsLeft,
  };
}
