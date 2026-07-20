// ─── Enums ───────────────────────────────────────────────────────────────────

export type UserRole = "CUSTOMER" | "ADMIN" | "MANAGER";

export type SeatType = "REGULAR" | "VIP" | "WHEELCHAIR";

export type ScreenType =
  | "STANDARD"
  | "IMAX"
  | "DOLBY_ATMOS"
  | "THREE_D"
  | "FOUR_DX"
  | "VIP";

export type ShowtimeStatus =
  | "UPCOMING"
  | "ONGOING"
  | "COMPLETED"
  | "CANCELLED";

export type ReservationStatus =
  | "PENDING"
  | "CONFIRMED"
  | "CANCELLED"
  | "COMPLETED";

export type PaymentStatus = "PENDING" | "PAID" | "REFUNDED" | "FAILED";

export type CheckoutSessionStatus =
  | "PENDING_PAYMENT"
  | "PAID"
  | "FAILED"
  | "CANCELLED"
  | "EXPIRED"
  | "FINALIZED"
  | "REFUND_PENDING"
  | "REFUNDED";

export type LockStatus =
  | "LOCKED"
  | "PROCESSING"
  | "EXPIRED"
  | "CONVERTED_TO_RESERVATION";

export type CurrencyCode = "GBP";

// ─── Auth ─────────────────────────────────────────────────────────────────────

export interface AuthUserResponse {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  role: UserRole;
  active: boolean;
}

export interface AuthResponse {
  token: string;
  user: AuthUserResponse;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  phoneNumber: string;
}

// ─── Movies ───────────────────────────────────────────────────────────────────

/** Returned by GET /api/movies */
export interface MovieCardResponse {
  id: number;
  title: string;
  director: string;
  posterPath: string | null;
}

/** Returned by GET /api/movies/{id} */
export interface MovieDetailResponse {
  id: number;
  title: string;
  director: string;
  posterPath: string | null;
  overview: string | null;
  runtimeMinutes: number | null;
  showtimes: ShowtimeSummaryResponse[];
}

// ─── Showtimes ────────────────────────────────────────────────────────────────

export interface ShowtimeSummaryMovieSummary {
  id: number;
  title: string;
  director: string;
}

export interface ShowtimeSummaryTheatreSummary {
  id: number;
  name: string;
  city: string;
  country: string;
}

export interface ShowtimeSummaryScreenSummary {
  id: number;
  name: string;
  screenType: ScreenType;
}

/** Returned by GET /api/showtimes and GET /api/showtimes/{id} */
export interface ShowtimeSummaryResponse {
  id: number;
  movie: ShowtimeSummaryMovieSummary;
  theatre: ShowtimeSummaryTheatreSummary;
  screen: ShowtimeSummaryScreenSummary;
  startTime: string;
  endTime: string;
  basePrice: string;
  availableSeats: number;
  totalSeats: number;
  status: ShowtimeStatus;
}

// ─── Seat Map ─────────────────────────────────────────────────────────────────

export interface SeatMapMovieSummary {
  id: number;
  title: string;
  director: string;
}

export interface SeatMapScreenSummary {
  id: number;
  name: string;
  screenType: ScreenType;
}

export interface SeatMapSeatSummary {
  id: number;
  rowLabel: string;
  seatNumber: number;
  seatType: SeatType;
  price: string;
  available: boolean;
}

/** Returned by GET /api/showtimes/{id}/seat-map */
export interface SeatMapResponse {
  showtimeId: number;
  showtimeStatus: ShowtimeStatus;
  startTime: string;
  endTime: string;
  movie: SeatMapMovieSummary;
  screen: SeatMapScreenSummary;
  seats: SeatMapSeatSummary[];
}

// ─── Theatres ─────────────────────────────────────────────────────────────────

/** Returned by GET /api/theatres */
export interface TheatreSummaryResponse {
  id: number;
  name: string;
  address: string;
  city: string;
  state: string;
  country: string;
  postalCode: string;
  phoneNumber: string;
  totalScreens: number;
  totalSeats: number;
  active: boolean;
}

export interface TheatreDetailScreenSummary {
  id: number;
  name: string;
  totalSeats: number;
  screenType: ScreenType;
  active: boolean;
}

/** Returned by GET /api/theatres/{id} */
export interface TheatreDetailResponse extends TheatreSummaryResponse {
  screens: TheatreDetailScreenSummary[];
}

// ─── Checkout ─────────────────────────────────────────────────────────────────

export interface CheckoutLockRequest {
  showtimeId: number;
  seatIds: number[];
  guestEmail?: string;
}

