"use client";

import { use } from "react";
import Link from "next/link";
import { buttonVariants } from "@/components/ui/button";
import { XCircle } from "lucide-react";
import { cn } from "@/lib/utils";

export default function CheckoutCancelPage({
  searchParams,
}: {
  searchParams: Promise<{ checkoutReference?: string }>;
}) {
  const { checkoutReference } = use(searchParams);

  return (
    <div className="flex flex-col items-center text-center py-16 px-4 space-y-6">
      <div className="rounded-full bg-muted p-5">
        <XCircle className="h-12 w-12 text-muted-foreground" />
      </div>
      <div>
        <h1 className="text-2xl font-bold mb-1">Payment Cancelled</h1>
        <p className="text-muted-foreground max-w-sm">
          You cancelled the payment. Your seat hold will be released shortly.
        </p>
        {checkoutReference && (
          <p className="text-xs text-muted-foreground mt-2">
            Ref: {checkoutReference}
          </p>
        )}
      </div>
      <div className="flex flex-col sm:flex-row gap-3">
        <Link href="/movies" className={cn(buttonVariants({ variant: "outline" }))}>
          Browse Movies
        </Link>
        <Link href="/" className={cn(buttonVariants())}>
          Go Home
        </Link>
      </div>
    </div>
  );
}
