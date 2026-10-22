package com.canteen.inventory.service;

import com.canteen.inventory.domain.Product;

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
