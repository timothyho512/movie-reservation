"use client";

import { useRef } from "react";
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
  const startedAt = useRef(Date.now());

  const query = useQuery({
    queryKey: queryKeys.checkout.status(checkoutReference ?? ""),
    queryFn: () => getCheckoutStatus(checkoutReference!),
    enabled: !!checkoutReference,
    refetchInterval: (q) => {
      const status = q.state.data?.status;
      if (status && TERMINAL_STATUSES.includes(status)) return false;
      if (Date.now() - startedAt.current >= POLL_TIMEOUT_MS) return false;
      return 2000;
    },
    refetchIntervalInBackground: false,
  });

  const isTimedOut =
    !query.data ||
    (!TERMINAL_STATUSES.includes(query.data.status) &&
      Date.now() - startedAt.current >= POLL_TIMEOUT_MS);

  return { ...query, isTimedOut };
}
