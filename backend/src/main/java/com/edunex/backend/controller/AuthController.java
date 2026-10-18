package com.edunex.backend.controller;

import com.edunex.backend.dto.LoginRequest;
import com.edunex.backend.dto.LoginResponse;
import com.edunex.backend.entity.User;
import com.edunex.backend.exception.TooManyRequestsException;
import com.edunex.backend.repository.UserRepository;
import com.edunex.backend.security.JwtService;
import com.edunex.backend.security.LoginAttemptService;
import com.edunex.backend.service.ActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final LoginAttemptService loginAttempts;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        String ip = http.getRemoteAddr();
        String key = request.email().toLowerCase() + "|" + ip;

        if (loginAttempts.isBlocked(key)) {
            throw new TooManyRequestsException("Too many failed attempts. Try again later.",
                    loginAttempts.retryAfterSeconds(key));
        }
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (AuthenticationException ex) {
            boolean locked = loginAttempts.recordFailure(key);
            activityLogService.registerForEmail(
                    (locked ? "LOGIN_LOCKED " : "LOGIN_FAILED ") + request.email(), "User", null, ip);
            throw ex;
        }
        loginAttempts.reset(key);

        User user = userRepository.findByEmail(request.email()).orElseThrow();
        String roleName = user.getRole() != null ? user.getRole().getName() : null;
        String token = jwtService.generateToken(user.getEmail(), roleName);
        activityLogService.registerForEmail("LOGIN", "User", user.getEmail(), ip);

        return ResponseEntity.ok(new LoginResponse(
                token, "Bearer", user.getId(), user.getEmail(),
                user.getFirstName(), user.getLastName(), roleName));
    }
}