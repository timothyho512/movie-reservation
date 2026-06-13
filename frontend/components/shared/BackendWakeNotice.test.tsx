import { QueryClient, QueryClientProvider, useQuery } from "@tanstack/react-query";
import { act, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { BackendWakeNotice } from "./BackendWakeNotice";

function PendingQuery() {
  useQuery({
    queryKey: ["pending-backend-request"],
    queryFn: () => new Promise(() => undefined),
  });
  return null;
}

describe("BackendWakeNotice", () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it("appears when a client request remains pending", () => {
    vi.useFakeTimers();
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <PendingQuery />
        <BackendWakeNotice />
      </QueryClientProvider>
    );

    expect(screen.queryByText("The demo server is waking up")).not.toBeInTheDocument();

    act(() => {
      vi.advanceTimersByTime(4_000);
    });

    expect(screen.getByText("The demo server is waking up")).toBeInTheDocument();
  });
});
