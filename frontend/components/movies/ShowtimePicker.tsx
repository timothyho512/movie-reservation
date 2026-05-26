import Link from "next/link";
import { ShowtimeSummaryResponse } from "@/types/api";
import { formatDate, formatDateKey, formatPrice, formatTime } from "@/lib/format";
import { Badge } from "@/components/ui/badge";

function groupByDate(showtimes: ShowtimeSummaryResponse[]) {
  const map = new Map<string, ShowtimeSummaryResponse[]>();
  for (const s of showtimes) {
    const key = formatDateKey(s.startTime);
    const existing = map.get(key) ?? [];
    existing.push(s);
    map.set(key, existing);
  }
  return map;
}

const screenTypeClass: Record<string, string> = {
  IMAX: "bg-blue-100 text-blue-800",
  DOLBY_ATMOS: "bg-purple-100 text-purple-800",
  FOUR_DX: "bg-orange-100 text-orange-800",
  THREE_D: "bg-green-100 text-green-800",
  VIP: "bg-yellow-100 text-yellow-800",
  STANDARD: "",
};

export function ShowtimePicker({
  showtimes,
}: {
  showtimes: ShowtimeSummaryResponse[];
}) {
  const upcoming = showtimes.filter(
    (s) => s.status === "UPCOMING" || s.status === "ONGOING"
  );

  if (upcoming.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">No upcoming showtimes.</p>
    );
  }

  const byDate = groupByDate(upcoming);

  return (
    <div className="space-y-6">
      {Array.from(byDate.entries()).map(([date, items]) => (
        <div key={date}>
          <p className="text-sm font-medium text-muted-foreground mb-2">
            {formatDate(items[0].startTime)}
          </p>
          <div className="flex flex-wrap gap-2">
            {items.map((s) => (
              <Link
                key={s.id}
                href={`/showtimes/${s.id}/seats`}
                className="inline-flex flex-col items-center gap-0.5 rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted transition-colors"
              >
                <span className="font-semibold">{formatTime(s.startTime)}</span>
                <span className="text-xs text-muted-foreground flex items-center gap-1">
                  <Badge
                    variant="secondary"
                    className={`text-[10px] px-1 py-0 ${screenTypeClass[s.screen.screenType] ?? ""}`}
                  >
                    {s.screen.screenType}
                  </Badge>
                  {formatPrice(s.basePrice)}
                </span>
                <span className="text-xs text-muted-foreground">
                  {s.availableSeats} seats left
                </span>
              </Link>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
