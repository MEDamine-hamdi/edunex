package com.edunex.backend.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** The login token travels in an HttpOnly, SameSite=Strict cookie: scripts cannot read it. */
@Component
public class SessionCookies {

    public static final String NAME = "edunex_session";

    private final boolean secure;
    private final JwtService jwtService;

    public SessionCookies(@Value("${app.security.cookie-secure:false}") boolean secure, JwtService jwtService) {
        this.secure = secure;
        this.jwtService = jwtService;
    }

    /** persistent = "remember me": the cookie survives the browser; otherwise it ends with the browser session. */
    public void write(HttpServletResponse response, String token, boolean persistent) {
        ResponseCookie.ResponseCookieBuilder cookie = ResponseCookie.from(NAME, token)
                .httpOnly(true).secure(secure).sameSite("Strict").path("/");
        if (persistent) {
            cookie.maxAge(Duration.ofSeconds(jwtService.expirationSeconds()));
        }
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.build().toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(NAME, "")
                .httpOnly(true).secure(secure).sameSite("Strict").path("/").maxAge(0).build().toString());
    }

    public static String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (NAME.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                return c.getValue();
            }
        }
        return null;
    }
}