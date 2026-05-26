"use client";

import { useCheckoutPolling } from "@/hooks/useCheckoutPolling";
import { useAuth } from "@/hooks/useAuth";
import { BookingConfirmation } from "./BookingConfirmation";
import { Loader2, XCircle, Clock } from "lucide-react";
import Link from "next/link";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export function PaymentPolling({
  checkoutReference,
}: {
  checkoutReference: string;
}) {
  const { data, isLoading } = useCheckoutPolling(checkoutReference);
  const { isLoggedIn } = useAuth();

  if (isLoading || !data) {
    return (
      <div className="flex flex-col items-center py-20 gap-4 text-muted-foreground">
        <Loader2 className="h-10 w-10 animate-spin" />
        <p>Processing your payment…</p>
      </div>
    );
  }

  if (data.status === "FINALIZED") {
    return (
      <BookingConfirmation
        bookingReference={data.bookingReference!}
        reservationId={data.reservationId}
        isGuest={!isLoggedIn}
      />
    );
  }

  if (data.status === "EXPIRED") {
    return (
      <div className="flex flex-col items-center text-center py-16 px-4 space-y-4">
        <div className="rounded-full bg-muted p-5">
          <Clock className="h-12 w-12 text-muted-foreground" />
        </div>
        <h1 className="text-2xl font-bold">Session Expired</h1>
        <p className="text-muted-foreground max-w-sm">
          Your payment session expired. Your seat hold has been released.
        </p>
        <Link href="/movies" className={cn(buttonVariants())}>
          Browse Movies
        </Link>
      </div>
    );
  }

  if (data.status === "FAILED" || data.status === "CANCELLED") {
    return (
      <div className="flex flex-col items-center text-center py-16 px-4 space-y-4">
        <div className="rounded-full bg-destructive/10 p-5">
          <XCircle className="h-12 w-12 text-destructive" />
        </div>
        <h1 className="text-2xl font-bold">
          Payment {data.status === "CANCELLED" ? "Cancelled" : "Failed"}
        </h1>
        <p className="text-muted-foreground max-w-sm">
          {data.message ?? "Something went wrong. Please try booking again."}
        </p>
        <Link href="/movies" className={cn(buttonVariants())}>
          Browse Movies
        </Link>
      </div>
    );
  }

  // Still PENDING_PAYMENT or PAID — keep polling
  return (
    <div className="flex flex-col items-center py-20 gap-4 text-muted-foreground">
      <Loader2 className="h-10 w-10 animate-spin" />
      <p>Confirming your booking…</p>
      <p className="text-xs">This may take a few seconds.</p>
    </div>
  );
}
