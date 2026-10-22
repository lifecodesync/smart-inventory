package com.canteen.inventory.web;

import com.canteen.inventory.config.AppProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/** Used in templates as ${@fmt.time(x)}: shows times in India time, not server (UTC) time. */
@Component("fmt")
public class Fmt {

    private final DateTimeFormatter short_;
    private final DateTimeFormatter long_;

    public Fmt(AppProperties props) {
        this.short_ = DateTimeFormatter.ofPattern("dd MMM HH:mm").withZone(props.zone());
        this.long_ = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm").withZone(props.zone());
    }

    public String time(Instant t) { return t == null ? "" : short_.format(t); }

    public String dateTime(Instant t) { return t == null ? "" : long_.format(t); }
}
