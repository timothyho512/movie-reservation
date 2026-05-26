"use client";

import { useAuthStore } from "@/stores/auth-store";

export function useAuth() {
  const user = useAuthStore((s) => s.user);
  const setUser = useAuthStore((s) => s.setUser);
  const clearUser = useAuthStore((s) => s.clearUser);

  return {
    user,
    isLoggedIn: user !== null,
    isAdmin: user?.role === "ADMIN" || user?.role === "MANAGER",
    setUser,
    clearUser,
  };
}
