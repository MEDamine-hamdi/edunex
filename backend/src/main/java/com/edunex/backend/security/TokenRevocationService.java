package com.edunex.backend.security;

import com.edunex.backend.entity.TokenRevocation;
import com.edunex.backend.repository.TokenRevocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    private final TokenRevocationRepository repository;

    /** Every token issued so far for this account stops working. */
    @Transactional
    public void invalidate(String email) {
        String key = key(email);
        TokenRevocation r = repository.findById(key).orElseGet(() -> {
            TokenRevocation n = new TokenRevocation();
            n.setEmail(key);
            return n;
        });
        r.setValidAfter(Instant.now());
        repository.save(r);
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String email, Date issuedAt) {
        return repository.findById(key(email))
                .map(r -> issuedAt == null
                        || issuedAt.toInstant().isBefore(r.getValidAfter().truncatedTo(ChronoUnit.SECONDS)))
                .orElse(false);
    }

    private static String key(String email) {
        return email.toLowerCase(Locale.ROOT);
    }
}