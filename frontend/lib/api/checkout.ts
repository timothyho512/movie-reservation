import { clientFetch } from "@/lib/api-client";
import {
  CheckoutLockRequest,
  CheckoutLockResponse,
  CheckoutSessionCreateRequest,
  CheckoutSessionCreateResponse,
  CheckoutSessionStatusResponse,
} from "@/types/api";

export function lockSeats(
  data: CheckoutLockRequest,
  idempotencyKey?: string
): Promise<CheckoutLockResponse> {
  return clientFetch<CheckoutLockResponse>("/checkout/lock", {
    method: "POST",
    headers: idempotencyKey ? { "Idempotency-Key": idempotencyKey } : undefined,
    body: JSON.stringify(data),
  });
}

export function createCheckoutSession(
  data: CheckoutSessionCreateRequest,
  idempotencyKey?: string
): Promise<CheckoutSessionCreateResponse> {
  return clientFetch<CheckoutSessionCreateResponse>("/checkout/session", {
    method: "POST",
    headers: idempotencyKey ? { "Idempotency-Key": idempotencyKey } : undefined,
    body: JSON.stringify(data),
  });
}

export function getCheckoutStatus(
  checkoutReference: string
): Promise<CheckoutSessionStatusResponse> {
  return clientFetch<CheckoutSessionStatusResponse>(
    `/checkout/session/${checkoutReference}`
  );
}
