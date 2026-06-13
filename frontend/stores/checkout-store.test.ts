import { beforeEach, describe, expect, it } from "vitest";

import { useCheckoutStore } from "./checkout-store";

describe("checkout store", () => {
  beforeEach(() => {
    useCheckoutStore.getState().reset();
  });

  it("clears checkout state when switching showtimes", () => {
    const store = useCheckoutStore.getState();
    store.setShowtimeId(1);
    store.toggleSeat(60);
    store.setLock("session-1", "2026-06-13T12:00:00Z");
    store.setCheckoutReference("checkout-1");

    useCheckoutStore.getState().setShowtimeId(2);

    const next = useCheckoutStore.getState();
    expect(next.showtimeId).toBe(2);
    expect(next.selectedSeatIds.size).toBe(0);
    expect(next.sessionId).toBeNull();
    expect(next.expiresAt).toBeNull();
    expect(next.checkoutReference).toBeNull();
  });

  it("preserves checkout state when the showtime is unchanged", () => {
    const store = useCheckoutStore.getState();
    store.setShowtimeId(1);
    store.toggleSeat(30);

    useCheckoutStore.getState().setShowtimeId(1);

    expect(useCheckoutStore.getState().selectedSeatIds).toEqual(new Set([30]));
  });
});
