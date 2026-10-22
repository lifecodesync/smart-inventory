package com.digitalshop.inventory.service;

import java.util.Arrays;
import java.util.function.Function;

/**
 * Pure maths, no database: easy to test by hand.
 * Input "history" = units sold per day, oldest first, e.g. [20, 22, 18, ...].
 */
public final class Forecaster {

    private Forecaster() { }

    /** Moving average: tomorrow = average of the last `window` days. Every day counts equally. */
    public static double movingAverage(double[] history, int window) {
        if (history.length == 0) return 0;
        int w = Math.min(window, history.length);
        double sum = 0;
        for (int i = history.length - w; i < history.length; i++) {
            sum += history[i];
        }
        return sum / w;
    }

    /**
     * Simple exponential smoothing: level = alpha * today + (1 - alpha) * old level.
     * Newer days count more; alpha (0..1) decides how much more.
     */
    public static double exponentialSmoothing(double[] history, double alpha) {
        if (history.length == 0) return 0;
        double level = history[0];
        for (int i = 1; i < history.length; i++) {
            level = alpha * history[i] + (1 - alpha) * level;
        }
        return level;
    }

    /**
     * Mean Absolute Error over the last `testDays` days, one day ahead at a time:
     * pretend we are at day t, forecast day t using only days before t, compare with what really happened.
     * MAE = average of |actual - forecast|, in units. Lower = better.
     */
    public static double meanAbsoluteError(double[] history, int testDays, Function<double[], Double> method) {
        int n = history.length;
        int start = Math.max(1, n - testDays);
        if (start >= n) return 0;
        double totalError = 0;
        for (int t = start; t < n; t++) {
            double predicted = method.apply(Arrays.copyOfRange(history, 0, t));
            totalError += Math.abs(history[t] - predicted);
        }
        return totalError / (n - start);
    }

    /** Sample standard deviation: how much daily sales jump around. */
    public static double stdDev(double[] values) {
        if (values.length < 2) return 0;
        double mean = Arrays.stream(values).average().orElse(0);
        double sq = 0;
        for (double v : values) sq += (v - mean) * (v - mean);
        return Math.sqrt(sq / (values.length - 1));
    }

    /** Safety stock = extra buffer for bad luck: z * daily std-dev * sqrt(lead time). */
    public static int safetyStock(double dailyStdDev, double z, int leadTimeDays) {
        return (int) Math.ceil(z * dailyStdDev * Math.sqrt(leadTimeDays));
    }

    /** restock = max(0, predicted demand + safety stock - current stock). */
    public static int restockQuantity(double predictedDemand, int safetyStock, int currentStock) {
        return (int) Math.max(0, Math.ceil(predictedDemand + safetyStock - currentStock));
    }
}
