package com.edunex.backend.controller;

import com.edunex.backend.entity.EmailPreference;
import com.edunex.backend.entity.User;
import com.edunex.backend.repository.EmailPreferenceRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile/email-notifications")
@RequiredArgsConstructor
public class EmailPreferenceController {

    public record Toggle(boolean enabled) {
    }

    private final EmailPreferenceRepository preferences;
    private final UserRepository users;

    @GetMapping
    public Toggle get(Authentication auth) {
        return new Toggle(preferences.findById(userId(auth)).map(EmailPreference::isEnabled).orElse(true));
    }

    @PutMapping
    public Toggle set(@RequestBody Toggle body, Authentication auth) {
        EmailPreference p = preferences.findById(userId(auth)).orElseGet(() -> {
            EmailPreference n = new EmailPreference();
            n.setUserId(userId(auth));
            return n;
        });
        p.setEnabled(body.enabled());
        preferences.save(p);
        return body;
    }

    private Long userId(Authentication auth) {
        return users.findByEmail(auth.getName()).map(User::getId)
                .orElseThrow(() -> new AccessDeniedException("Unknown user"));
    }
}