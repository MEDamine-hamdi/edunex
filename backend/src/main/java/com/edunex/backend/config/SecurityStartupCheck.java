package com.edunex.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * Reports risky settings in the log. Only a broken JWT secret is critical, and the application
 * refuses to start for it only when app.security.strict=true. Everything else is a warning.
 */
@Slf4j
@Component
public class SecurityStartupCheck implements CommandLineRunner {

    private static final Set<String> WEAK = Set.of("admin", "password", "admin123", "changeme", "123456789", "azerty123");

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${springdoc.api-docs.enabled:false}")
    private boolean apiDocs;

    @Value("${app.security.strict:false}")
    private boolean strict;

    @Value("${app.public-url:}")
    private String publicUrl;

    @Value("${app.security.cookie-secure:false}")
    private boolean cookieSecure;

    @Override
    public void run(String... args) {
        List<String> critical = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        try {
            if (Base64.getDecoder().decode(jwtSecret.trim()).length < 64) {
                critical.add("jwt.secret is shorter than 64 bytes: generate a new one");
            }
        } catch (IllegalArgumentException e) {
            critical.add("jwt.secret is not valid Base64");
        }
        if (expirationMs > 12L * 3600 * 1000) {
            warnings.add("jwt.expiration-ms is longer than 12 hours");
        }
        if (!adminPassword.isBlank() && (adminPassword.length() < 12 || WEAK.contains(adminPassword.toLowerCase()))) {
            warnings.add("app.admin.password is weak (12 characters minimum); remove APP_ADMIN_PASSWORD once the admin account exists");
        }
        if (apiDocs) {
            warnings.add("the API documentation (Swagger) is enabled");
        }
        if (publicUrl.isBlank() || publicUrl.contains("localhost") || publicUrl.contains("127.0.0.1")) {
            warnings.add("APP_PUBLIC_URL points to localhost: the links in e-mails (password reset, notifications) will not work for users");
        } else if (publicUrl.startsWith("http://")) {
            warnings.add("APP_PUBLIC_URL is not https");
        }
        if (!cookieSecure) {
            warnings.add("APP_COOKIE_SECURE is not true: set it to true when the site is served over https");
        }

        critical.forEach(p -> log.error("SECURITY (critical): {}", p));
        warnings.forEach(p -> log.warn("SECURITY: {}", p));
        if (strict && !critical.isEmpty()) {
            throw new IllegalStateException("Refusing to start: " + critical);
        }
    }
}