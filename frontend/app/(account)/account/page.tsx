export const dynamic = "force-dynamic";

import { cookies } from "next/headers";
import { apiFetch } from "@/lib/api-client";
import { AuthUserResponse } from "@/types/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { User } from "lucide-react";

export const metadata = { title: "My Account" };

export default async function AccountPage() {
  const cookieStore = await cookies();
  const token = cookieStore.get("jwt")?.value;

  let user: AuthUserResponse | null = null;
  if (token) {
    user = await apiFetch<AuthUserResponse>("/api/auth/me", { token }).catch(
      () => null
    );
  }

  if (!user) {
    return <p className="text-muted-foreground">Unable to load profile.</p>;
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">My Account</h1>
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <User className="h-5 w-5" />
            Profile
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <p className="text-muted-foreground text-xs uppercase tracking-wide mb-1">
                First Name
              </p>
              <p className="font-medium">{user.firstName}</p>
            </div>
            <div>
              <p className="text-muted-foreground text-xs uppercase tracking-wide mb-1">
                Last Name
              </p>
              <p className="font-medium">{user.lastName}</p>
            </div>
          </div>
          <div>
            <p className="text-muted-foreground text-xs uppercase tracking-wide mb-1">
              Email
            </p>
            <p className="font-medium">{user.email}</p>
          </div>
          <div>
            <p className="text-muted-foreground text-xs uppercase tracking-wide mb-1">
              Phone
            </p>
            <p className="font-medium">{user.phoneNumber}</p>
          </div>
          <div>
            <p className="text-muted-foreground text-xs uppercase tracking-wide mb-1">
              Role
            </p>
            <Badge variant="secondary">{user.role}</Badge>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
