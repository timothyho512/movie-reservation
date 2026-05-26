"use client";

import { create } from "zustand";
import { persist } from "zustand/middleware";
import { AuthUserResponse } from "@/types/api";

interface AuthState {
  user: AuthUserResponse | null;
  setUser: (user: AuthUserResponse) => void;
  clearUser: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      setUser: (user) => set({ user }),
      clearUser: () => set({ user: null }),
    }),
    {
      name: "auth-user",
    }
  )
);
