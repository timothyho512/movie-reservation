import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import {
  PaymentStatusBadge,
  ReservationStatusBadge,
} from "./ReservationStatusBadge";

describe("reservation status badges", () => {
  it("renders a confirmed reservation with its success styling", () => {
    render(<ReservationStatusBadge status="CONFIRMED" />);

    expect(screen.getByText("CONFIRMED")).toHaveClass(
      "bg-green-100",
      "text-green-800"
    );
  });

  it("renders a refunded payment with its refund styling", () => {
    render(<PaymentStatusBadge status="REFUNDED" />);

    expect(screen.getByText("REFUNDED")).toHaveClass(
      "bg-purple-100",
      "text-purple-800"
    );
  });
});
