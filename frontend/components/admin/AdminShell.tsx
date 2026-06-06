"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  BarChart3,
  Building2,
  CalendarDays,
  Clapperboard,
  ExternalLink,
  Film,
  LayoutDashboard,
  MonitorPlay,
} from "lucide-react";
import { cn } from "@/lib/utils";
import type { AuthUserResponse } from "@/types/api";

const navItems = [
  { href: "/admin", label: "Overview", icon: LayoutDashboard },
  { href: "/admin/movies", label: "Movies", icon: Clapperboard },
  { href: "/admin/theatres", label: "Theatres", icon: Building2 },
  { href: "/admin/screens", label: "Screens", icon: MonitorPlay },
  { href: "/admin/showtimes", label: "Showtimes", icon: CalendarDays },
  { href: "/admin/reports", label: "Reports", icon: BarChart3 },
];

export function AdminShell({
  user,
  children,
}: {
  user: AuthUserResponse;
  children: React.ReactNode;
}) {
  const pathname = usePathname();

  return (
    <div className="admin-theme min-h-screen bg-muted/30">
      <aside className="fixed inset-y-0 left-0 hidden w-56 border-r bg-background lg:flex lg:flex-col">
        <div className="flex h-14 items-center gap-2 border-b px-4 font-semibold">
          <Film className="text-primary" />
          CineBook Admin
        </div>
        <nav className="flex flex-1 flex-col gap-1 p-3">
          {navItems.map((item) => {
            const active =
              item.href === "/admin"
                ? pathname === item.href
                : pathname.startsWith(item.href);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "flex h-9 items-center gap-3 rounded-md px-3 text-sm transition-colors",
                  active
                    ? "bg-primary text-primary-foreground"
                    : "text-muted-foreground hover:bg-muted hover:text-foreground"
                )}
              >
                <item.icon className="size-4" />
                {item.label}
              </Link>
            );
          })}
        </nav>
        <div className="border-t p-3">
          <Link
            href="/movies"
            className="flex h-9 items-center gap-3 rounded-md px-3 text-sm text-muted-foreground hover:bg-muted hover:text-foreground"
          >
            <ExternalLink className="size-4" />
            Return to site
          </Link>
        </div>
      </aside>

      <div className="lg:pl-56">
        <header className="sticky top-0 z-40 border-b bg-background/95 backdrop-blur">
          <div className="flex h-14 items-center justify-between px-4 sm:px-6">
            <Link href="/admin" className="flex items-center gap-2 font-semibold lg:hidden">
              <Film className="size-5 text-primary" />
              CineBook Admin
            </Link>
            <div className="hidden text-sm text-muted-foreground lg:block">
              Cinema operations
            </div>
            <div className="text-right">
              <p className="text-sm font-medium">{user.firstName} {user.lastName}</p>
              <p className="text-xs text-muted-foreground">{user.role}</p>
            </div>
          </div>
          <nav className="flex overflow-x-auto border-t px-2 lg:hidden">
            {navItems.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "flex h-11 shrink-0 items-center gap-1.5 border-b-2 px-3 text-xs",
                  pathname === item.href || (item.href !== "/admin" && pathname.startsWith(item.href))
                    ? "border-primary text-foreground"
                    : "border-transparent text-muted-foreground"
                )}
              >
                <item.icon className="size-4" />
                {item.label}
              </Link>
            ))}
          </nav>
        </header>
        <main className="p-4 sm:p-6">{children}</main>
      </div>
    </div>
  );
}
