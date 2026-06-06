import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import Link from "next/link";
import { apiFetch, ApiError } from "@/lib/api-client";
import type { AuthUserResponse } from "@/types/api";
import { AdminShell } from "@/components/admin/AdminShell";

export default async function AdminLayout({ children }: { children: React.ReactNode }) {
  const token = (await cookies()).get("jwt")?.value;
  if (!token) redirect("/login?redirectTo=/admin");

  let user: AuthUserResponse;
  try {
    user = await apiFetch<AuthUserResponse>("/api/auth/me", { token, cache: "no-store" });
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      redirect("/login?redirectTo=/admin");
    }
    throw error;
  }

  if (user.role !== "ADMIN" && user.role !== "MANAGER") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-muted/30 px-4">
        <div className="max-w-md rounded-lg border bg-background p-8 text-center">
          <h1 className="text-xl font-semibold">Access denied</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Your account does not have permission to access cinema administration.
          </p>
          <Link href="/movies" className="mt-5 inline-block text-sm font-medium underline underline-offset-4">
            Return to CineBook
          </Link>
        </div>
      </div>
    );
  }

  return <AdminShell user={user}>{children}</AdminShell>;
}
