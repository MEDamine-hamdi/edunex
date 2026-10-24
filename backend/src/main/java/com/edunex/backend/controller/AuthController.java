package com.edunex.backend.controller;

import com.edunex.backend.dto.LoginRequest;
import com.edunex.backend.dto.LoginResponse;
import com.edunex.backend.entity.User;
import com.edunex.backend.exception.TooManyRequestsException;
import com.edunex.backend.repository.UserRepository;
import com.edunex.backend.security.JwtService;
import com.edunex.backend.security.LoginThrottleService;
import com.edunex.backend.security.SessionCookies;
import com.edunex.backend.service.ActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final ActivityLogService activityLogService;
    private final LoginThrottleService throttle;
    private final SessionCookies cookies;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest http, HttpServletResponse response) {
        String ip = http.getRemoteAddr();
        String account = "login:" + request.email().toLowerCase();
        String pair = account + "|" + ip;

        // 5 failures from one address, or 30 on one account from anywhere: locked for 15 minutes
        long wait = Math.max(throttle.lockedSeconds(pair), throttle.lockedSeconds(account));
        if (wait > 0) {
            throw new TooManyRequestsException("Too many failed attempts. Try again later.", wait);
        }
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (AuthenticationException ex) {
            boolean locked = throttle.recordFailure(pair, 5, 15, 15) | throttle.recordFailure(account, 30, 15, 15);
            activityLogService.registerForEmail(
                    (locked ? "LOGIN_LOCKED " : "LOGIN_FAILED ") + request.email(), "User", null, ip);
            throw ex;
        }
        throttle.reset(pair);
        throttle.reset(account);

        User user = userRepository.findByEmail(request.email()).orElseThrow();
        String roleName = user.getRole() != null ? user.getRole().getName() : null;
        String token = jwtService.generateToken(user.getEmail(), roleName);
        activityLogService.registerForEmail("LOGIN", "User", user.getEmail(), ip);

        // Web: the token is in an HttpOnly cookie and never in the answer. Mobile apps ask for it with X-Client: mobile.
        boolean mobile = "mobile".equalsIgnoreCase(http.getHeader("X-Client"));
        if (!mobile) {
            cookies.write(response, token, Boolean.TRUE.equals(request.remember()));
        }
        return ResponseEntity.ok(new LoginResponse(
                mobile ? token : null, "Bearer", user.getId(), user.getEmail(),
                user.getFirstName(), user.getLastName(), roleName));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        cookies.clear(response);
        return ResponseEntity.noContent().build();
    }
}