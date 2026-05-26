"use client";

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

export function useCheckoutPolling(checkoutReference: string | null) {
  return useQuery({
    queryKey: queryKeys.checkout.status(checkoutReference ?? ""),
    queryFn: () => getCheckoutStatus(checkoutReference!),
    enabled: !!checkoutReference,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (status && TERMINAL_STATUSES.includes(status)) return false;
      return 2000;
    },
    refetchIntervalInBackground: false,
  });
}
