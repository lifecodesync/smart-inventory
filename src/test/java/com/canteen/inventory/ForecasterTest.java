package com.canteen.inventory;

import com.canteen.inventory.service.Forecaster;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Pure maths tests: every expected number here was calculated by hand (see docs/forecast-by-hand.md). */
class ForecasterTest {

    // 7 days of samosa sales, oldest first
    private final double[] week = {20, 22, 18, 25, 30, 10, 2};

    @Test
    void movingAverageIsMeanOfLastWindowDays() {
        // (20+22+18+25+30+10+2) / 7 = 127 / 7 = 18.142857
        assertThat(Forecaster.movingAverage(week, 7)).isCloseTo(18.142857, within(1e-6));
        // last 3 days: (30+10+2)/3 = 14
        assertThat(Forecaster.movingAverage(week, 3)).isCloseTo(14.0, within(1e-9));
    }

    @Test
    void exponentialSmoothingWeightsRecentDaysMore() {
        // alpha 0.5: 20 -> 21 -> 19.5 -> 22.25 -> 26.125 -> 18.0625 -> 10.03125
        assertThat(Forecaster.exponentialSmoothing(week, 0.5)).isCloseTo(10.03125, within(1e-9));
    }

    @Test
    void maeComparesOneDayAheadForecastsWithActuals() {
        double[] h = {10, 20, 30, 40};
        // last 2 days with a 2-day moving average:
        // day 3 forecast = (10+20)/2 = 15, actual 30 -> error 15
        // day 4 forecast = (20+30)/2 = 25, actual 40 -> error 15
        // MAE = (15 + 15) / 2 = 15
        assertThat(Forecaster.meanAbsoluteError(h, 2, x -> Forecaster.movingAverage(x, 2))).isCloseTo(15.0, within(1e-9));
    }

    @Test
    void restockFollowsTheFormulaAndNeverGoesNegative() {
        // predicted 70 + safety 10 - stock 30 = 50
        assertThat(Forecaster.restockQuantity(70, 10, 30)).isEqualTo(50);
        // plenty of stock -> 0, never negative
        assertThat(Forecaster.restockQuantity(70, 10, 500)).isZero();
    }

    @Test
    void safetyStockUsesZTimesSigmaTimesRootLeadTime() {
        // 1.65 * 4 * sqrt(4) = 13.2 -> rounded up to 14
        assertThat(Forecaster.safetyStock(4.0, 1.65, 4)).isEqualTo(14);
    }

    @Test
    void emptyHistoryForecastsZero() {
        assertThat(Forecaster.movingAverage(new double[0], 7)).isZero();
        assertThat(Forecaster.exponentialSmoothing(new double[0], 0.3)).isZero();
    }
}
