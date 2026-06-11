export const dynamic = "force-dynamic";

import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { getTheatre } from "@/lib/api/theatres";
import { ApiError } from "@/lib/api-client";
import { Badge } from "@/components/ui/badge";
import { MapPin, Phone } from "lucide-react";

type Props = { params: Promise<{ id: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { id } = await params;
  try {
    const theatre = await getTheatre(Number(id));
    return { title: theatre.name };
  } catch {
    return { title: "Theatre" };
  }
}

const screenTypeColor: Record<string, string> = {
  IMAX: "bg-blue-100 text-blue-800",
  DOLBY_ATMOS: "bg-purple-100 text-purple-800",
  FOUR_DX: "bg-orange-100 text-orange-800",
  THREE_D: "bg-green-100 text-green-800",
  VIP: "bg-yellow-100 text-yellow-800",
  STANDARD: "",
};

export default async function TheatreDetailPage({ params }: Props) {
  const { id } = await params;
  let theatre;

  try {
    theatre = await getTheatre(Number(id));
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) notFound();
    throw e;
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-10">
      <h1 className="text-3xl font-bold mb-2">{theatre.name}</h1>
      <div className="flex flex-col gap-1 text-muted-foreground text-sm mb-8">
        <span className="flex items-center gap-1.5">
          <MapPin className="h-4 w-4" />
          {theatre.address}, {theatre.city}, {theatre.state}, {theatre.country}{" "}
          {theatre.postalCode}
        </span>
        {theatre.phoneNumber && (
          <span className="flex items-center gap-1.5">
            <Phone className="h-4 w-4" />
            {theatre.phoneNumber}
          </span>
        )}
      </div>

      <section>
        <h2 className="text-xl font-semibold mb-4">
          Screens ({theatre.screens?.length ?? 0})
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {theatre.screens?.map((screen) => (
            <div key={screen.id} className="border rounded-lg p-4 flex items-center justify-between">
              <div>
                <p className="font-medium">{screen.name}</p>
                <p className="text-sm text-muted-foreground">
                  {screen.totalSeats} seats
                </p>
              </div>
              <Badge
                variant="secondary"
                className={screenTypeColor[screen.screenType] ?? ""}
              >
                {screen.screenType}
              </Badge>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
