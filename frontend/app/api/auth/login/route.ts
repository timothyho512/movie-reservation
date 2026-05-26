import { cookies } from "next/headers";
import { AuthResponse } from "@/types/api";

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export async function POST(request: Request) {
  const body = await request.json();

  const res = await fetch(`${API_BASE}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: "Login failed" }));
    return Response.json(err, { status: res.status });
  }

  const data: AuthResponse = await res.json();

  const cookieStore = await cookies();
  cookieStore.set("jwt", data.token, {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    maxAge: 60 * 60, // 1 hour
    path: "/",
  });

  return Response.json({ user: data.user });
}
