package com.example.moviereservation.service;

import com.example.moviereservation.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AdminReportService {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AdminReportService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Page<ShowtimeOccupancyReportRow> getShowtimeOccupancy(ReportFilter filter) {
        String baseSql = """
                SELECT
                    st.id AS showtime_id,
                    m.id AS movie_id,
                    m.title AS movie_title,
                    t.id AS theatre_id,
                    t.name AS theatre_name,
                    sc.id AS screen_id,
                    sc.name AS screen_name,
                    st.start_time,
                    st.total_seats,
                    COUNT(rs.seat_id) AS reserved_seats,
                    CASE
                        WHEN st.total_seats = 0 THEN 0
                        ELSE ROUND((COUNT(rs.seat_id)::numeric / st.total_seats), 4)
                    END AS occupancy_rate
                FROM showtime st
                JOIN movie m ON m.id = st.movie_id
                JOIN screen sc ON sc.id = st.screen_id
                JOIN theatres t ON t.id = sc.theatre_id
                LEFT JOIN reservation r ON r.showtime_id = st.id
                    AND r.status IN ('CONFIRMED', 'COMPLETED')
                    AND r.payment_status = 'PAID'
                LEFT JOIN reservation_seats rs ON rs.reservation_id = r.id
                WHERE (CAST(:fromDate AS TIMESTAMP) IS NULL OR st.start_time >= CAST(:fromDate AS TIMESTAMP))
                  AND (CAST(:toDate AS TIMESTAMP) IS NULL OR st.start_time <= CAST(:toDate AS TIMESTAMP))
                  AND (CAST(:movieId AS BIGINT) IS NULL OR m.id = CAST(:movieId AS BIGINT))
                  AND (CAST(:theatreId AS BIGINT) IS NULL OR t.id = CAST(:theatreId AS BIGINT))
                  AND (CAST(:screenId AS BIGINT) IS NULL OR sc.id = CAST(:screenId AS BIGINT))
                  AND (CAST(:showtimeId AS BIGINT) IS NULL OR st.id = CAST(:showtimeId AS BIGINT))
                GROUP BY st.id, m.id, m.title, t.id, t.name, sc.id, sc.name, st.start_time, st.total_seats
                """;

        return queryPage(
                baseSql,
                sortClause(filter.sort(), Map.of(
                        "startTime", "start_time",
                        "occupancyRate", "occupancy_rate",
                        "reservedSeats", "reserved_seats",
                        "movieTitle", "movie_title"
                ), "start_time ASC"),
                filter,
                (rs, rowNum) -> new ShowtimeOccupancyReportRow(
                        rs.getLong("showtime_id"),
                        rs.getLong("movie_id"),
                        rs.getString("movie_title"),
                        rs.getLong("theatre_id"),
                        rs.getString("theatre_name"),
                        rs.getLong("screen_id"),
                        rs.getString("screen_name"),
                        toLocalDateTime(rs.getTimestamp("start_time")),
                        rs.getInt("total_seats"),
                        rs.getLong("reserved_seats"),
                        rs.getBigDecimal("occupancy_rate")
                )
        );
    }

    public Page<MovieRevenueReportRow> getMovieRevenue(ReportFilter filter) {
        String baseSql = """
                SELECT
                    m.id AS movie_id,
                    m.title AS movie_title,
                    COUNT(DISTINCT r.id) AS reservation_count,
                    COALESCE(SUM(r.number_of_seats), 0) AS tickets_sold,
                    COALESCE(SUM(r.total_price), 0) AS revenue
                FROM reservation r
                JOIN showtime st ON st.id = r.showtime_id
                JOIN movie m ON m.id = st.movie_id
                JOIN screen sc ON sc.id = st.screen_id
                JOIN theatres t ON t.id = sc.theatre_id
                WHERE r.status IN ('CONFIRMED', 'COMPLETED')
                  AND r.payment_status = 'PAID'
                  AND (CAST(:fromDate AS TIMESTAMP) IS NULL OR r.booking_time >= CAST(:fromDate AS TIMESTAMP))
                  AND (CAST(:toDate AS TIMESTAMP) IS NULL OR r.booking_time <= CAST(:toDate AS TIMESTAMP))
                  AND (CAST(:movieId AS BIGINT) IS NULL OR m.id = CAST(:movieId AS BIGINT))
                  AND (CAST(:theatreId AS BIGINT) IS NULL OR t.id = CAST(:theatreId AS BIGINT))
                  AND (CAST(:screenId AS BIGINT) IS NULL OR sc.id = CAST(:screenId AS BIGINT))
                  AND (CAST(:showtimeId AS BIGINT) IS NULL OR st.id = CAST(:showtimeId AS BIGINT))
                GROUP BY m.id, m.title
                """;

        return queryPage(
                baseSql,
                sortClause(filter.sort(), Map.of(
                        "revenue", "revenue",
                        "ticketsSold", "tickets_sold",
                        "reservationCount", "reservation_count",
                        "movieTitle", "movie_title"
                ), "revenue DESC"),
                filter,
                (rs, rowNum) -> new MovieRevenueReportRow(
                        rs.getLong("movie_id"),
                        rs.getString("movie_title"),
                        rs.getLong("reservation_count"),
                        rs.getLong("tickets_sold"),
                        rs.getBigDecimal("revenue")
                )
        );
    }

    public Page<CancelledBookingReportRow> getCancelledBookings(ReportFilter filter) {
        String baseSql = """
                SELECT
                    r.id AS reservation_id,
                    r.booking_reference,
                    m.id AS movie_id,
                    m.title AS movie_title,
                    st.id AS showtime_id,
                    st.start_time AS showtime_start_time,
                    r.cancelled_at,
                    r.number_of_seats,
                    r.total_price,
                    r.payment_status
                FROM reservation r
                JOIN showtime st ON st.id = r.showtime_id
                JOIN movie m ON m.id = st.movie_id
                JOIN screen sc ON sc.id = st.screen_id
                JOIN theatres t ON t.id = sc.theatre_id
                WHERE r.status = 'CANCELLED'
                  AND (CAST(:fromDate AS TIMESTAMP) IS NULL OR r.cancelled_at >= CAST(:fromDate AS TIMESTAMP))
                  AND (CAST(:toDate AS TIMESTAMP) IS NULL OR r.cancelled_at <= CAST(:toDate AS TIMESTAMP))
                  AND (CAST(:movieId AS BIGINT) IS NULL OR m.id = CAST(:movieId AS BIGINT))
                  AND (CAST(:theatreId AS BIGINT) IS NULL OR t.id = CAST(:theatreId AS BIGINT))
                  AND (CAST(:screenId AS BIGINT) IS NULL OR sc.id = CAST(:screenId AS BIGINT))
                  AND (CAST(:showtimeId AS BIGINT) IS NULL OR st.id = CAST(:showtimeId AS BIGINT))
                """;

        return queryPage(
                baseSql,
                sortClause(filter.sort(), Map.of(
                        "cancelledAt", "cancelled_at",
                        "totalPrice", "total_price",
                        "showtimeStartTime", "showtime_start_time"
                ), "cancelled_at DESC NULLS LAST"),
                filter,
                (rs, rowNum) -> new CancelledBookingReportRow(
                        rs.getLong("reservation_id"),
                        rs.getString("booking_reference"),
                        rs.getLong("movie_id"),
                        rs.getString("movie_title"),
                        rs.getLong("showtime_id"),
                        toLocalDateTime(rs.getTimestamp("showtime_start_time")),
                        toLocalDateTime(rs.getTimestamp("cancelled_at")),
                        rs.getInt("number_of_seats"),
                        rs.getBigDecimal("total_price"),
                        rs.getString("payment_status")
                )
        );
    }

    public Page<PopularSeatReportRow> getPopularSeats(ReportFilter filter) {
        String baseSql = """
                SELECT
                    sc.id AS screen_id,
                    sc.name AS screen_name,
                    t.id AS theatre_id,
                    t.name AS theatre_name,
                    s.row_label,
                    s.seat_number,
                    s.seat_type,
                    COUNT(*) AS booking_count,
                    COALESCE(SUM(r.total_price / NULLIF(r.number_of_seats, 0)), 0) AS revenue
                FROM reservation r
                JOIN reservation_seats rs ON rs.reservation_id = r.id
                JOIN seat s ON s.id = rs.seat_id
                JOIN showtime st ON st.id = r.showtime_id
                JOIN movie m ON m.id = st.movie_id
                JOIN screen sc ON sc.id = st.screen_id
                JOIN theatres t ON t.id = sc.theatre_id
                WHERE r.status IN ('CONFIRMED', 'COMPLETED')
                  AND r.payment_status = 'PAID'
                  AND (CAST(:fromDate AS TIMESTAMP) IS NULL OR r.booking_time >= CAST(:fromDate AS TIMESTAMP))
                  AND (CAST(:toDate AS TIMESTAMP) IS NULL OR r.booking_time <= CAST(:toDate AS TIMESTAMP))
                  AND (CAST(:movieId AS BIGINT) IS NULL OR m.id = CAST(:movieId AS BIGINT))
                  AND (CAST(:theatreId AS BIGINT) IS NULL OR t.id = CAST(:theatreId AS BIGINT))
                  AND (CAST(:screenId AS BIGINT) IS NULL OR sc.id = CAST(:screenId AS BIGINT))
                  AND (CAST(:showtimeId AS BIGINT) IS NULL OR st.id = CAST(:showtimeId AS BIGINT))
                GROUP BY sc.id, sc.name, t.id, t.name, s.row_label, s.seat_number, s.seat_type
                """;

        return queryPage(
                baseSql,
                sortClause(filter.sort(), Map.of(
                        "bookingCount", "booking_count",
                        "revenue", "revenue",
                        "rowLabel", "row_label",
                        "seatNumber", "seat_number"
                ), "booking_count DESC"),
                filter,
                (rs, rowNum) -> new PopularSeatReportRow(
                        rs.getLong("screen_id"),
                        rs.getString("screen_name"),
                        rs.getLong("theatre_id"),
                        rs.getString("theatre_name"),
                        rs.getString("row_label"),
                        rs.getInt("seat_number"),
                        rs.getString("seat_type"),
                        rs.getLong("booking_count"),
                        rs.getBigDecimal("revenue")
                )
        );
    }

    public Page<CheckoutConversionReportRow> getCheckoutConversion(ReportFilter filter) {
        String baseSql = """
                SELECT
                    st.id AS showtime_id,
                    m.id AS movie_id,
                    m.title AS movie_title,
                    COUNT(cs.id) AS checkout_count,
                    COUNT(cs.id) FILTER (WHERE cs.status IN ('PAID', 'FINALIZED')) AS paid_checkout_count,
                    COUNT(cs.id) FILTER (WHERE cs.status IN ('EXPIRED', 'CANCELLED', 'FAILED')) AS abandoned_checkout_count,
                    CASE
                        WHEN COUNT(cs.id) = 0 THEN 0
                        ELSE ROUND((COUNT(cs.id) FILTER (WHERE cs.status IN ('PAID', 'FINALIZED'))::numeric / COUNT(cs.id)), 4)
                    END AS conversion_rate,
                    CASE
                        WHEN COUNT(cs.id) = 0 THEN 0
                        ELSE ROUND((COUNT(cs.id) FILTER (WHERE cs.status IN ('EXPIRED', 'CANCELLED', 'FAILED'))::numeric / COUNT(cs.id)), 4)
                    END AS abandoned_rate
                FROM checkout_sessions cs
                JOIN showtime st ON st.id = cs.showtime_id
                JOIN movie m ON m.id = st.movie_id
                JOIN screen sc ON sc.id = st.screen_id
                JOIN theatres t ON t.id = sc.theatre_id
                WHERE (CAST(:fromDate AS TIMESTAMP) IS NULL OR cs.created_at >= CAST(:fromDate AS TIMESTAMP))
                  AND (CAST(:toDate AS TIMESTAMP) IS NULL OR cs.created_at <= CAST(:toDate AS TIMESTAMP))
                  AND (CAST(:movieId AS BIGINT) IS NULL OR m.id = CAST(:movieId AS BIGINT))
                  AND (CAST(:theatreId AS BIGINT) IS NULL OR t.id = CAST(:theatreId AS BIGINT))
                  AND (CAST(:screenId AS BIGINT) IS NULL OR sc.id = CAST(:screenId AS BIGINT))
                  AND (CAST(:showtimeId AS BIGINT) IS NULL OR st.id = CAST(:showtimeId AS BIGINT))
                GROUP BY st.id, m.id, m.title
                """;

        return queryPage(
                baseSql,
                sortClause(filter.sort(), Map.of(
                        "checkoutCount", "checkout_count",
                        "conversionRate", "conversion_rate",
                        "abandonedRate", "abandoned_rate",
                        "movieTitle", "movie_title"
                ), "checkout_count DESC"),
                filter,
                (rs, rowNum) -> new CheckoutConversionReportRow(
                        rs.getLong("showtime_id"),
                        rs.getLong("movie_id"),
                        rs.getString("movie_title"),
                        rs.getLong("checkout_count"),
                        rs.getLong("paid_checkout_count"),
                        rs.getLong("abandoned_checkout_count"),
                        scaleRate(rs.getBigDecimal("conversion_rate")),
                        scaleRate(rs.getBigDecimal("abandoned_rate"))
                )
        );
    }

    private <T> Page<T> queryPage(String baseSql, String orderBy, ReportFilter filter, org.springframework.jdbc.core.RowMapper<T> rowMapper) {
        MapSqlParameterSource params = params(filter)
                .addValue("limit", filter.size())
                .addValue("offset", (long) filter.page() * filter.size());

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM (" + baseSql + ") report_rows", params, Long.class);
        List<T> rows = jdbcTemplate.query(baseSql + " ORDER BY " + orderBy + " LIMIT :limit OFFSET :offset", params, rowMapper);
        return new PageImpl<>(rows, PageRequest.of(filter.page(), filter.size()), total == null ? 0 : total);
    }

    private MapSqlParameterSource params(ReportFilter filter) {
        return new MapSqlParameterSource()
                .addValue("fromDate", filter.from(), Types.TIMESTAMP)
                .addValue("toDate", filter.to(), Types.TIMESTAMP)
                .addValue("movieId", filter.movieId())
                .addValue("theatreId", filter.theatreId())
                .addValue("screenId", filter.screenId())
                .addValue("showtimeId", filter.showtimeId());
    }

    private String sortClause(String requestedSort, Map<String, String> allowedColumns, String defaultSort) {
        if (requestedSort == null || requestedSort.isBlank()) {
            return defaultSort;
        }

        String[] parts = requestedSort.split(",", 2);
        String column = allowedColumns.get(parts[0].trim());
        if (column == null) {
            return defaultSort;
        }

        String direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim()) ? "ASC" : "DESC";
        return column + " " + direction;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private BigDecimal scaleRate(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    public record ReportFilter(
            LocalDateTime from,
            LocalDateTime to,
            Long movieId,
            Long theatreId,
            Long screenId,
            Long showtimeId,
            int page,
            int size,
            String sort
    ) {
        public ReportFilter {
            page = Math.max(page, 0);
            size = Math.min(Math.max(size, 1), 100);
        }
    }
}
