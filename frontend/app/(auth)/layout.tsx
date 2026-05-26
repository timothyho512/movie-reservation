import Link from "next/link";
import { Film } from "lucide-react";

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-muted/40 px-4">
      <Link href="/" className="mb-8 flex items-center gap-2 font-semibold text-xl">
        <Film className="h-6 w-6" />
        CineBook
      </Link>
      {children}
    </div>
  );
}
