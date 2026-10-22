package com.digitalshop.inventory.service;

import com.digitalshop.inventory.domain.Product;

/** One line of the forecast page. */
public record ForecastRow(
        Product product,
        double movingAvgDaily,
        double smoothingDaily,
        double maeMovingAvg,
        double maeSmoothing,
        String chosenMethod,
        double predicted7Days,
        int safetyStock,
        int restockQuantity,
        Double daysOfCover) {

    /** Will we run out within the 7-day forecast window? */
    public boolean shortageExpected() {
        return product.getStock() < predicted7Days;
    }
}
