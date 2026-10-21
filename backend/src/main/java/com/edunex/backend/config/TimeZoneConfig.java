package com.edunex.backend.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/** One time zone for the whole platform: sessions, attendance windows and e-mails all use it. */
@Configuration
public class TimeZoneConfig {

    @Value("${app.timezone:Africa/Tunis}")
    private String zone;

    @PostConstruct
    public void apply() {
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
    }
}