package com.canteen.inventory.service;

import com.canteen.inventory.config.AppProperties;
import com.canteen.inventory.domain.Product;
import com.canteen.inventory.repository.ProductRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;

@Service
public class ForecastService {

    private static final int HORIZON_DAYS = 7;

    private final JdbcTemplate jdbc;
    private final ProductRepository products;
    private final AppProperties props;
    private final Clock clock;

    public ForecastService(JdbcTemplate jdbc, ProductRepository products, AppProperties props, Clock clock) {
        this.jdbc = jdbc;
        this.products = products;
        this.props = props;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ForecastRow> forecastAll() {
        AppProperties.Forecast f = props.forecast();
        LocalDate lastFullDay = LocalDate.now(clock.withZone(props.zone())).minusDays(1);   // today is not finished yet
        LocalDate firstDay = lastFullDay.minusDays(f.historyDays() - 1L);                   // exactly historyDays days
        Map<Long, double[]> history = dailySales(firstDay, lastFullDay);

        List<ForecastRow> rows = new ArrayList<>();
        for (Product p : products.findAllByOrderByNameAsc()) {
            double[] h = history.getOrDefault(p.getId(), new double[0]);
            rows.add(forecastOne(p, h, f));
        }
        rows.sort(Comparator.comparingInt(ForecastRow::restockQuantity).reversed()
                .thenComparing(r -> r.product().getName()));
        return rows;
    }

    static ForecastRow forecastOne(Product p, double[] h, AppProperties.Forecast f) {
        double ma = Forecaster.movingAverage(h, f.window());
        double ses = Forecaster.exponentialSmoothing(h, f.alpha());
        double maeMa = Forecaster.meanAbsoluteError(h, f.testDays(), x -> Forecaster.movingAverage(x, f.window()));
        double maeSes = Forecaster.meanAbsoluteError(h, f.testDays(), x -> Forecaster.exponentialSmoothing(x, f.alpha()));

        boolean useMa = maeMa <= maeSes;          // pick whichever was more accurate on recent days
        double daily = useMa ? ma : ses;
        double predicted7 = daily * HORIZON_DAYS;
        int safety = Forecaster.safetyStock(Forecaster.stdDev(h), f.serviceZ(), f.leadTimeDays());
        int restock = Forecaster.restockQuantity(predicted7, safety, p.getStock());
        Double cover = daily > 0 ? p.getStock() / daily : null;

        return new ForecastRow(p, ma, ses, maeMa, maeSes, useMa ? "Moving average" : "Exp. smoothing",
                predicted7, safety, restock, cover);
    }

    /**
     * Units sold per product per day, including days with 0 sales (generate_series fills the gaps).
     * Days are calendar days in India (app.zone), and the range filter stays index-friendly.
     */
    Map<Long, double[]> dailySales(LocalDate from, LocalDate to) {
        int days = (int) (to.toEpochDay() - from.toEpochDay()) + 1;
        String zone = props.zone().getId();
        String sql = """
                SELECT p.id AS product_id, d.day, COALESCE(SUM(m.quantity), 0) AS qty
                FROM product p
                CROSS JOIN generate_series(?::timestamp, ?::timestamp, interval '1 day') AS d(day)
                LEFT JOIN stock_movement m
                       ON m.product_id = p.id
                      AND m.type = 'SALE'
                      AND m.created_at >= (d.day AT TIME ZONE ?)
                      AND m.created_at <  ((d.day + interval '1 day') AT TIME ZONE ?)
                GROUP BY p.id, d.day
                ORDER BY p.id, d.day
                """;
        Map<Long, double[]> result = new HashMap<>();
        jdbc.query(sql, rs -> {
            long id = rs.getLong("product_id");
            LocalDate day = rs.getObject("day", java.time.LocalDateTime.class).toLocalDate();
            int index = (int) (day.toEpochDay() - from.toEpochDay());
            result.computeIfAbsent(id, k -> new double[days])[index] = rs.getDouble("qty");
        }, from, to, zone, zone);
        return result;
    }
}
