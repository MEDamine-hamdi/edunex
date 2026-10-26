package com.edunex.backend.service;

import com.edunex.backend.entity.User;
import com.edunex.backend.exception.MailUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Generates a temporary password for a new account and e-mails it through the outbox. */
@Component
@RequiredArgsConstructor
public class AccountCredentialsService {

    private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordEncoder passwordEncoder;
    private final MailOutboxService mail;

    @Value("${app.public-url:http://localhost:3000}")
    private String publicUrl;

    /** Call before saving the user, inside the same transaction. */
    public void issue(User user) {
        if (!mail.isConfigured()) {
            // rolls the creation back: otherwise nobody would know the password
            throw new MailUnavailableException("E-mail is not configured: credentials cannot be sent");
        }
        String temp = generate();
        user.setPassword(passwordEncoder.encode(temp));
        user.setMustChangePassword(true);

        String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
        String intro = "Votre compte EduNex a \u00e9t\u00e9 cr\u00e9\u00e9. Identifiant : " + user.getEmail()
                + " - Mot de passe provisoire : " + temp
                + ". Vous devrez le changer \u00e0 votre premi\u00e8re connexion.";
        mail.enqueueDirect(user.getEmail(), user.getFirstName() + " " + user.getLastName(),
                "[EduNex] Vos identifiants de connexion",
                MailTemplates.action(user.getFirstName(), "Bienvenue sur EduNex", intro,
                        "Se connecter", base + "/login",
                        "Si vous n'\u00eates pas concern\u00e9 par ce message, ignorez-le."));
    }

    private static String generate() {
        String all = LETTERS + DIGITS;
        List<Character> chars = new ArrayList<>();
        chars.add(LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
        chars.add(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        for (int i = 0; i < 10; i++) {
            chars.add(all.charAt(RANDOM.nextInt(all.length())));
        }
        Collections.shuffle(chars, RANDOM);
        StringBuilder sb = new StringBuilder();
        chars.forEach(sb::append);
        return sb.toString();
    }
}