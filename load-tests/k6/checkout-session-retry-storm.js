import http from "k6/http";
import { check, fail } from "k6";
import { Counter } from "k6/metrics";

const checkoutSessionReplays = new Counter("checkout_session_replays");
const checkoutSessionUnexpectedResponses = new Counter("checkout_session_unexpected_responses");

const VUS = Number(__ENV.VUS || 100);
const ITERATIONS = Number(__ENV.ITERATIONS || 100);
const BASE_URL = (__ENV.BASE_URL || "http://localhost:8080").replace(/\/$/, "");

export const options = {
  summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)", "p(99)"],
  scenarios: {
    checkout_session_retry_storm: {
      executor: "shared-iterations",
      vus: VUS,
      iterations: ITERATIONS,
      maxDuration: __ENV.MAX_DURATION || "1m",
    },
  },
  thresholds: {
    checks: ["rate==1"],
    checkout_session_replays: [`count==${ITERATIONS}`],
    checkout_session_unexpected_responses: ["count==0"],
    "http_req_duration{scenario:checkout_session_retry_storm}": [
      `p(95)<${Number(__ENV.P95_THRESHOLD_MS || 1000)}`,
      `p(99)<${Number(__ENV.P99_THRESHOLD_MS || 2000)}`,
    ],
  },
};

export function setup() {
  const runId = `${Date.now()}`;
  const guestEmail = __ENV.GUEST_EMAIL || `k6-checkout-retry-${runId}@example.com`;
  const lockIdempotencyKey = __ENV.LOCK_IDEMPOTENCY_KEY || `k6-lock-${runId}`;
  const checkoutIdempotencyKey = __ENV.CHECKOUT_IDEMPOTENCY_KEY || `k6-checkout-${runId}`;

  const selected = selectAvailableSeat();
  const lockResponse = lockSeat(selected.showtimeId, selected.seatId, guestEmail, lockIdempotencyKey);

  const checkoutRequest = {
    showtimeId: selected.showtimeId,
    seatIds: [selected.seatId],
    guestEmail,
    sessionId: lockResponse.sessionId,
  };

  const initialCheckoutSession = createCheckoutSession(checkoutRequest, checkoutIdempotencyKey);

  console.log(
    `Selected showtimeId=${selected.showtimeId}, seatId=${selected.seatId}, checkoutReference=${initialCheckoutSession.checkoutReference}`
  );

  return {
    checkoutRequest,
    checkoutIdempotencyKey,
    expectedCheckoutReference: initialCheckoutSession.checkoutReference,
    expectedStripeCheckoutSessionId: initialCheckoutSession.stripeCheckoutSessionId,
  };
}

export default function (data) {
  const response = http.post(
    `${BASE_URL}/checkout/session`,
    JSON.stringify(data.checkoutRequest),
    {
      headers: {
        "Content-Type": "application/json",
        "Idempotency-Key": data.checkoutIdempotencyKey,
      },
      tags: {
        scenario: "checkout_session_retry_storm",
      },
    }
  );

  const body = response.status === 200 ? parseJson(response) : {};
  const isReplay = response.status === 200
    && body.checkoutReference === data.expectedCheckoutReference
    && body.stripeCheckoutSessionId === data.expectedStripeCheckoutSessionId;

  if (isReplay) {
    checkoutSessionReplays.add(1);
  } else {
    checkoutSessionUnexpectedResponses.add(1);
  }

  const ok = check(response, {
    "checkout retry returns 200": (res) => res.status === 200,
    "checkout retry returns original checkout reference": () => body.checkoutReference === data.expectedCheckoutReference,
    "checkout retry returns original Stripe session": () => body.stripeCheckoutSessionId === data.expectedStripeCheckoutSessionId,
    "checkout retry remains pending payment": () => body.status === "PENDING_PAYMENT",
  });

  if (!ok) {
    fail(`Unexpected checkout retry response: ${response.status} ${response.body}`);
  }
}

export function handleSummary(data) {
  const replayCount = data.metrics.checkout_session_replays?.values?.count || 0;
  const unexpectedCount = data.metrics.checkout_session_unexpected_responses?.values?.count || 0;
  const duration = data.metrics.http_req_duration?.values || {};
  const passed = replayCount === ITERATIONS && unexpectedCount === 0;

  const summary = [
    "Checkout session retry storm test",
    "=================================",
    `Base URL: ${BASE_URL}`,
    `Expected: ${ITERATIONS} idempotent replay responses and 0 unexpected responses`,
    `Actual: ${replayCount} replay responses, ${unexpectedCount} unexpected responses`,
    `Latency: avg=${formatMs(duration.avg)}, p95=${formatMs(duration["p(95)"])}, p99=${formatMs(duration["p(99)"])}`,
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

function selectAvailableSeat() {
  const showtimes = getJson(`${BASE_URL}/api/showtimes`);
  const candidates = showtimes
    .filter((showtime) => showtime.status === "UPCOMING")
    .filter((showtime) => Number(showtime.availableSeats) > 0)
    .sort((left, right) => new Date(left.startTime) - new Date(right.startTime));

  for (const showtime of candidates) {
    const seatMap = getJson(`${BASE_URL}/api/showtimes/${showtime.id}/seat-map`);
    const seat = seatMap.seats.find((candidate) => candidate.available);

    if (seat) {
      return {
        showtimeId: showtime.id,
        seatId: seat.id,
      };
    }
  }

  fail("No available seeded showtime/seat found. Start the backend with the dev profile and a clean enough database.");
}

function lockSeat(showtimeId, seatId, guestEmail, lockIdempotencyKey) {
  const response = http.post(
    `${BASE_URL}/checkout/lock`,
    JSON.stringify({
      showtimeId,
      seatIds: [seatId],
      guestEmail,
    }),
    {
      headers: {
        "Content-Type": "application/json",
        "Idempotency-Key": lockIdempotencyKey,
      },
    }
  );

  if (response.status !== 200) {
    fail(`Could not create initial seat lock: ${response.status} ${response.body}`);
  }

  return parseJson(response);
}

function createCheckoutSession(checkoutRequest, checkoutIdempotencyKey) {
  const response = http.post(
    `${BASE_URL}/checkout/session`,
    JSON.stringify(checkoutRequest),
    {
      headers: {
        "Content-Type": "application/json",
        "Idempotency-Key": checkoutIdempotencyKey,
      },
    }
  );

  if (response.status !== 200) {
    fail(`Could not create initial checkout session: ${response.status} ${response.body}`);
  }

  return parseJson(response);
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

function formatMs(value) {
  if (typeof value !== "number") {
    return "n/a";
  }

  return `${value.toFixed(2)}ms`;
}
