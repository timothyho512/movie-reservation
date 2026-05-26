"use client";

import { use } from "react";
import { PaymentPolling } from "@/components/checkout/PaymentPolling";
import Link from "next/link";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export default function CheckoutSuccessPage({
  searchParams,
}: {
  searchParams: Promise<{ checkoutReference?: string }>;
}) {
  const { checkoutReference } = use(searchParams);

  if (!checkoutReference) {
    return (
      <div className="flex flex-col items-center py-16 px-4 space-y-4 text-center">
        <p className="text-muted-foreground">No checkout reference found.</p>
        <Link href="/movies" className={cn(buttonVariants({ variant: "outline" }))}>
          Browse Movies
        </Link>
      </div>
    );
  }

  return <PaymentPolling checkoutReference={checkoutReference} />;
}
