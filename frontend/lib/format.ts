/**
 * Format a price string (BigDecimal serialised as string/number by Jackson)
 * into a GBP currency string, e.g. "12.50" → "£12.50"
 */
export function formatPrice(
  value: string | number,
  currency: string = "GBP"
): string {
  const num = typeof value === "string" ? parseFloat(value) : value;
  return new Intl.NumberFormat("en-GB", {
    style: "currency",
    currency,
    minimumFractionDigits: 2,
  }).format(num);
}

/**
 * Format an ISO datetime string into a readable date, e.g. "Thu 15 Apr 2026"
 */
export function formatDate(iso: string): string {
  return new Intl.DateTimeFormat("en-GB", {
    weekday: "short",
    day: "numeric",
    month: "short",
    year: "numeric",
  }).format(new Date(iso));
}

/**
 * Format an ISO datetime string into a time, e.g. "14:30"
 */
export function formatTime(iso: string): string {
  return new Intl.DateTimeFormat("en-GB", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(new Date(iso));
}

/**
 * Format a date for grouping showtimes, e.g. "2026-04-15"
 */
export function formatDateKey(iso: string): string {
  return new Date(iso).toISOString().slice(0, 10);
}

export function bookingCutoffEpoch(cutoffMinutes = 10): number {
  return new Date().getTime() + cutoffMinutes * 60 * 1000;
}

/**
 * Returns MM:SS string from a future ISO date string, or "00:00" if expired.
 */
export function secondsUntil(iso: string): number {
  return Math.max(
    0,
    Math.floor((new Date(iso).getTime() - Date.now()) / 1000)
  );
}

export function formatCountdown(totalSeconds: number): string {
  const mins = Math.floor(totalSeconds / 60);
  const secs = totalSeconds % 60;
  return `${String(mins).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
}
