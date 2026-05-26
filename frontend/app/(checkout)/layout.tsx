import Link from "next/link";
import { Film, Lock } from "lucide-react";

export default function CheckoutLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen flex flex-col">
      <header className="border-b px-4 h-14 flex items-center justify-between max-w-7xl mx-auto w-full">
        <Link href="/" className="flex items-center gap-2 font-semibold">
          <Film className="h-5 w-5" />
          CineBook
        </Link>
        <div className="flex items-center gap-1.5 text-sm text-muted-foreground">
          <Lock className="h-4 w-4" />
          Secure Checkout
        </div>
      </header>
      <main className="flex-1">{children}</main>
    </div>
  );
}
