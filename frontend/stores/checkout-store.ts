"use client";

import { create } from "zustand";

interface CheckoutState {
  showtimeId: number | null;
  selectedSeatIds: Set<number>;
  sessionId: string | null;
  expiresAt: string | null;
  checkoutReference: string | null;

  setShowtimeId: (id: number) => void;
  toggleSeat: (seatId: number) => void;
  clearSelection: () => void;
  setLock: (sessionId: string | null, expiresAt: string) => void;
  setCheckoutReference: (ref: string) => void;
  reset: () => void;
}

export const useCheckoutStore = create<CheckoutState>()((set) => ({
  showtimeId: null,
  selectedSeatIds: new Set(),
  sessionId: null,
  expiresAt: null,
  checkoutReference: null,

  setShowtimeId: (id) => set({ showtimeId: id }),

  toggleSeat: (seatId) =>
    set((state) => {
      const next = new Set(state.selectedSeatIds);
      if (next.has(seatId)) {
        next.delete(seatId);
      } else {
        next.add(seatId);
      }
      return { selectedSeatIds: next };
    }),

  clearSelection: () => set({ selectedSeatIds: new Set() }),

  setLock: (sessionId, expiresAt) => set({ sessionId, expiresAt }),

  setCheckoutReference: (ref) => set({ checkoutReference: ref }),

  reset: () =>
    set({
      showtimeId: null,
      selectedSeatIds: new Set(),
      sessionId: null,
      expiresAt: null,
      checkoutReference: null,
    }),
}));
