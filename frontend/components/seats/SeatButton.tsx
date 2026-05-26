"use client";

import { SeatType } from "@/types/api";
import { cn } from "@/lib/utils";

type SeatState = "available" | "selected" | "unavailable";

const seatTypeStyle: Record<SeatType, string> = {
  REGULAR: "bg-slate-200 hover:bg-slate-300 text-slate-700",
  VIP: "bg-amber-200 hover:bg-amber-300 text-amber-800",
  WHEELCHAIR: "bg-sky-200 hover:bg-sky-300 text-sky-800",
};

const selectedStyle = "bg-primary text-primary-foreground hover:bg-primary/90 ring-2 ring-primary ring-offset-1";
const unavailableStyle = "bg-muted text-muted-foreground cursor-not-allowed opacity-50";

export function SeatButton({
  rowLabel,
  seatNumber,
  seatType,
  state,
  onClick,
}: {
  rowLabel: string;
  seatNumber: number;
  seatType: SeatType;
  state: SeatState;
  onClick?: () => void;
}) {
  const isDisabled = state === "unavailable";

  return (
    <button
      type="button"
      disabled={isDisabled}
      onClick={onClick}
      aria-label={`Seat ${rowLabel}${seatNumber} — ${seatType} — ${state}`}
      aria-pressed={state === "selected"}
      className={cn(
        "h-8 w-8 rounded text-[10px] font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
        state === "selected"
          ? selectedStyle
          : state === "unavailable"
            ? unavailableStyle
            : seatTypeStyle[seatType]
      )}
    >
      {seatNumber}
    </button>
  );
}
