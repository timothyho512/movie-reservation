"use client";

import { use, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { useQuery } from "@tanstack/react-query";
import { getSeatMap } from "@/lib/api/showtimes";
import { lockSeats, createCheckoutSession } from "@/lib/api/checkout";
import { queryKeys } from "@/lib/query-keys";
import { useCheckoutStore } from "@/stores/checkout-store";
import { useAuth } from "@/hooks/useAuth";
import { useLockCountdown } from "@/hooks/useLockCountdown";
import { SeatMap } from "@/components/seats/SeatMap";
import { SeatSummaryPanel } from "@/components/seats/SeatSummaryPanel";
import { LockCountdown } from "@/components/seats/LockCountdown";
import { SeatMapSkeleton } from "@/components/shared/LoadingSkeleton";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { formatDate, formatTime } from "@/lib/format";
import { ApiError } from "@/lib/api-client";
import { z } from "zod";

const emailSchema = z.string().email();

export default function SeatsPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const showtimeId = Number(id);
  const router = useRouter();
  const { isLoggedIn, clearUser } = useAuth();

  const {
    selectedSeatIds,
    toggleSeat,
    sessionId,
    expiresAt,
    setShowtimeId,
    setLock,
    setCheckoutReference,
    reset,
  } = useCheckoutStore();

  const [guestEmail, setGuestEmail] = useState("");
  const [guestEmailError, setGuestEmailError] = useState("");
  const [isLocking, setIsLocking] = useState(false);
  const [isPaying, setIsPaying] = useState(false);
  const [lockAttempt, setLockAttempt] = useState<{
    signature: string;
    idempotencyKey: string;
  } | null>(null);
  const [checkoutAttempt, setCheckoutAttempt] = useState<{
    signature: string;
    idempotencyKey: string;
  } | null>(null);
  const isLocked = !!expiresAt;

  const { isExpired } = useLockCountdown(expiresAt);

  // Reset store when navigating to a new showtime
  useEffect(() => {
    setShowtimeId(showtimeId);
    // Only reset if switching showtimes
    return () => {
      // cleanup intentionally left for navigation
    };
  }, [showtimeId, setShowtimeId]);

  // If lock expires, reset
  useEffect(() => {
    if (isExpired && isLocked) {
      reset();
      setLockAttempt(null);
      setCheckoutAttempt(null);
      toast.error("Your seat hold expired. Please select seats again.");
    }
  }, [isExpired, isLocked, reset]);

  const { data: seatMap, isLoading } = useQuery({
    queryKey: queryKeys.showtimes.seatMap(showtimeId),
    queryFn: () => getSeatMap(showtimeId),
    staleTime: 15_000, // re-fetch availability every 15s
    refetchInterval: 15_000,
  });

  const selectedSeats =
    seatMap?.seats.filter((s) => selectedSeatIds.has(s.id)) ?? [];

  async function handleLock() {
    if (!isLoggedIn) {
      const result = emailSchema.safeParse(guestEmail);
      if (!result.success) {
        setGuestEmailError("Please enter a valid email address");
        return;
      }
      setGuestEmailError("");
    }

    setIsLocking(true);
    try {
      const seatIds = Array.from(selectedSeatIds).sort((a, b) => a - b);
      const signature = JSON.stringify({
        showtimeId,
        seatIds,
        mode: isLoggedIn ? "authenticated" : "guest",
        guestEmail: isLoggedIn ? null : guestEmail.trim().toLowerCase(),
      });

      const attempt =
        lockAttempt?.signature === signature
          ? lockAttempt
          : {
              signature,
              idempotencyKey: createIdempotencyKey(),
            };

      if (attempt !== lockAttempt) {
        setLockAttempt(attempt);
      }

      const response = await lockSeats({
        showtimeId,
        seatIds,
        ...(!isLoggedIn && { guestEmail }),
      }, attempt.idempotencyKey);
      setLock(response.sessionId, response.expiresAt);
      setCheckoutAttempt(null);
      toast.success("Seats held for 15 minutes!");
    } catch (e) {
      if (isLoggedIn && e instanceof ApiError && (e.status === 401 || e.status === 400)) {
        clearUser();
        toast.error("Your session expired. Enter your email below to continue as guest, or log in.");
      } else {
        const msg = e instanceof ApiError ? e.message : "Failed to hold seats. Please try again.";
        toast.error(msg);
      }
    } finally {
      setIsLocking(false);
    }
  }

  async function handlePay() {
    setIsPaying(true);
    try {
      const seatIds = Array.from(selectedSeatIds).sort((a, b) => a - b);
      const signature = JSON.stringify({
        showtimeId,
        seatIds,
        mode: isLoggedIn ? "authenticated" : "guest",
        guestEmail: isLoggedIn ? null : guestEmail.trim().toLowerCase(),
        sessionId: isLoggedIn ? null : sessionId,
      });

      const attempt =
        checkoutAttempt?.signature === signature
          ? checkoutAttempt
          : {
              signature,
              idempotencyKey: createIdempotencyKey(),
            };

      if (attempt !== checkoutAttempt) {
        setCheckoutAttempt(attempt);
      }

      const response = await createCheckoutSession({
        showtimeId,
        seatIds,
        ...(!isLoggedIn && { guestEmail, sessionId: sessionId! }),
      }, attempt.idempotencyKey);
      setCheckoutReference(response.checkoutReference);
      // Full browser navigation to Stripe
      window.location.href = response.checkoutUrl;
    } catch (e) {
      if (isLoggedIn && e instanceof ApiError && (e.status === 401 || e.status === 400)) {
        clearUser();
        toast.error("Your session expired. Enter your email below to continue as guest, or log in.");
      } else {
        const msg = e instanceof ApiError ? e.message : "Failed to start checkout. Please try again.";
        toast.error(msg);
      }
      setIsPaying(false);
    }
  }

  function createIdempotencyKey() {
    if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
      return crypto.randomUUID();
    }

    return `checkout-${Date.now()}-${Math.random().toString(36).slice(2)}`;
  }

  async function handleContinue() {
    if (isLocked) {
      await handlePay();
    } else {
      await handleLock();
    }
  }

  if (isLoading || !seatMap) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-10">
        <SeatMapSkeleton />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold">{seatMap.movie.title}</h1>
        <div className="flex flex-wrap items-center gap-3 mt-1 text-sm text-muted-foreground">
          <span>
            {formatDate(seatMap.startTime)} · {formatTime(seatMap.startTime)} →{" "}
            {formatTime(seatMap.endTime)}
          </span>
          <Badge variant="secondary">{seatMap.screen.screenType}</Badge>
          <Badge variant="secondary">{seatMap.screen.name}</Badge>
        </div>
        {isLocked && expiresAt && (
          <div className="mt-2">
            <LockCountdown expiresAt={expiresAt} />
          </div>
        )}
      </div>

      <div className="flex flex-col lg:flex-row gap-8">
        {/* Seat map */}
        <div className="flex-1 min-w-0">
          <SeatMap
            seats={seatMap.seats}
            selectedIds={selectedSeatIds}
            onToggle={isLocked ? () => {} : toggleSeat}
          />
          {isLocked && (
            <p className="text-center text-xs text-muted-foreground mt-4">
              Seat selection is locked while your hold is active.
            </p>
          )}
        </div>

        {/* Summary panel */}
        <div className="w-full lg:w-72 shrink-0 space-y-4">
          {!isLoggedIn && !isLocked && (
            <div className="border rounded-lg p-4 space-y-3 bg-card">
              <h3 className="font-semibold text-sm">Guest Checkout</h3>
              <div className="space-y-1.5">
                <Label htmlFor="guestEmail" className="text-xs">
                  Your email address
                </Label>
                <Input
                  id="guestEmail"
                  type="email"
                  placeholder="you@example.com"
                  value={guestEmail}
                  onChange={(e) => setGuestEmail(e.target.value)}
                />
                {guestEmailError && (
                  <p className="text-xs text-destructive">{guestEmailError}</p>
                )}
              </div>
              <p className="text-xs text-muted-foreground">
                Your booking confirmation will be sent to this address.{" "}
                <a href="/login" className="underline hover:text-foreground">
                  Log in instead
                </a>
              </p>
            </div>
          )}

          <SeatSummaryPanel
            selectedSeats={selectedSeats}
            onContinue={handleContinue}
            isLoading={isLocking || isPaying}
            isLocked={isLocked}
          />

          <div className="text-center">
            <button
              type="button"
              onClick={() => router.back()}
              className="text-xs text-muted-foreground hover:underline"
            >
              ← Back
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
