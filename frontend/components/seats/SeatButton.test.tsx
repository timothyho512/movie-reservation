import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { SeatButton } from "./SeatButton";

describe("SeatButton", () => {
  it("calls onClick when an available seat is selected", async () => {
    const user = userEvent.setup();
    const onClick = vi.fn();

    render(
      <SeatButton
        rowLabel="A"
        seatNumber={4}
        seatType="REGULAR"
        state="available"
        onClick={onClick}
      />
    );

    const seat = screen.getByRole("button", {
      name: "Seat A4 — REGULAR — available",
    });
    await user.click(seat);

    expect(onClick).toHaveBeenCalledOnce();
    expect(seat).toHaveAttribute("aria-pressed", "false");
  });

  it("marks a selected seat as pressed", () => {
    render(
      <SeatButton
        rowLabel="B"
        seatNumber={2}
        seatType="VIP"
        state="selected"
      />
    );

    expect(
      screen.getByRole("button", { name: "Seat B2 — VIP — selected" })
    ).toHaveAttribute("aria-pressed", "true");
  });

  it("prevents interaction with an unavailable seat", async () => {
    const user = userEvent.setup();
    const onClick = vi.fn();

    render(
      <SeatButton
        rowLabel="C"
        seatNumber={1}
        seatType="WHEELCHAIR"
        state="unavailable"
        onClick={onClick}
      />
    );

    const seat = screen.getByRole("button", {
      name: "Seat C1 — WHEELCHAIR — unavailable",
    });
    await user.click(seat);

    expect(seat).toBeDisabled();
    expect(onClick).not.toHaveBeenCalled();
  });
});
