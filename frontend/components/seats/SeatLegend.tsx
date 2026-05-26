export function SeatLegend() {
  const items = [
    { color: "bg-slate-200", label: "Regular" },
    { color: "bg-amber-200", label: "VIP" },
    { color: "bg-sky-200", label: "Wheelchair" },
    { color: "bg-primary", label: "Selected" },
    { color: "bg-muted opacity-50", label: "Unavailable" },
  ];

  return (
    <div className="flex flex-wrap justify-center gap-4 text-xs text-muted-foreground">
      {items.map(({ color, label }) => (
        <span key={label} className="flex items-center gap-1.5">
          <span className={`h-4 w-4 rounded ${color} inline-block`} />
          {label}
        </span>
      ))}
    </div>
  );
}
