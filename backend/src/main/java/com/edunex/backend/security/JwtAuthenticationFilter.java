package com.edunex.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Reads the token from "Authorization: Bearer" (mobile apps, scripts) or from the session cookie (web).
 * A request authenticated by the cookie that changes data must carry "X-Requested-With": a page of another
 * site cannot add that header, which closes cross-site request forgery.
 * An account that still has its temporary password can only read its own identity, set a new password or sign out.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private static final String MUST_CHANGE = "PASSWORD_CHANGE_REQUIRED";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final com.edunex.backend.tenant.TenantAccess tenantAccess;
    private final UserRightsService userRights;
    private final TokenRevocationService tokenRevocation;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = null;
        boolean fromCookie = false;
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        } else {
            token = SessionCookies.read(request);
            fromCookie = token != null;
        }

        if (fromCookie && !SAFE_METHODS.contains(request.getMethod())
                && !"XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing request header\"}");
            return;
        }

        if (token != null && jwtService.isTokenValid(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String email = jwtService.extractEmail(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (userDetails.isEnabled()
                        && !tokenRevocation.isRevoked(email, jwtService.extractIssuedAt(token))) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userRights.effective(userDetails));
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    if (tenantAccess.allows(email)) {
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        com.edunex.backend.tenant.TenantContext.set(tenantAccess.universityOf(email));
                    }

                    boolean mustChange = userDetails.getAuthorities().stream()
                            .anyMatch(a -> MUST_CHANGE.equals(a.getAuthority()));
                    String path = request.getRequestURI();
                    if (mustChange && !path.startsWith("/api/auth/")
                            && !path.equals("/api/users/me")
                            && !path.equals("/api/profile/first-password")) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"PASSWORD_CHANGE_REQUIRED\"}");
                        return;
                    }
                }
            } catch (UsernameNotFoundException ignored) {
                // unknown user (for example after an e-mail change): the request stays unauthenticated
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            com.edunex.backend.tenant.TenantContext.clear();
        }
    }
}