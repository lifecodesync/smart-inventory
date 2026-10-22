package com.digitalshop.inventory.seed;

import com.digitalshop.inventory.config.AppProperties;
import com.digitalshop.inventory.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Fills an EMPTY database with 12 electronics/mobile-store products and 90 days of realistic history,
 * so the dashboard and forecast have something to show.
 *
 * How the data is made realistic:
 *  - each product has a base daily demand (cables and screen guards sell far more than smartphones)
 *  - weekday pattern: weekends are busiest (Sat 140%, Sun 150%), Monday is quietest (80%)
 *  - random noise of about +/-30% (seeded Random(42), so the data is the same every run)
 *  - stock is SIMULATED: we can only sell what is on the shelf, and we restock when stock
 *    falls to the reorder level. So final stock = purchases - sales, exactly like real life.
 *  - three products stop being restocked in the last 10 days, to create low/out-of-stock alerts.
 */
@Component
@Order(2)
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final int DAYS = 90;

    private record Item(String name, String category, String unit, String price, String cost,
                        double baseDemand, int reorderLevel, int batch, boolean stopRestockAtEnd) { }

    //              name                     category      unit     price    cost   demand/day reorder batch stop
    private static final List<Item> ITEMS = List.of(
            new Item("USB-C Cable 1m", "Accessories", "piece", "299", "120", 14, 20, 60, false),
            new Item("Tempered Glass Guard", "Accessories", "piece", "199", "40", 18, 25, 80, false),
            new Item("Phone Back Cover", "Accessories", "piece", "349", "90", 12, 15, 50, true),
            new Item("Fast Charger 20W", "Chargers", "piece", "999", "550", 8, 12, 40, false),
            new Item("Wired Earphones", "Audio", "piece", "399", "180", 9, 12, 40, false),
            new Item("Wireless Earbuds", "Audio", "piece", "1999", "1200", 5, 8, 25, true),
            new Item("Bluetooth Speaker", "Audio", "piece", "1799", "1100", 2, 3, 10, false),
            new Item("Power Bank 10000mAh", "Power", "piece", "1499", "900", 4, 6, 20, false),
            new Item("Memory Card 64GB", "Storage", "piece", "599", "350", 4, 6, 20, false),
            new Item("Pen Drive 32GB", "Storage", "piece", "449", "250", 5, 8, 25, true),
            new Item("Smartwatch", "Wearables", "piece", "2999", "1900", 2, 3, 10, false),
            new Item("Budget Smartphone", "Phones", "piece", "11999", "9800", 1.5, 2, 8, false));

    private final AppProperties props;
    private final ProductRepository products;
    private final JdbcTemplate jdbc;

    public DemoDataSeeder(AppProperties props, ProductRepository products, JdbcTemplate jdbc) {
        this.props = props;
        this.products = products;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!props.seedDemo() || products.count() > 0) {
            return;
        }
        Random random = new Random(42);
        ZoneId zone = props.zone();
        LocalDate today = LocalDate.now(zone);
        LocalDate start = today.minusDays(DAYS);
        List<Object[]> rows = new ArrayList<>();

        for (Item item : ITEMS) {
            int stock = item.batch();
            Long id = jdbc.queryForObject("""
                    INSERT INTO product (name, category, unit, price, stock, reorder_level, created_at)
                    VALUES (?, ?, ?, ?, 0, ?, ?) RETURNING id
                    """, Long.class, item.name(), item.category(), item.unit(), new BigDecimal(item.price()),
                    item.reorderLevel(), ts(start.atTime(9, 0).atZone(zone)));
            rows.add(movement(id, "PURCHASE", item.batch(), item.cost(), "manager", start.atTime(9, 30).atZone(zone)));

            for (LocalDate day = start; day.isBefore(today); day = day.plusDays(1)) {
                boolean lastTenDays = day.isAfter(today.minusDays(11));
                // restock before the shop opens if at or below reorder level
                if (stock <= item.reorderLevel() && !(item.stopRestockAtEnd() && lastTenDays)) {
                    stock += item.batch();
                    rows.add(movement(id, "PURCHASE", item.batch(), item.cost(), "manager", day.atTime(9, 30).atZone(zone)));
                }
                int demand = demandFor(item, day, random);
                // sales happen in small bills (1-2 items) during shop hours, 10:00-21:59
                while (demand > 0 && stock > 0) {
                    int qty = Math.min(Math.min(demand, 1 + random.nextInt(2)), stock);
                    ZonedDateTime when = day.atTime(10 + random.nextInt(12), random.nextInt(60)).atZone(zone);
                    rows.add(movement(id, "SALE", qty, item.price(), random.nextBoolean() ? "staff" : "manager", when));
                    demand -= qty;
                    stock -= qty;
                }
            }
            jdbc.update("UPDATE product SET stock = ? WHERE id = ?", stock, id);
        }
        jdbc.batchUpdate("""
                INSERT INTO stock_movement (product_id, type, quantity, unit_price, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, rows);
        log.info("Demo data created: {} products, {} stock movements over {} days", ITEMS.size(), rows.size(), DAYS);
    }

    private static int demandFor(Item item, LocalDate day, Random random) {
        double weekday = switch (day.getDayOfWeek()) {
            case SATURDAY -> 1.4;
            case SUNDAY -> 1.5;
            case MONDAY -> 0.8;
            case FRIDAY -> 1.0;
            default -> 0.9;
        };
        double noise = 0.7 + random.nextDouble() * 0.6;    // 0.7 .. 1.3
        return (int) Math.round(item.baseDemand() * weekday * noise);
    }

    private static Object[] movement(Long productId, String type, int qty, String price, String by, ZonedDateTime when) {
        return new Object[]{productId, type, qty, new BigDecimal(price), by, ts(when)};
    }

    private static Timestamp ts(ZonedDateTime when) {
        return Timestamp.from(when.toInstant());
    }
}
