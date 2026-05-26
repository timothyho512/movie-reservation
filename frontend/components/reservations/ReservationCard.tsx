import Link from "next/link";
import { ReservationResponse } from "@/types/api";
import { formatDate, formatPrice, formatTime } from "@/lib/format";
import { ReservationStatusBadge, PaymentStatusBadge } from "./ReservationStatusBadge";
import { Card, CardContent } from "@/components/ui/card";
import { Film, Calendar, Ticket } from "lucide-react";

export function ReservationCard({
  reservation,
}: {
  reservation: ReservationResponse;
}) {
  return (
    <Link href={`/account/bookings/${reservation.reservationId}`}>
      <Card className="hover:shadow-md transition-shadow cursor-pointer">
        <CardContent className="p-4">
          <div className="flex items-start gap-4">
            <div className="p-2 rounded-md bg-primary/10 shrink-0">
              <Film className="h-5 w-5 text-primary" />
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-start justify-between gap-2">
                <h3 className="font-semibold truncate">{reservation.movie.title}</h3>
                <ReservationStatusBadge status={reservation.reservationStatus} />
              </div>
              <div className="flex flex-wrap gap-3 mt-1.5 text-sm text-muted-foreground">
                <span className="flex items-center gap-1">
                  <Calendar className="h-3.5 w-3.5" />
                  {formatDate(reservation.showtime.startTime)} ·{" "}
                  {formatTime(reservation.showtime.startTime)}
                </span>
                <span className="flex items-center gap-1">
                  <Ticket className="h-3.5 w-3.5" />
                  {reservation.seats.length} seat
                  {reservation.seats.length !== 1 && "s"}
                </span>
              </div>
              <div className="flex items-center justify-between mt-2">
                <p className="text-xs text-muted-foreground font-mono">
                  {reservation.reservationReference}
                </p>
                <div className="flex items-center gap-2">
                  <PaymentStatusBadge status={reservation.paymentStatus} />
                  <span className="text-sm font-semibold">
                    {formatPrice(reservation.totalAmount)}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}
