"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useState } from "react";
import { ApiError } from "@/lib/api-client";
import { BackendWakeNotice } from "@/components/shared/BackendWakeNotice";
import { Toaster } from "@/components/ui/sonner";

const TEMPORARY_BACKEND_STATUSES = new Set([502, 503, 504]);

export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 60 * 1000,
            retry: (failureCount, error) =>
              failureCount < 20 &&
              (error instanceof TypeError ||
                (error instanceof ApiError && TEMPORARY_BACKEND_STATUSES.has(error.status))),
            retryDelay: (attempt) => Math.min(2_000 * 1.5 ** attempt, 15_000),
          },
        },
      })
  );

  return (
    <QueryClientProvider client={queryClient}>
      {children}
      <BackendWakeNotice />
      <Toaster richColors position="top-right" />
    </QueryClientProvider>
  );
}
