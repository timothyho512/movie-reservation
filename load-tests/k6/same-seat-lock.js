import http from "k6/http";
import { check, fail } from "k6";
import exec from "k6/execution";
import { Counter } from "k6/metrics";

const lockSuccesses = new Counter("same_seat_lock_successes");
const lockConflicts = new Counter("same_seat_lock_conflicts");
const lockUnexpectedResponses = new Counter("same_seat_lock_unexpected_responses");

export const options = {
  scenarios: {
    same_seat_lock_contention: {
      executor: "shared-iterations",
      vus: Number(__ENV.VUS || 100),
      iterations: Number(__ENV.ITERATIONS || 100),
      maxDuration: __ENV.MAX_DURATION || "30s",
    },
  },
  thresholds: {
    checks: ["rate==1"],
    same_seat_lock_successes: ["count==1"],
    same_seat_lock_conflicts: [`count==${Number(__ENV.ITERATIONS || 100) - 1}`],
    same_seat_lock_unexpected_responses: ["count==0"],
  },
};

const BASE_URL = (__ENV.BASE_URL || "http://localhost:8080").replace(/\/$/, "");
const GUEST_EMAIL_PREFIX = __ENV.GUEST_EMAIL_PREFIX || "k6-seat-lock";

export function setup() {
  const showtimes = getJson(`${BASE_URL}/api/showtimes`);
  const candidates = showtimes
    .filter((showtime) => showtime.status === "UPCOMING")
    .filter((showtime) => Number(showtime.availableSeats) > 0)
    .sort((left, right) => new Date(left.startTime) - new Date(right.startTime));

  for (const showtime of candidates) {
    const seatMap = getJson(`${BASE_URL}/api/showtimes/${showtime.id}/seat-map`);
    const seat = seatMap.seats.find((candidate) => candidate.available);

    if (seat) {
      console.log(
        `Selected showtimeId=${showtime.id}, seatId=${seat.id}, movie="${showtime.movie?.title || "unknown movie"}", seat=${seat.rowLabel}${seat.seatNumber}`
      );

      return {
        showtimeId: showtime.id,
        seatId: seat.id,
        movieTitle: showtime.movie?.title || "unknown movie",
        seatLabel: `${seat.rowLabel}${seat.seatNumber}`,
      };
    }
  }

  fail("No available seeded showtime/seat found. Start the backend with the dev profile and a clean enough database.");
}

export default function (data) {
  const attempt = exec.scenario.iterationInTest;
  const guestEmail = `${GUEST_EMAIL_PREFIX}-${attempt}@example.com`;
  const idempotencyKey = `same-seat-lock-${Date.now()}-${attempt}`;

  const response = http.post(
    `${BASE_URL}/checkout/lock`,
    JSON.stringify({
      showtimeId: data.showtimeId,
      seatIds: [data.seatId],
      guestEmail,
    }),
    {
      headers: {
        "Content-Type": "application/json",
        "Idempotency-Key": idempotencyKey,
      },
      tags: {
        scenario: "same_seat_lock_contention",
      },
    }
  );

  if (response.status === 200) {
    lockSuccesses.add(1);
  } else if (response.status === 409) {
    lockConflicts.add(1);
  } else {
    lockUnexpectedResponses.add(1);
  }

  const ok = check(response, {
    "lock request returns only success or conflict": (res) => res.status === 200 || res.status === 409,
    "successful lock response contains selected seat": (res) => {
      if (res.status !== 200) {
        return true;
      }

      const body = parseJson(res);
      return Array.isArray(body.lockedSeatIds) && body.lockedSeatIds.includes(data.seatId);
    },
    "conflict response is seat contention": (res) => {
      if (res.status !== 409) {
        return true;
      }

      const body = parseJson(res);
      return typeof body.message === "string" && body.message.toLowerCase().includes("seat");
    },
  });

  if (!ok) {
    fail(`Unexpected response for same-seat lock attempt ${attempt}: ${response.status} ${response.body}`);
  }
}

export function handleSummary(data) {
  const successCount = data.metrics.same_seat_lock_successes?.values?.count || 0;
  const conflictCount = data.metrics.same_seat_lock_conflicts?.values?.count || 0;
  const unexpectedCount = data.metrics.same_seat_lock_unexpected_responses?.values?.count || 0;
  const expectedIterations = Number(__ENV.ITERATIONS || 100);

  const passed = successCount === 1 && conflictCount === expectedIterations - 1 && unexpectedCount === 0;

  const summary = [
    "Same-seat lock contention test",
    "================================",
    `Base URL: ${BASE_URL}`,
    `Expected: 1 success and ${expectedIterations - 1} conflicts`,
    `Actual: ${successCount} success, ${conflictCount} conflicts, ${unexpectedCount} unexpected statuses`,
    `Result: ${passed ? "PASS" : "FAIL"}`,
    "",
  ].join("\n");

  if (!passed) {
    console.error(summary);
  }

  return {
    stdout: summary,
  };
}

function getJson(url) {
  const response = http.get(url);

  if (response.status !== 200) {
    fail(`GET ${url} returned ${response.status}: ${response.body}`);
  }

  return parseJson(response);
}

function parseJson(response) {
  try {
    return response.json();
  } catch (error) {
    fail(`Response was not valid JSON: ${response.body}`);
  }
}
