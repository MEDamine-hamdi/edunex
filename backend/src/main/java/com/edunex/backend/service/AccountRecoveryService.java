package com.edunex.backend.service;

import com.edunex.backend.entity.AccountToken;
import com.edunex.backend.entity.User;
import com.edunex.backend.enums.UserStatus;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.MailUnavailableException;
import com.edunex.backend.exception.TooManyRequestsException;
import com.edunex.backend.repository.AccountTokenRepository;
import com.edunex.backend.repository.UserRepository;
import com.edunex.backend.security.LoginThrottleService;
import com.edunex.backend.security.TokenRevocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

/** Forgotten password and e-mail change. The links are random, single use, and expire. */
@Service
@RequiredArgsConstructor
public class AccountRecoveryService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String RESET = "PASSWORD_RESET";
    private static final String CHANGE = "EMAIL_CHANGE";

    private final UserRepository userRepository;
    private final AccountTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final MailOutboxService mail;
    private final TokenRevocationService revocation;
    private final LoginThrottleService throttle;

    @Value("${app.public-url:http://localhost:3000}")
    private String publicUrl;

    // ---------- forgotten password ----------

    /** Always looks the same from outside: nothing tells whether the address has an account. */
    @Transactional
    public void requestPasswordReset(String rawEmail) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        if (throttle.consume("forgot:all", 300, 60) > 0 || throttle.consume("forgot:" + email, 3, 60) > 0) {
            return;
        }
        User u = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (u == null || u.getStatus() != UserStatus.ACTIVE || !mail.isConfigured()) {
            return;
        }
        tokens.deleteUnused(u.getId(), RESET);
        String raw = save(u.getId(), RESET, null, Duration.ofMinutes(30));
        mail.enqueueDirect(u.getEmail(), name(u), "[EduNex] Réinitialisation de votre mot de passe",
                MailTemplates.action(u.getFirstName(), "Réinitialisation du mot de passe",
                        "Vous avez demandé à réinitialiser votre mot de passe EduNex. Ce lien est valable 30 minutes et ne peut servir qu'une seule fois.",
                        "Choisir un nouveau mot de passe", base() + "/reset-password?token=" + raw,
                        "Si vous n'êtes pas à l'origine de cette demande, ignorez cet e-mail : votre mot de passe ne change pas."));
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        AccountToken t = usable(rawToken, RESET);
        User u = userRepository.findById(t.getUserId())
                .filter(x -> x.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(AccountRecoveryService::invalid);
        if (tokens.markUsed(t.getId(), Instant.now()) != 1) {
            throw invalid();
        }
        u.setPassword(passwordEncoder.encode(newPassword));
        revocation.invalidate(u.getEmail());
        throttle.reset("login:" + u.getEmail().toLowerCase(Locale.ROOT));
        mail.enqueueDirect(u.getEmail(), name(u), "[EduNex] Votre mot de passe a été modifié",
                MailTemplates.action(u.getFirstName(), "Mot de passe modifié",
                        "Le mot de passe de votre compte EduNex vient d'être modifié.",
                        null, null,
                        "Si ce n'est pas vous, contactez immédiatement l'administration de votre établissement."));
    }

    // ---------- e-mail change ----------

    @Transactional
    public void requestEmailChange(String currentEmail, String password, String rawNewEmail) {
        if (!mail.isConfigured()) {
            throw new MailUnavailableException("E-mail service is not configured");
        }
        User u = userRepository.findByEmail(currentEmail).orElseThrow(() -> new AccessDeniedException("Unknown user"));
        long wait = throttle.consume("emailchange:" + u.getId(), 5, 60);
        if (wait > 0) {
            throw new TooManyRequestsException("Too many requests. Try again later.", wait);
        }
        if (!passwordEncoder.matches(password, u.getPassword())) {
            throw new BadRequestException("Incorrect password");
        }
        String newEmail = rawNewEmail.trim().toLowerCase(Locale.ROOT);
        if (newEmail.equalsIgnoreCase(u.getEmail())) {
            throw new BadRequestException("This is already your e-mail address");
        }
        if (userRepository.existsByEmailIgnoreCase(newEmail)) {
            return; // same answer as a success: no way to find out which addresses have an account
        }
        tokens.deleteUnused(u.getId(), CHANGE);
        String raw = save(u.getId(), CHANGE, newEmail, Duration.ofMinutes(60));
        mail.enqueueDirect(newEmail, name(u), "[EduNex] Confirmez votre nouvelle adresse e-mail",
                MailTemplates.action(u.getFirstName(), "Confirmez votre adresse e-mail",
                        "Vous avez demandé à utiliser cette adresse pour votre compte EduNex. Confirmez-la avec le bouton ci-dessous : le lien est valable 60 minutes.",
                        "Confirmer mon adresse", base() + "/confirm-email?token=" + raw,
                        "Si vous n'êtes pas à l'origine de cette demande, ignorez cet e-mail."));
        mail.enqueueDirect(u.getEmail(), name(u), "[EduNex] Demande de changement d'adresse e-mail",
                MailTemplates.action(u.getFirstName(), "Changement d'adresse e-mail demandé",
                        "Une demande de changement d'adresse e-mail a été faite sur votre compte. Votre adresse ne change qu'après confirmation depuis la nouvelle boîte mail.",
                        null, null,
                        "Si ce n'est pas vous, changez votre mot de passe et prévenez l'administration."));
    }

    @Transactional
    public void confirmEmailChange(String rawToken) {
        AccountToken t = usable(rawToken, CHANGE);
        User u = userRepository.findById(t.getUserId()).orElseThrow(AccountRecoveryService::invalid);
        String newEmail = t.getNewEmail();
        if (newEmail == null || userRepository.existsByEmailIgnoreCase(newEmail)) {
            throw invalid();
        }
        if (tokens.markUsed(t.getId(), Instant.now()) != 1) {
            throw invalid();
        }
        String old = u.getEmail();
        u.setEmail(newEmail);
        revocation.invalidate(old);
        revocation.invalidate(newEmail);
        mail.enqueueDirect(old, name(u), "[EduNex] Votre adresse e-mail a été modifiée",
                MailTemplates.action(u.getFirstName(), "Adresse e-mail modifiée",
                        "L'adresse e-mail de votre compte EduNex est maintenant " + newEmail + ".",
                        null, null,
                        "Si ce n'est pas vous, contactez immédiatement l'administration de votre établissement."));
    }

    @Scheduled(initialDelayString = "PT15M", fixedDelayString = "PT6H")
    @Transactional
    public void purge() {
        tokens.purgeExpired(Instant.now().minus(1, ChronoUnit.DAYS));
    }

    // ---------- helpers ----------

    private String save(Long userId, String type, String newEmail, Duration ttl) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        AccountToken t = new AccountToken();
        t.setUserId(userId);
        t.setType(type);
        t.setTokenHash(hash(raw));
        t.setNewEmail(newEmail);
        t.setExpiresAt(Instant.now().plus(ttl));
        tokens.save(t);
        return raw;
    }

    private AccountToken usable(String rawToken, String type) {
        AccountToken t = tokens.findByTokenHash(hash(rawToken)).orElseThrow(AccountRecoveryService::invalid);
        if (!type.equals(t.getType()) || t.getUsedAt() != null || t.getExpiresAt().isBefore(Instant.now())) {
            throw invalid();
        }
        return t;
    }

    private static BadRequestException invalid() {
        return new BadRequestException("This link is invalid or has expired");
    }

    private static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String base() {
        return publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
    }

    private static String name(User u) {
        return u.getFirstName() + " " + u.getLastName();
    }
}