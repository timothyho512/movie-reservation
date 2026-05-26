import Link from "next/link";
import { CheckCircle2 } from "lucide-react";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export function BookingConfirmation({
  bookingReference,
  reservationId,
  isGuest = false,
}: {
  bookingReference: string;
  reservationId?: number | null;
  isGuest?: boolean;
}) {
  return (
    <div className="flex flex-col items-center text-center py-16 px-4 space-y-6">
      <div className="rounded-full bg-green-100 p-5">
        <CheckCircle2 className="h-12 w-12 text-green-600" />
      </div>
      <div>
        <h1 className="text-2xl font-bold mb-1">Booking Confirmed!</h1>
        <p className="text-muted-foreground">
          Your seats have been reserved. Check your email for confirmation.
        </p>
      </div>
      <div className="bg-muted rounded-lg px-8 py-4">
        <p className="text-xs text-muted-foreground uppercase tracking-widest mb-1">
          Booking Reference
        </p>
        <p className="text-2xl font-mono font-bold tracking-wider">
          {bookingReference}
        </p>
      </div>
      {isGuest ? (
        <p className="text-sm text-muted-foreground">
          A confirmation has been sent to your email.
        </p>
      ) : (
        <div className="flex flex-col sm:flex-row gap-3 w-full max-w-xs">
          {reservationId && (
            <Link
              href={`/account/bookings/${reservationId}`}
              className={cn(buttonVariants({ variant: "outline" }), "flex-1")}
            >
              View Booking
            </Link>
          )}
          <Link
            href="/account/bookings"
            className={cn(buttonVariants(), "flex-1")}
          >
            My Bookings
          </Link>
        </div>
      )}
      <Link
        href="/movies"
        className={cn(buttonVariants({ variant: "ghost", size: "sm" }))}
      >
        Back to Movies
      </Link>
    </div>
  );
}
