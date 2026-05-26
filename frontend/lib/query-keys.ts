export const queryKeys = {
  movies: {
    all: ["movies"] as const,
    detail: (id: number) => ["movies", id] as const,
  },
  theatres: {
    all: ["theatres"] as const,
    detail: (id: number) => ["theatres", id] as const,
  },
  showtimes: {
    all: ["showtimes"] as const,
    detail: (id: number) => ["showtimes", id] as const,
    seatMap: (id: number) => ["showtimes", id, "seat-map"] as const,
  },
  checkout: {
    status: (ref: string) => ["checkout", ref] as const,
  },
  reservations: {
    all: ["reservations"] as const,
    detail: (id: number) => ["reservations", id] as const,
  },
};
