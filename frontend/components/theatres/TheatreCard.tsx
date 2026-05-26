import Link from "next/link";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { buttonVariants } from "@/components/ui/button";
import { TheatreSummaryResponse } from "@/types/api";
import { MapPin, ScreenShare } from "lucide-react";
import { cn } from "@/lib/utils";

export function TheatreCard({ theatre }: { theatre: TheatreSummaryResponse }) {
  return (
    <Card className="flex flex-col h-full hover:shadow-md transition-shadow">
      <CardHeader className="pb-2">
        <h3 className="font-semibold">{theatre.name}</h3>
        <div className="flex items-start gap-1 text-sm text-muted-foreground">
          <MapPin className="h-4 w-4 mt-0.5 shrink-0" />
          <span>
            {theatre.address}, {theatre.city}, {theatre.country}
          </span>
        </div>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="flex items-center gap-1.5 text-sm text-muted-foreground">
          <ScreenShare className="h-4 w-4" />
          {theatre.totalScreens} screens · {theatre.totalSeats} seats
        </div>
        <Link
          href={`/theatres/${theatre.id}`}
          className={cn(buttonVariants({ variant: "outline", size: "sm" }), "w-full")}
        >
          View Details
        </Link>
      </CardContent>
    </Card>
  );
}
