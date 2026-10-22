package com.digitalshop.inventory;

import com.digitalshop.inventory.config.AppProperties;
import com.digitalshop.inventory.service.DashboardService;
import com.digitalshop.inventory.service.ForecastRow;
import com.digitalshop.inventory.service.ForecastService;
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
        long cable = product("USB-C Cable", "1000", 50, 20);      // OK
        long earbuds = product("Earbuds", "1500", 5, 10);         // low (5 <= 10)
        product("Smartwatch", "2500", 0, 5);                      // out

        sale(cable, 3, "1000", today, 11);                // ₹3,000
        sale(earbuds, 2, "1500", today, 13);              // ₹3,000
        sale(cable, 4, "1000", today.minusDays(3), 12);   // ₹4,000 (in last 7 days, not today)
        sale(cable, 9, "1000", today.minusDays(10), 12);  // ₹9,000 (outside 7 days)

        var s = dashboard.summary();
        assertThat(s.productCount()).isEqualTo(3);
        // stock value = 50*1000 + 5*1500 + 0*2500 = 57,500
        assertThat(s.stockValue()).isEqualByComparingTo("57500");
        assertThat(s.lowStock()).isEqualTo(1);
        assertThat(s.outOfStock()).isEqualTo(1);

        var todaySales = dashboard.salesBetween(today, today);
        assertThat(todaySales.revenue()).isEqualByComparingTo("6000");
        assertThat(todaySales.units()).isEqualTo(5);
        assertThat(todaySales.bills()).isEqualTo(2);

        var week = dashboard.salesBetween(today.minusDays(6), today);
        assertThat(week.revenue()).isEqualByComparingTo("10000");

        // 14-day series has exactly 14 days, including zero days
        assertThat(dashboard.dailyRevenue(today.minusDays(13), today)).hasSize(14);

        // best seller over 30 days: USB-C Cable (16 units); slow sellers include Smartwatch with 0
        var best = dashboard.sellers(today.minusDays(29), true);
        assertThat(best.get(0).name()).isEqualTo("USB-C Cable");
        assertThat(best.get(0).units()).isEqualTo(16);
        var slow = dashboard.sellers(today.minusDays(29), false);
        assertThat(slow.get(0).name()).isEqualTo("Smartwatch");
        assertThat(slow.get(0).units()).isZero();
    }

    @Test
    void joinDoesNotDoubleCountWhenAProductHasManySales() {
        LocalDate today = LocalDate.now(props.zone());
        long cable = product("USB-C Cable", "1000", 50, 20);
        for (int i = 0; i < 5; i++) sale(cable, 1, "1000", today, 11 + i);
        // 5 sales of 1 unit = 5 units, ₹5,000 — NOT multiplied by the number of rows in another table
        assertThat(dashboard.salesBetween(today, today).revenue()).isEqualByComparingTo("5000");
        assertThat(dashboard.sellers(today.minusDays(29), true).get(0).units()).isEqualTo(5);
    }

    @Test
    void forecastUsesExactlyHistoryDaysAndExcludesToday() {
        LocalDate today = LocalDate.now(props.zone());
        long cable = product("USB-C Cable", "1000", 30, 10);
        // 10 units every day for the full history window, plus a huge sale TODAY that must be ignored
        for (int d = 1; d <= props.forecast().historyDays(); d++) sale(cable, 10, "1000", today.minusDays(d), 12);
        sale(cable, 500, "1000", today, 12);
        // and one sale just OUTSIDE the window that must also be ignored
        sale(cable, 500, "1000", today.minusDays(props.forecast().historyDays() + 1L), 12);

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
