import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { decodeJwt } from "jose";

export function proxy(request: NextRequest) {
  const token = request.cookies.get("jwt")?.value;
  const redirectTo = encodeURIComponent(request.nextUrl.pathname);

  if (!token) {
    return NextResponse.redirect(
      new URL(`/login?redirectTo=${redirectTo}`, request.url)
    );
  }

  try {
    const payload = decodeJwt(token);
    const exp = payload.exp as number | undefined;
    if (exp && exp * 1000 < Date.now()) {
      throw new Error("expired");
    }
    return NextResponse.next();
  } catch {
    const response = NextResponse.redirect(
      new URL(`/login?redirectTo=${redirectTo}`, request.url)
    );
    response.cookies.delete("jwt");
    return response;
  }
}

export const config = {
  matcher: ["/account/:path*"],
};
