package com.digitalshop.inventory.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AlertService {

    private final ForecastService forecast;

    public AlertService(ForecastService forecast) {
        this.forecast = forecast;
    }

    public List<Alert> currentAlerts() {
        List<Alert> out = new ArrayList<>();
        List<Alert> low = new ArrayList<>();
        List<Alert> shortage = new ArrayList<>();
        for (ForecastRow r : forecast.forecastAll()) {
            var p = r.product();
            if (p.isOutOfStock()) {
                out.add(new Alert("OUT", p.getId(), p.getName(),
                        "Out of stock. Suggested restock: " + r.restockQuantity() + " " + p.getUnit()));
            } else if (p.isLowStock()) {
                low.add(new Alert("LOW", p.getId(), p.getName(),
                        "Low stock: " + p.getStock() + " left (reorder level " + p.getReorderLevel() + ")"));
            } else if (r.shortageExpected() && r.daysOfCover() != null) {
                shortage.add(new Alert("SHORTAGE", p.getId(), p.getName(), String.format(
                        "Predicted to run out in about %.1f days. Suggested restock: %d %s",
                        r.daysOfCover(), r.restockQuantity(), p.getUnit())));
            }
        }
        out.addAll(low);
        out.addAll(shortage);
        return out;
    }
}