export interface CheckoutLockResponse {
  sessionId: string | null;
  expiresAt: string;
  lockedSeatIds: number[];
  message: string;
}

export interface CheckoutSessionCreateRequest {
  showtimeId: number;
  seatIds: number[];
  guestEmail?: string;
  sessionId?: string;
}

export interface CheckoutSessionCreateResponse {
  checkoutReference: string;
  stripeCheckoutSessionId: string;
  checkoutUrl: string;
  status: CheckoutSessionStatus;
  expiresAt: string;
  message: string;
}

export interface CheckoutSessionStatusResponse {
  checkoutReference: string;
  status: CheckoutSessionStatus;
  reservationId: number | null;
  bookingReference: string | null;
  message: string;
}

// ─── Reservations ─────────────────────────────────────────────────────────────

export interface ReservationShowtimeSummary {
  id: number;
  startTime: string;
  endTime: string;
}

export interface ReservationMovieSummary {
  id: number;
  title: string;
  director: string;
}

export interface ReservationScreenSummary {
  id: number;
  name: string;
  screenType: ScreenType;
}

export interface ReservationSeatSummary {
  id: number;
  rowLabel: string;
  seatNumber: number;
  seatType: SeatType;
}

/** Returned by GET /api/reservations/{id} */
export interface ReservationResponse {
  reservationId: number;
  reservationReference: string;
  reservationStatus: ReservationStatus;
  paymentStatus: PaymentStatus;
  showtime: ReservationShowtimeSummary;
  movie: ReservationMovieSummary;
  screen: ReservationScreenSummary;
  seats: ReservationSeatSummary[];
  totalAmount: string;
  currency: CurrencyCode;
  createdAt: string;
}

// ─── API Error ────────────────────────────────────────────────────────────────

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

// ─── Admin ───────────────────────────────────────────────────────────────────

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface AdminMovieResponse extends MovieCardResponse {
  tmdbManaged: boolean;
  nowPlaying: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AdminTheatreResponse extends TheatreSummaryResponse {
  createdAt: string;
  updatedAt: string;
}

export interface AdminScreenResponse {
  id: number;
  name: string;
  theatreId: number;
  theatreName: string;
  totalSeats: number;
  screenType: ScreenType;
  active: boolean;
  currentLayoutVersionId: number | null;
  currentLayoutVersion: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface AdminShowtimeResponse {
  id: number;
  movieId: number;
  movieTitle: string;
  theatreId: number;
  theatreName: string;
  screenId: number;
  screenName: string;
  screenType: ScreenType;
  layoutVersionId: number | null;
  layoutVersion: number | null;
  startTime: string;
  endTime: string;
  basePrice: string;
  availableSeats: number;
  totalSeats: number;
  status: ShowtimeStatus;
  createdAt: string;
  updatedAt: string;
}

export interface AdminSeatDefinition {
  rowLabel: string;
  seatNumber: number;
  seatType: SeatType;
  basePrice: number;
}

export interface AdminSeatLayoutResponse {
  screenId: number;
  layoutVersionId: number;
  versionNumber: number;
  totalSeats: number;
  seats: Array<{
    id: number;
    rowLabel: string;
    seatNumber: number;
    seatType: SeatType;
    basePrice: string;
    active: boolean;
  }>;
}

export interface ShowtimeOccupancyReportRow {
  showtimeId: number;
  movieId: number;
  movieTitle: string;
  theatreId: number;
  theatreName: string;
  screenId: number;
  screenName: string;
  startTime: string;
  totalSeats: number;
  reservedSeats: number;
  occupancyRate: number;
}

export interface MovieRevenueReportRow {
  movieId: number;
  movieTitle: string;
  reservationCount: number;
  ticketsSold: number;
  revenue: number;
}

export interface CancelledBookingReportRow {
  reservationId: number;
  bookingReference: string;
  movieId: number;
  movieTitle: string;
  showtimeId: number;
  showtimeStartTime: string;
  cancelledAt: string;
  numberOfSeats: number;
  totalPrice: number;
  paymentStatus: PaymentStatus;
}

export interface PopularSeatReportRow {
  screenId: number;
  screenName: string;
  theatreId: number;
  theatreName: string;
  rowLabel: string;
  seatNumber: number;
  seatType: SeatType;
  bookingCount: number;
  revenue: number;
}

export interface CheckoutConversionReportRow {
  showtimeId: number;
  movieId: number;
  movieTitle: string;
  checkoutCount: number;
  paidCheckoutCount: number;
  abandonedCheckoutCount: number;
  conversionRate: number;
  abandonedRate: number;
}
