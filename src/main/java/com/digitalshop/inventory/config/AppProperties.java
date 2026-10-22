package com.digitalshop.inventory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.ZoneId;

/** Typed view of the "app:" section of application.yml. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        ZoneId zone,
        boolean seedDemo,
        String managerPassword,
        String staffPassword,
        Forecast forecast) {

    public record Forecast(int historyDays, int testDays, int window, double alpha,
                           int leadTimeDays, double serviceZ) {
    }
}
