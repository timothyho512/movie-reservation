"use client";

import { SeatMapSeatSummary } from "@/types/api";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { formatPrice } from "@/lib/format";

export function SeatSummaryPanel({
  selectedSeats,
  onContinue,
  isLoading,
  isLocked,
}: {
  selectedSeats: SeatMapSeatSummary[];
  onContinue: () => void;
  isLoading: boolean;
  isLocked: boolean;
}) {
  const total = selectedSeats.reduce(
    (sum, s) => sum + parseFloat(s.price),
    0
  );

  return (
    <div className="border rounded-lg p-4 space-y-4 bg-card">
      <h3 className="font-semibold">Your Selection</h3>

      {selectedSeats.length === 0 ? (
        <p className="text-sm text-muted-foreground">No seats selected</p>
      ) : (
        <ul className="space-y-1.5 text-sm">
          {selectedSeats.map((seat) => (
            <li key={seat.id} className="flex justify-between">
              <span className="text-muted-foreground">
                {seat.rowLabel}
                {seat.seatNumber}{" "}
                <span className="capitalize text-xs">({seat.seatType.toLowerCase()})</span>
              </span>
              <span className="font-medium">{formatPrice(seat.price)}</span>
            </li>
          ))}
        </ul>
      )}

      {selectedSeats.length > 0 && (
        <>
          <Separator />
          <div className="flex justify-between font-semibold">
            <span>Total</span>
            <span>{formatPrice(total)}</span>
          </div>
        </>
      )}

      <Button
        className="w-full"
        disabled={selectedSeats.length === 0 || isLoading}
        onClick={onContinue}
      >
        {isLoading
          ? "Holding seats…"
          : isLocked
            ? "Pay Now"
            : "Continue to Checkout"}
      </Button>
    </div>
  );
}
