package com.canteen.inventory.seed;

import com.canteen.inventory.config.AppProperties;
import com.canteen.inventory.repository.ProductRepository;
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
 * Fills an EMPTY database with 12 canteen products and 90 days of realistic history,
 * so the dashboard and forecast have something to show.
 *
 * How the data is made realistic:
 *  - each product has a base daily demand (tea sells far more than sandwiches)
 *  - weekday pattern: Mon-Fri normal, Saturday ~half, Sunday college closed (~10%)
 *  - random noise of about +/-25% (seeded Random(42), so the data is the same every run)
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

    private static final List<Item> ITEMS = List.of(
            new Item("Tea", "Beverages", "cup", "10", "4", 80, 60, 300, false),
            new Item("Coffee", "Beverages", "cup", "15", "6", 45, 40, 200, false),
            new Item("Samosa", "Snacks", "piece", "15", "8", 60, 50, 200, true),
            new Item("Vada Pav", "Snacks", "piece", "20", "10", 55, 45, 200, false),
            new Item("Poha", "Breakfast", "plate", "25", "12", 30, 25, 100, false),
            new Item("Upma", "Breakfast", "plate", "25", "12", 12, 10, 50, false),
            new Item("Veg Sandwich", "Snacks", "piece", "40", "22", 18, 15, 60, true),
            new Item("Maggi", "Meals", "plate", "35", "15", 25, 20, 100, false),
            new Item("Cold Drink", "Beverages", "bottle", "40", "30", 20, 24, 96, false),
            new Item("Water Bottle", "Beverages", "bottle", "20", "12", 35, 30, 120, true),
            new Item("Chips", "Packaged", "packet", "20", "15", 15, 20, 80, false),
            new Item("Biscuits", "Packaged", "packet", "10", "7", 8, 15, 60, false));

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
                    item.reorderLevel(), ts(start.atTime(7, 0).atZone(zone)));
            rows.add(movement(id, "PURCHASE", item.batch(), item.cost(), "manager", start.atTime(7, 30).atZone(zone)));

            for (LocalDate day = start; day.isBefore(today); day = day.plusDays(1)) {
                boolean lastTenDays = day.isAfter(today.minusDays(11));
                // restock in the morning if at or below reorder level
                if (stock <= item.reorderLevel() && !(item.stopRestockAtEnd() && lastTenDays)) {
                    stock += item.batch();
                    rows.add(movement(id, "PURCHASE", item.batch(), item.cost(), "manager", day.atTime(7, 30).atZone(zone)));
                }
                int demand = demandFor(item, day, random);
                // sales happen in several small bills through the day
                while (demand > 0 && stock > 0) {
                    int qty = Math.min(Math.min(demand, 1 + random.nextInt(4)), stock);
                    ZonedDateTime when = day.atTime(8 + random.nextInt(9), random.nextInt(60)).atZone(zone);
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
            case SATURDAY -> 0.5;
            case SUNDAY -> 0.1;
            case MONDAY -> 1.1;
            default -> 1.0;
        };
        double noise = 0.75 + random.nextDouble() * 0.5;   // 0.75 .. 1.25
        return (int) Math.round(item.baseDemand() * weekday * noise);
    }

    private static Object[] movement(Long productId, String type, int qty, String price, String by, ZonedDateTime when) {
        return new Object[]{productId, type, qty, new BigDecimal(price), by, ts(when)};
    }

    private static Timestamp ts(ZonedDateTime when) {
        return Timestamp.from(when.toInstant());
    }
}
