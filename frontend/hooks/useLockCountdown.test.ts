import { act, renderHook } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { useLockCountdown } from "./useLockCountdown";

describe("useLockCountdown", () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it("counts down to expiry", () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-06-11T12:00:00.000Z"));

    const { result } = renderHook(() =>
      useLockCountdown("2026-06-11T12:01:05.000Z")
    );

    expect(result.current.secondsLeft).toBe(65);
    expect(result.current.display).toBe("01:05");
    expect(result.current.isExpired).toBe(false);

    act(() => {
      vi.advanceTimersByTime(5000);
    });

    expect(result.current.secondsLeft).toBe(60);
    expect(result.current.display).toBe("01:00");
  });

  it("reports an expired lock", () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-06-11T12:00:00.000Z"));

    const { result } = renderHook(() =>
      useLockCountdown("2026-06-11T11:59:59.000Z")
    );

    expect(result.current.secondsLeft).toBe(0);
    expect(result.current.display).toBe("00:00");
    expect(result.current.isExpired).toBe(true);
  });

  it("treats a missing expiry as inactive rather than expired", () => {
    const { result } = renderHook(() => useLockCountdown(null));

    expect(result.current.secondsLeft).toBe(0);
    expect(result.current.display).toBe("00:00");
    expect(result.current.isExpired).toBe(false);
  });
});
