import type { Metadata } from "next";
import { getTheatres } from "@/lib/api/theatres";
import { TheatreCard } from "@/components/theatres/TheatreCard";

export const revalidate = 300; // ISR: rebuild every 5 minutes
export const metadata: Metadata = { title: "Theatres" };

export default async function TheatresPage() {
  const theatres = await getTheatres();
  const active = theatres.filter((t) => t.active);

  return (
    <div className="mx-auto max-w-7xl px-4 py-10">
      <h1 className="text-3xl font-bold mb-8">Our Theatres</h1>
      {active.length === 0 ? (
        <p className="text-muted-foreground">No theatres available.</p>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {active.map((theatre) => (
            <TheatreCard key={theatre.id} theatre={theatre} />
          ))}
        </div>
      )}
    </div>
  );
}
