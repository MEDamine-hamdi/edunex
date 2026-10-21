package com.edunex.backend.controller;

import com.edunex.backend.dto.MailDto;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.service.MailOutboxService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/platform/mail")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class PlatformMailController {

    private final MailOutboxService mail;

    @GetMapping("/status")
    public MailDto.Status status() {
        return mail.status();
    }

    @PostMapping("/test")
    public Map<String, String> test(@Valid @RequestBody MailDto.TestRequest request) {
        if (!mail.isConfigured()) {
            throw new BadRequestException("E-mail is not configured (Brevo API key and sender)");
        }
        mail.enqueueDirect(request.to(), request.to(), "[EduNex] E-mail de test",
                "<p>Bonjour, ceci est un e-mail de test envoyé depuis la plateforme EduNex.</p>");
        return Map.of("status", "queued");
    }
}