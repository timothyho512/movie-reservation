"use client";

import { use } from "react";
import { useQuery } from "@tanstack/react-query";
import { getReservation } from "@/lib/api/reservations";
import { queryKeys } from "@/lib/query-keys";
import { ReservationStatusBadge, PaymentStatusBadge } from "@/components/reservations/ReservationStatusBadge";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { formatDate, formatPrice, formatTime } from "@/lib/format";
import { Badge } from "@/components/ui/badge";
import Link from "next/link";
import { ChevronLeft, Film } from "lucide-react";

export default function BookingDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const reservationId = Number(id);

  const { data: reservation, isLoading } = useQuery({
    queryKey: queryKeys.reservations.detail(reservationId),
    queryFn: () => getReservation(reservationId),
  });

  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  if (!reservation) {
    return <p className="text-muted-foreground">Booking not found.</p>;
  }

  return (
    <div className="space-y-6">
      <div>
        <Link
          href="/account/bookings"
          className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground mb-2"
        >
          <ChevronLeft className="h-4 w-4" />
          My Bookings
        </Link>
        <h1 className="text-2xl font-bold flex items-center gap-2">
          <Film className="h-6 w-6" />
          {reservation.movie.title}
        </h1>
      </div>

      <div className="flex flex-wrap gap-2">
        <ReservationStatusBadge status={reservation.reservationStatus} />
        <PaymentStatusBadge status={reservation.paymentStatus} />
        <Badge variant="secondary">{reservation.screen.screenType}</Badge>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Booking Details</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <p className="text-muted-foreground text-xs uppercase tracking-wide mb-1">
                Reference
              </p>
              <p className="font-mono font-semibold">{reservation.reservationReference}</p>
            </div>
            <div>
              <p className="text-muted-foreground text-xs uppercase tracking-wide mb-1">
                Date & Time
              </p>
              <p>
                {formatDate(reservation.showtime.startTime)} ·{" "}
                {formatTime(reservation.showtime.startTime)} →{" "}
                {formatTime(reservation.showtime.endTime)}
              </p>
            </div>
          </div>
          <div>
            <p className="text-muted-foreground text-xs uppercase tracking-wide mb-1">
              Screen
            </p>
            <p>{reservation.screen.name}</p>
          </div>

          <Separator />

          <div>
            <p className="text-muted-foreground text-xs uppercase tracking-wide mb-2">
              Seats ({reservation.seats.length})
            </p>
            <div className="flex flex-wrap gap-2">
              {reservation.seats.map((seat) => (
                <Badge key={seat.id} variant="outline">
                  {seat.rowLabel}
                  {seat.seatNumber}{" "}
                  <span className="ml-1 text-muted-foreground capitalize">
                    {seat.seatType.toLowerCase()}
                  </span>
                </Badge>
              ))}
            </div>
          </div>

          <Separator />

          <div className="flex justify-between font-semibold">
            <span>Total Paid</span>
            <span>
              {formatPrice(reservation.totalAmount)} {reservation.currency}
            </span>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
