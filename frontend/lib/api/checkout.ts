import { clientFetch } from "@/lib/api-client";
import {
  CheckoutLockRequest,
  CheckoutLockResponse,
  CheckoutSessionCreateRequest,
  CheckoutSessionCreateResponse,
  CheckoutSessionStatusResponse,
} from "@/types/api";

export function lockSeats(
  data: CheckoutLockRequest
): Promise<CheckoutLockResponse> {
  return clientFetch<CheckoutLockResponse>("/checkout/lock", {
    method: "POST",
    body: JSON.stringify(data),
  });
}

export function createCheckoutSession(
  data: CheckoutSessionCreateRequest
): Promise<CheckoutSessionCreateResponse> {
  return clientFetch<CheckoutSessionCreateResponse>("/checkout/session", {
    method: "POST",
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
