package com.canteen.inventory.web;

import com.canteen.inventory.config.AppProperties;
import com.canteen.inventory.service.AlertService;
import com.canteen.inventory.service.DashboardService;
import com.canteen.inventory.service.ForecastService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Manager-only read pages: dashboard, forecast, alerts. */
@Controller
public class ReportController {

    /** One bar of the 14-day revenue chart; pct = bar height as % of the best day. */
    public record Bar(String label, BigDecimal revenue, long units, int pct) { }

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE dd");

    private final DashboardService dashboard;
    private final ForecastService forecast;
    private final AlertService alerts;
    private final AppProperties props;

    public ReportController(DashboardService dashboard, ForecastService forecast, AlertService alerts,
                            AppProperties props) {
        this.dashboard = dashboard;
        this.forecast = forecast;
        this.alerts = alerts;
        this.props = props;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        var d = dashboard.load();
        double max = d.daily().stream().mapToDouble(x -> x.revenue().doubleValue()).max().orElse(0);
        List<Bar> bars = d.daily().stream()
                .map(x -> new Bar(x.day().format(DAY), x.revenue(), x.units(),
                        max == 0 ? 0 : (int) Math.round(x.revenue().doubleValue() * 100 / max)))
                .toList();
        model.addAttribute("d", d);
        model.addAttribute("bars", bars);
        model.addAttribute("alerts", alerts.currentAlerts());
        return "dashboard";
    }

    @GetMapping("/forecast")
    public String forecast(Model model) {
        model.addAttribute("rows", forecast.forecastAll());
        model.addAttribute("cfg", props.forecast());
        return "forecast";
    }

    @GetMapping("/alerts")
    public String alerts(Model model) {
        model.addAttribute("alerts", alerts.currentAlerts());
        return "alerts";
    }
}
