import http from "k6/http";
import { check, fail } from "k6";
import exec from "k6/execution";
import { Counter } from "k6/metrics";

const seatMapSuccesses = new Counter("seat_map_successes");
const seatMapUnexpectedResponses = new Counter("seat_map_unexpected_responses");

const VUS = Number(__ENV.VUS || 1000);
const ITERATIONS = Number(__ENV.ITERATIONS || 1000);
const BASE_URL = (__ENV.BASE_URL || "http://localhost:8080").replace(/\/$/, "");

export const options = {
  summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)", "p(99)"],
  scenarios: {
    seat_map_browsing: {
      executor: "shared-iterations",
      vus: VUS,
      iterations: ITERATIONS,
      maxDuration: __ENV.MAX_DURATION || "1m",
    },
  },
  thresholds: {
    checks: ["rate==1"],
    seat_map_successes: [`count==${ITERATIONS}`],
    seat_map_unexpected_responses: ["count==0"],
    "http_req_duration{scenario:seat_map_browsing}": [
      `p(95)<${Number(__ENV.P95_THRESHOLD_MS || 500)}`,
      `p(99)<${Number(__ENV.P99_THRESHOLD_MS || 1000)}`,
    ],
  },
};

export function setup() {
  const showtimes = getJson(`${BASE_URL}/api/showtimes`);
  const showtimeIds = showtimes
    .filter((showtime) => showtime.status === "UPCOMING")
    .filter((showtime) => Number(showtime.availableSeats) > 0)
    .sort((left, right) => new Date(left.startTime) - new Date(right.startTime))
    .map((showtime) => showtime.id);

  if (showtimeIds.length === 0) {
    fail("No upcoming seeded showtimes found. Start the backend with the dev profile and a clean enough database.");
  }

  console.log(`Selected ${showtimeIds.length} upcoming showtimes for seat-map browsing load`);

  return {
    showtimeIds,
  };
}

export default function (data) {
  const showtimeId = data.showtimeIds[exec.scenario.iterationInTest % data.showtimeIds.length];
  const response = http.get(`${BASE_URL}/api/showtimes/${showtimeId}/seat-map`, {
    tags: {
      scenario: "seat_map_browsing",
    },
  });

  if (response.status === 200) {
    seatMapSuccesses.add(1);
  } else {
    seatMapUnexpectedResponses.add(1);
  }

  const ok = check(response, {
    "seat map returns 200": (res) => res.status === 200,
    "seat map contains showtime id": (res) => {
      const body = parseJson(res);
      return body.showtimeId === showtimeId;
    },
    "seat map contains seats": (res) => {
      const body = parseJson(res);
      return Array.isArray(body.seats) && body.seats.length > 0;
    },
  });

  if (!ok) {
    fail(`Unexpected seat-map response for showtime ${showtimeId}: ${response.status} ${response.body}`);
  }
}

export function handleSummary(data) {
  const successCount = data.metrics.seat_map_successes?.values?.count || 0;
  const unexpectedCount = data.metrics.seat_map_unexpected_responses?.values?.count || 0;
  const duration = data.metrics.http_req_duration?.values || {};
  const passed = successCount === ITERATIONS && unexpectedCount === 0;

  const summary = [
    "Seat-map browsing load test",
    "===========================",
    `Base URL: ${BASE_URL}`,
    `Expected: ${ITERATIONS} successful seat-map responses and 0 unexpected statuses`,
    `Actual: ${successCount} successes, ${unexpectedCount} unexpected statuses`,
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
