package com.edunex.backend.service;

import com.edunex.backend.entity.User;
import com.edunex.backend.exception.ConflictException;
import com.edunex.backend.exception.MailUnavailableException;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** New accounts: a random temporary password is generated, e-mailed to the person, and must be changed at the first login. */
@Service
@RequiredArgsConstructor
public class AccountCredentialsService {

    private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordEncoder passwordEncoder;
    private final MailOutboxService mail;
    private final UserRepository userRepository;

    @Value("${app.public-url:http://localhost:3000}")
    private String publicUrl;

    @Value("${app.accounts.temporary-password-days:7}")
    private int days;

    @Transactional
    public void issue(User user) {
        if (!mail.isConfigured()) {
            throw new MailUnavailableException("E-mail service is not configured: the credentials cannot be sent");
        }
        if (user.getCin() != null && userRepository.existsByCin(user.getCin())) {
            throw new ConflictException("This CIN is already used by another account");
        }
        String temporary = generate();
        user.setPassword(passwordEncoder.encode(temporary));
        user.setMustChangePassword(true);
        user.setTemporaryPasswordExpiresAt(Instant.now().plus(days, ChronoUnit.DAYS));

        String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
        mail.enqueueDirect(user.getEmail(), user.getFirstName() + " " + user.getLastName(),
                "[EduNex] Vos identifiants de connexion",
                MailTemplates.credentials(user.getFirstName(), user.getEmail(), temporary, base + "/login", days));
    }

    /** 14 characters: 4 digits and 10 letters in random order, without look-alike characters. */
    static String generate() {
        char[] chars = new char[14];
        for (int i = 0; i < 14; i++) {
            chars[i] = i < 4 ? DIGITS.charAt(RANDOM.nextInt(DIGITS.length())) : LETTERS.charAt(RANDOM.nextInt(LETTERS.length()));
        }
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }
}