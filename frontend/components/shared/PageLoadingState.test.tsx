import { act, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { PageLoadingState } from "./PageLoadingState";

describe("PageLoadingState", () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it("explains the backend cold start after a short delay", () => {
    vi.useFakeTimers();
    render(<PageLoadingState />);

    expect(screen.getByText("Loading CineBook")).toBeInTheDocument();
    expect(screen.queryByText("The demo server is waking up")).not.toBeInTheDocument();

    act(() => {
      vi.advanceTimersByTime(4_000);
    });

    expect(screen.getByText("The demo server is waking up")).toBeInTheDocument();
    expect(screen.getByText(/free backend sleeps when inactive/i)).toBeInTheDocument();
  });
});
