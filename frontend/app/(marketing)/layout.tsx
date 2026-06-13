import { NavBar } from "@/components/layout/NavBar";

export default function MarketingLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <>
      <NavBar />
      <main className="flex-1">{children}</main>
    </>
  );
}
