import { Badge } from "@/components/ui/badge";
import { ReservationStatus, PaymentStatus } from "@/types/api";

const reservationVariant: Record<ReservationStatus, string> = {
  CONFIRMED: "bg-green-100 text-green-800",
  PENDING: "bg-yellow-100 text-yellow-800",
  CANCELLED: "bg-red-100 text-red-800",
  COMPLETED: "bg-blue-100 text-blue-800",
};

const paymentVariant: Record<PaymentStatus, string> = {
  PAID: "bg-green-100 text-green-800",
  PENDING: "bg-yellow-100 text-yellow-800",
  FAILED: "bg-red-100 text-red-800",
  REFUNDED: "bg-purple-100 text-purple-800",
};

export function ReservationStatusBadge({ status }: { status: ReservationStatus }) {
  return (
    <Badge variant="secondary" className={reservationVariant[status] ?? ""}>
      {status}
    </Badge>
  );
}

export function PaymentStatusBadge({ status }: { status: PaymentStatus }) {
  return (
    <Badge variant="secondary" className={paymentVariant[status] ?? ""}>
      {status}
    </Badge>
  );
}
