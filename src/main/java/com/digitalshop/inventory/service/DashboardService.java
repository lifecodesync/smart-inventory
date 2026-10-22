package com.digitalshop.inventory.service;

import com.digitalshop.inventory.config.AppProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

@Service
public class DashboardService {

    private final JdbcTemplate jdbc;
    private final AppProperties props;
    private final Clock clock;

    public DashboardService(JdbcTemplate jdbc, AppProperties props, Clock clock) {
        this.jdbc = jdbc;
        this.props = props;
        this.clock = clock;
    }

    public record Summary(long productCount, BigDecimal stockValue, long outOfStock, long lowStock) { }
    public record SalesTotal(BigDecimal revenue, long units, long bills) { }
    public record DayRevenue(LocalDate day, BigDecimal revenue, long units) { }
    public record SellerRow(String name, long units, BigDecimal revenue) { }
    public record StockRow(long id, String name, int stock, int reorderLevel, String unit) { }

    public record Dashboard(Summary summary, SalesTotal today, SalesTotal last7Days,
                            List<DayRevenue> daily, List<SellerRow> best, List<SellerRow> slow,
                            List<StockRow> attention) { }

    @Transactional(readOnly = true)
    public Dashboard load() {
        LocalDate today = LocalDate.now(clock.withZone(props.zone()));
        return new Dashboard(
                summary(),
                salesBetween(today, today),
                salesBetween(today.minusDays(6), today),
                dailyRevenue(today.minusDays(13), today),
                sellers(today.minusDays(29), true),
                sellers(today.minusDays(29), false),
                needsAttention());
    }

    public Summary summary() {
        return jdbc.queryForObject("""
                SELECT COUNT(*)                                                  AS n,
                       COALESCE(SUM(stock * price), 0)                           AS value,
                       COUNT(*) FILTER (WHERE stock = 0)                         AS out_of_stock,
                       COUNT(*) FILTER (WHERE stock > 0 AND stock <= reorder_level) AS low
                FROM product
                """, (rs, i) -> new Summary(rs.getLong("n"), rs.getBigDecimal("value"),
                rs.getLong("out_of_stock"), rs.getLong("low")));
    }

    /** Sales from the start of `from` to the end of `to` (both inclusive, India time). */
    public SalesTotal salesBetween(LocalDate from, LocalDate to) {
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(quantity * unit_price), 0) AS revenue,
                       COALESCE(SUM(quantity), 0)              AS units,
                       COUNT(*)                                AS bills
                FROM stock_movement
                WHERE type = 'SALE' AND created_at >= ? AND created_at < ?
                """, (rs, i) -> new SalesTotal(rs.getBigDecimal("revenue"), rs.getLong("units"), rs.getLong("bills")),
                startOf(from), startOf(to.plusDays(1)));
    }

    public List<DayRevenue> dailyRevenue(LocalDate from, LocalDate to) {
        String zone = props.zone().getId();
        return jdbc.query("""
                SELECT d.day,
                       COALESCE(SUM(m.quantity * m.unit_price), 0) AS revenue,
                       COALESCE(SUM(m.quantity), 0)                AS units
                FROM generate_series(?::timestamp, ?::timestamp, interval '1 day') AS d(day)
                LEFT JOIN stock_movement m
                       ON m.type = 'SALE'
                      AND m.created_at >= (d.day AT TIME ZONE ?)
                      AND m.created_at <  ((d.day + interval '1 day') AT TIME ZONE ?)
                GROUP BY d.day
                ORDER BY d.day
                """, (rs, i) -> new DayRevenue(rs.getObject("day", java.time.LocalDateTime.class).toLocalDate(),
                rs.getBigDecimal("revenue"), rs.getLong("units")), from, to, zone, zone);
    }

    /**
     * Best (or slowest) sellers since `from`. LEFT JOIN keeps products with zero sales,
     * which matters for slow sellers. The type/date filter sits in the ON clause, not WHERE,
     * otherwise the zero-sale products would disappear.
     */
    public List<SellerRow> sellers(LocalDate from, boolean best) {
        String order = best ? "units DESC, p.name" : "units ASC, p.name";
        return jdbc.query("""
                SELECT p.name,
                       COALESCE(SUM(m.quantity), 0)                AS units,
                       COALESCE(SUM(m.quantity * m.unit_price), 0) AS revenue
                FROM product p
                LEFT JOIN stock_movement m
                       ON m.product_id = p.id AND m.type = 'SALE' AND m.created_at >= ?
                GROUP BY p.id, p.name
                ORDER BY """ + " " + order + " LIMIT 5",
                (rs, i) -> new SellerRow(rs.getString("name"), rs.getLong("units"), rs.getBigDecimal("revenue")),
                startOf(from));
    }

    public List<StockRow> needsAttention() {
        return jdbc.query("""
                SELECT id, name, stock, reorder_level, unit FROM product
                WHERE stock <= reorder_level
                ORDER BY stock, name
                """, (rs, i) -> new StockRow(rs.getLong("id"), rs.getString("name"), rs.getInt("stock"),
                rs.getInt("reorder_level"), rs.getString("unit")));
    }

    private OffsetDateTime startOf(LocalDate day) {
        return day.atStartOfDay(props.zone()).toOffsetDateTime();
    }
}
