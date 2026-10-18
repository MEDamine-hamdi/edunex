package com.edunex.backend.security;

import com.edunex.backend.service.ActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Records every successful write request (POST, PUT, PATCH, DELETE) made by an authenticated user. */
@Component
@RequiredArgsConstructor
public class ActivityLogInterceptor implements HandlerInterceptor {

    private final ActivityLogService activityLogService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        String method = request.getMethod();
        if (method.equals("GET") || method.equals("HEAD") || method.equals("OPTIONS")) {
            return;
        }
        if (response.getStatus() >= 400) {
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return;
        }
        try {
            String path = request.getRequestURI();
            String verb = switch (method) {
                case "POST" -> "CREATE";
                case "DELETE" -> "DELETE";
                default -> "UPDATE";
            };
            String[] parts = path.split("/");
            String entity = parts.length > 3 ? parts[3] : parts[parts.length - 1];
            activityLogService.registerForEmail(verb + " " + path, entity, auth.getName(), request.getRemoteAddr());
        } catch (Exception ignored) {
            // logging must never break the request
        }
    }
}