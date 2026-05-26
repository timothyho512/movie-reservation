"use client";

import { SeatMapSeatSummary } from "@/types/api";
import { SeatButton } from "./SeatButton";
import { SeatLegend } from "./SeatLegend";

function groupByRow(seats: SeatMapSeatSummary[]) {
  const map = new Map<string, SeatMapSeatSummary[]>();
  for (const seat of seats) {
    const row = seat.rowLabel;
    const existing = map.get(row) ?? [];
    existing.push(seat);
    map.set(row, existing);
  }
  return map;
}

export function SeatMap({
  seats,
  selectedIds,
  onToggle,
}: {
  seats: SeatMapSeatSummary[];
  selectedIds: Set<number>;
  onToggle: (seatId: number) => void;
}) {
  const byRow = groupByRow(seats);

  return (
    <div className="space-y-6">
      {/* Screen indicator */}
      <div className="relative mx-auto max-w-md">
        <div className="h-2 bg-muted rounded-full" />
        <p className="text-center text-xs text-muted-foreground mt-1">SCREEN</p>
      </div>

      {/* Seat grid */}
      <div className="overflow-x-auto">
        <div className="inline-block min-w-full">
          {Array.from(byRow.entries()).map(([rowLabel, rowSeats]) => {
            const maxSeat = Math.max(...rowSeats.map((s) => s.seatNumber));
            const seatByNumber = new Map(rowSeats.map((s) => [s.seatNumber, s]));

            return (
              <div key={rowLabel} className="flex items-center gap-1 mb-1">
                <span className="w-5 text-xs font-medium text-muted-foreground text-right shrink-0">
                  {rowLabel}
                </span>
                <div className="flex gap-1">
                  {Array.from({ length: maxSeat }, (_, i) => i + 1).map((num) => {
                    const seat = seatByNumber.get(num);
                    if (!seat) {
                      // Gap/aisle spacer
                      return <div key={num} className="h-8 w-8" />;
                    }

                    const isSelected = selectedIds.has(seat.id);
                    const state = isSelected
                      ? "selected"
                      : seat.available
                        ? "available"
                        : "unavailable";

                    return (
                      <SeatButton
                        key={seat.id}
                        rowLabel={rowLabel}
                        seatNumber={num}
                        seatType={seat.seatType}
                        state={state}
                        onClick={seat.available ? () => onToggle(seat.id) : undefined}
                      />
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      <SeatLegend />
    </div>
  );
}
