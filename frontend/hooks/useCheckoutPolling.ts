"use client";

import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { getCheckoutStatus } from "@/lib/api/checkout";
import { queryKeys } from "@/lib/query-keys";
import { CheckoutSessionStatus } from "@/types/api";

const TERMINAL_STATUSES: CheckoutSessionStatus[] = [
  "FINALIZED",
  "EXPIRED",
  "FAILED",
  "CANCELLED",
];

const POLL_TIMEOUT_MS = 2 * 60 * 1000; // 2 minutes

export function useCheckoutPolling(checkoutReference: string | null) {
  const [startedAt, setStartedAt] = useState<number | null>(null);
  const [now, setNow] = useState<number | null>(null);

  useEffect(() => {
    const resetId = window.setTimeout(() => {
      const timestamp = checkoutReference ? Date.now() : null;
      setStartedAt(timestamp);
      setNow(timestamp);
    }, 0);

    const intervalId = window.setInterval(() => {
      setNow(Date.now());
    }, 1000);

    return () => {
      window.clearTimeout(resetId);
      window.clearInterval(intervalId);
    };
  }, [checkoutReference]);

  const query = useQuery({
    queryKey: queryKeys.checkout.status(checkoutReference ?? ""),
    queryFn: () => getCheckoutStatus(checkoutReference!),
    enabled: !!checkoutReference,
    refetchInterval: (q) => {
      const status = q.state.data?.status;
      if (status && TERMINAL_STATUSES.includes(status)) return false;
      if (startedAt && now && now - startedAt >= POLL_TIMEOUT_MS) return false;
      return 2000;
    },
    refetchIntervalInBackground: false,
  });

  const isTimedOut =
    !query.data ||
    (!TERMINAL_STATUSES.includes(query.data.status) &&
      !!startedAt &&
      !!now &&
      now - startedAt >= POLL_TIMEOUT_MS);

  return { ...query, isTimedOut };
}
