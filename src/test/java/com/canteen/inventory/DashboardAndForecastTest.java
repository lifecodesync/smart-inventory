package com.canteen.inventory;

import com.canteen.inventory.config.AppProperties;
import com.canteen.inventory.service.DashboardService;
import com.canteen.inventory.service.ForecastRow;
import com.canteen.inventory.service.ForecastService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Small known data set -> numbers checked against a hand calculation. */
class DashboardAndForecastTest extends DatabaseTest {

    @Autowired DashboardService dashboard;
    @Autowired ForecastService forecast;
    @Autowired AppProperties props;

    private long product(String name, String price, int stock, int reorder) {
        return jdbc.queryForObject("INSERT INTO product (name, category, unit, price, stock, reorder_level) " +
                "VALUES (?, 'Test', 'piece', ?, ?, ?) RETURNING id", Long.class, name, new BigDecimal(price), stock, reorder);
    }

    private void sale(long productId, int qty, String price, LocalDate day, int hour) {
        var when = Timestamp.from(day.atTime(LocalTime.of(hour, 0)).atZone(props.zone()).toInstant());
        jdbc.update("INSERT INTO stock_movement (product_id, type, quantity, unit_price, created_by, created_at) " +
                "VALUES (?, 'SALE', ?, ?, 'staff', ?)", productId, qty, new BigDecimal(price), when);
    }

    @Test
    void summaryAndSalesMatchHandCalculation() {
        LocalDate today = LocalDate.now(props.zone());
        long tea = product("Tea", "10", 50, 20);       // OK
        long samosa = product("Samosa", "15", 5, 10);  // low (5 <= 10)
        product("Upma", "25", 0, 5);                    // out

        sale(tea, 3, "10", today, 9);                  // ₹30
        sale(samosa, 2, "15", today, 11);              // ₹30
        sale(tea, 4, "10", today.minusDays(3), 10);    // ₹40 (in last 7 days, not today)
        sale(tea, 9, "10", today.minusDays(10), 10);   // ₹90 (outside 7 days)

        var s = dashboard.summary();
        assertThat(s.productCount()).isEqualTo(3);
        // stock value = 50*10 + 5*15 + 0*25 = 575
        assertThat(s.stockValue()).isEqualByComparingTo("575");
        assertThat(s.lowStock()).isEqualTo(1);
        assertThat(s.outOfStock()).isEqualTo(1);

        var todaySales = dashboard.salesBetween(today, today);
        assertThat(todaySales.revenue()).isEqualByComparingTo("60");
        assertThat(todaySales.units()).isEqualTo(5);
        assertThat(todaySales.bills()).isEqualTo(2);

        var week = dashboard.salesBetween(today.minusDays(6), today);
        assertThat(week.revenue()).isEqualByComparingTo("100");

        // 14-day series has exactly 14 days, including zero days
        assertThat(dashboard.dailyRevenue(today.minusDays(13), today)).hasSize(14);

        // best seller over 30 days: Tea (16 units); slow sellers include Upma with 0
        var best = dashboard.sellers(today.minusDays(29), true);
        assertThat(best.get(0).name()).isEqualTo("Tea");
        assertThat(best.get(0).units()).isEqualTo(16);
        var slow = dashboard.sellers(today.minusDays(29), false);
        assertThat(slow.get(0).name()).isEqualTo("Upma");
        assertThat(slow.get(0).units()).isZero();
    }

    @Test
    void joinDoesNotDoubleCountWhenAProductHasManySales() {
        LocalDate today = LocalDate.now(props.zone());
        long tea = product("Tea", "10", 50, 20);
        for (int i = 0; i < 5; i++) sale(tea, 1, "10", today, 9 + i);
        // 5 sales of 1 unit = 5 units, ₹50 — NOT multiplied by the number of rows in another table
        assertThat(dashboard.salesBetween(today, today).revenue()).isEqualByComparingTo("50");
        assertThat(dashboard.sellers(today.minusDays(29), true).get(0).units()).isEqualTo(5);
    }

    @Test
    void forecastUsesExactlyHistoryDaysAndExcludesToday() {
        LocalDate today = LocalDate.now(props.zone());
        long tea = product("Tea", "10", 30, 10);
        // 10 units every day for the full history window, plus a huge sale TODAY that must be ignored
        for (int d = 1; d <= props.forecast().historyDays(); d++) sale(tea, 10, "10", today.minusDays(d), 10);
        sale(tea, 500, "10", today, 10);
        // and one sale just OUTSIDE the window that must also be ignored
        sale(tea, 500, "10", today.minusDays(props.forecast().historyDays() + 1L), 10);

        ForecastRow r = forecast.forecastAll().get(0);
        assertThat(r.movingAvgDaily()).isEqualTo(10.0);
        assertThat(r.smoothingDaily()).isEqualTo(10.0);
        assertThat(r.maeMovingAvg()).isZero();
        assertThat(r.predicted7Days()).isEqualTo(70.0);
        assertThat(r.safetyStock()).isZero();                  // no variation -> no buffer needed
        assertThat(r.restockQuantity()).isEqualTo(40);         // 70 + 0 - 30
        assertThat(r.shortageExpected()).isTrue();
    }
}
