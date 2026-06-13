import type { Metadata } from "next";
import "./globals.css";
import { Providers } from "@/components/providers";
import { SiteFooter } from "@/components/layout/SiteFooter";

export const metadata: Metadata = {
  title: {
    default: "CineBook — Movie Reservations",
    template: "%s | CineBook",
  },
  description: "Book movie tickets online. Browse showtimes, pick your seats, and pay securely.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="h-full antialiased">
      <body className="min-h-full flex flex-col bg-background text-foreground">
        <Providers>{children}</Providers>
        <SiteFooter />
      </body>
    </html>
  );
}
