package com.edunex.backend.controller;

import com.edunex.backend.dto.AccountDto;
import com.edunex.backend.service.AccountRecoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AccountRecoveryController {

    private final AccountRecoveryService service;

    // public (under /api/auth): the person is not signed in
    @PostMapping("/api/auth/forgot-password")
    public ResponseEntity<Map<String, String>> forgot(@Valid @RequestBody AccountDto.Forgot request) {
        service.requestPasswordReset(request.email());
        return ResponseEntity.accepted().body(Map.of("status", "ok"));
    }

    @PostMapping("/api/auth/reset-password")
    public Map<String, String> reset(@Valid @RequestBody AccountDto.Reset request) {
        service.resetPassword(request.token(), request.newPassword());
        return Map.of("status", "ok");
    }

    @PostMapping("/api/auth/confirm-email-change")
    public Map<String, String> confirm(@Valid @RequestBody AccountDto.Token request) {
        service.confirmEmailChange(request.token());
        return Map.of("status", "ok");
    }

    // signed in
    @PostMapping("/api/profile/email-change")
    public ResponseEntity<Map<String, String>> emailChange(@Valid @RequestBody AccountDto.EmailChange request,
                                                           Authentication auth) {
        service.requestEmailChange(auth.getName(), request.password(), request.newEmail());
        return ResponseEntity.accepted().body(Map.of("status", "ok"));
    }
}