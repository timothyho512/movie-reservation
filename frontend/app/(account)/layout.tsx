import { NavBar } from "@/components/layout/NavBar";
import Link from "next/link";
import { User, Ticket } from "lucide-react";

export default function AccountLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <>
      <NavBar />
      <div className="mx-auto max-w-7xl px-4 py-8">
        <div className="flex gap-8">
          <aside className="hidden md:block w-48 shrink-0">
            <nav className="space-y-1">
              <Link
                href="/account"
                className="flex items-center gap-2 rounded-md px-3 py-2 text-sm hover:bg-muted transition-colors"
              >
                <User className="h-4 w-4" />
                My Account
              </Link>
              <Link
                href="/account/bookings"
                className="flex items-center gap-2 rounded-md px-3 py-2 text-sm hover:bg-muted transition-colors"
              >
                <Ticket className="h-4 w-4" />
                My Bookings
              </Link>
            </nav>
          </aside>
          <main className="flex-1 min-w-0">{children}</main>
        </div>
      </div>
    </>
  );
}
