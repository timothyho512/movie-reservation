import { ApiErrorResponse } from "@/types/api";

export class ApiError extends Error {
  status: number;
  body: ApiErrorResponse | null;

  constructor(status: number, body: ApiErrorResponse | null, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

/**
 * Typed fetch wrapper for direct backend calls (Server Components, Route Handlers).
 * For client-side authenticated calls, use /api/proxy/* instead.
 */
export async function apiFetch<T>(
  path: string,
  options: RequestInit & { token?: string } = {}
): Promise<T> {
  const { token, ...fetchOptions } = options;

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(fetchOptions.headers as Record<string, string>),
  };

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const res = await fetch(`${API_BASE}${path}`, {
    ...fetchOptions,
    headers,
  });

  if (!res.ok) {
    let body: ApiErrorResponse | null = null;
    try {
      body = await res.json();
    } catch {
      // ignore parse error
    }
    throw new ApiError(
      res.status,
      body,
      body?.message ?? `Request failed with status ${res.status}`
    );
  }

  if (res.status === 204) {
    return undefined as T;
  }

  return res.json() as Promise<T>;
}

/**
 * Client-side fetch that goes through the Next.js proxy route handler.
 * The proxy injects the Authorization header server-side from the httpOnly cookie.
 */
export async function clientFetch<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string>),
  };

  const res = await fetch(`/api/proxy${path}`, {
    ...options,
    headers,
  });

  if (!res.ok) {
    let body: ApiErrorResponse | null = null;
    try {
      body = await res.json();
    } catch {
      // ignore parse error
    }
    throw new ApiError(
      res.status,
      body,
      body?.message ?? `Request failed with status ${res.status}`
    );
  }

  if (res.status === 204) {
    return undefined as T;
  }

  return res.json() as Promise<T>;
}
