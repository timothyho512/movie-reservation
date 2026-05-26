"use client";

import { useQuery } from "@tanstack/react-query";
import { getReservations } from "@/lib/api/reservations";
import { queryKeys } from "@/lib/query-keys";
import { ReservationCard } from "@/components/reservations/ReservationCard";
import { ReservationListSkeleton } from "@/components/shared/LoadingSkeleton";
import { Ticket } from "lucide-react";

export default function BookingsPage() {
  const { data: reservations, isLoading } = useQuery({
    queryKey: queryKeys.reservations.all,
    queryFn: getReservations,
  });

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">My Bookings</h1>

      {isLoading ? (
        <ReservationListSkeleton />
      ) : !reservations || reservations.length === 0 ? (
        <div className="text-center py-16 text-muted-foreground">
          <Ticket className="h-10 w-10 mx-auto mb-3 opacity-30" />
          <p>No bookings yet.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {reservations.map((r) => (
            <ReservationCard key={r.reservationId} reservation={r} />
          ))}
        </div>
      )}
    </div>
  );
}
