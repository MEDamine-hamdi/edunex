package com.edunex.backend.service;

import com.edunex.backend.dto.MailDto;
import com.edunex.backend.entity.EmailPreference;
import com.edunex.backend.entity.MailOutbox;
import com.edunex.backend.entity.Notification;
import com.edunex.backend.entity.User;
import com.edunex.backend.enums.NotificationType;
import com.edunex.backend.repository.EmailPreferenceRepository;
import com.edunex.backend.repository.MailOutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Outbox: notifications become e-mails here, and a background job sends them with retries. */
@Service
public class MailOutboxService {

    public record Due(Long id, String to, String name, String subject, String html) {
    }

    private static final int[] BACKOFF_MINUTES = {1, 5, 15, 60};
    private static final int MAX_ATTEMPTS = 5;

    private final MailOutboxRepository outbox;
    private final EmailPreferenceRepository preferences;
    private final String apiKey;
    private final String senderEmail;
    private final String publicUrl;

    public MailOutboxService(MailOutboxRepository outbox, EmailPreferenceRepository preferences,
                             @Value("${app.mail.brevo-api-key:}") String apiKey,
                             @Value("${app.mail.sender-email:}") String senderEmail,
                             @Value("${app.public-url:http://localhost:3000}") String publicUrl) {
        this.outbox = outbox;
        this.preferences = preferences;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.senderEmail = senderEmail == null ? "" : senderEmail.trim();
        this.publicUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
    }

    public boolean isConfigured() {
        return !apiKey.isBlank() && !senderEmail.isBlank();
    }

    @Transactional
    public void enqueueNotifications(Collection<Notification> notifications) {
        if (!isConfigured() || notifications.isEmpty()) {
            return;
        }
        Set<Long> ids = notifications.stream().map(n -> n.getUser().getId()).collect(Collectors.toSet());
        Set<Long> optedOut = preferences.findAllById(ids).stream()
                .filter(p -> !p.isEnabled())
                .map(EmailPreference::getUserId)
                .collect(Collectors.toSet());
        List<MailOutbox> rows = new ArrayList<>();
        for (Notification n : notifications) {
            User u = n.getUser();
            if (optedOut.contains(u.getId()) || u.getEmail() == null || u.getEmail().isBlank()) {
                continue;
            }
            String title = titleFor(n.getType());
            rows.add(row(u.getEmail(), u.getFirstName() + " " + u.getLastName(), "[EduNex] " + title,
                    MailTemplates.notification(u.getFirstName(), title, n.getMessage(), publicUrl + pathFor(u, n.getType()))));
        }
        outbox.saveAll(rows);
    }

    @Transactional
    public void enqueueDirect(String to, String name, String subject, String html) {
        outbox.save(row(to, name, subject, html));
    }

    @Transactional(readOnly = true)
    public List<Due> claimDue() {
        return outbox.findTop20ByStatusAndNextAttemptAtLessThanEqualOrderByIdAsc("PENDING", LocalDateTime.now()).stream()
                .map(m -> new Due(m.getId(), m.getToEmail(), m.getToName(), m.getSubject(), m.getHtml()))
                .toList();
    }

    @Transactional
    public void markSent(Long id) {
        outbox.findById(id).ifPresent(m -> {
            m.setStatus("SENT");
            m.setSentAt(LocalDateTime.now());
            m.setLastError(null);
        });
    }

    @Transactional
    public void markFailed(Long id, String error, boolean permanent) {
        outbox.findById(id).ifPresent(m -> {
            m.setAttempts(m.getAttempts() + 1);
            m.setLastError(error == null ? null : error.substring(0, Math.min(error.length(), 480)));
            if (permanent || m.getAttempts() >= MAX_ATTEMPTS) {
                m.setStatus("FAILED");
            } else {
                m.setNextAttemptAt(LocalDateTime.now()
                        .plusMinutes(BACKOFF_MINUTES[Math.min(m.getAttempts() - 1, BACKOFF_MINUTES.length - 1)]));
            }
        });
    }

    @Transactional(readOnly = true)
    public MailDto.Status status() {
        List<MailDto.Failure> failures = outbox.findTop8ByStatusOrderByIdDesc("FAILED").stream()
                .map(m -> new MailDto.Failure(m.getToEmail(), m.getSubject(), m.getLastError(), m.getCreatedAt()))
                .toList();
        return new MailDto.Status(isConfigured(), senderEmail, outbox.countByStatus("PENDING"),
                outbox.countByStatus("SENT"), outbox.countByStatus("FAILED"), failures);
    }

    private MailOutbox row(String to, String name, String subject, String html) {
        MailOutbox m = new MailOutbox();
        m.setToEmail(to);
        m.setToName(name);
        m.setSubject(subject.length() > 240 ? subject.substring(0, 240) : subject);
        m.setHtml(html);
        return m;
    }

    private static String titleFor(NotificationType type) {
        return switch (type) {
            case NEW_COURSE -> "Nouveau cours";
            case TIMETABLE_CHANGE -> "Emploi du temps";
            case NEW_GRADE -> "Nouvelle note";
            case ATTENDANCE_UPDATE -> "Présence";
            case ANNOUNCEMENT -> "Nouvelle annonce";
            case DOCUMENT_REQUEST -> "Demande de document";
        };
    }

    private static String pathFor(User u, NotificationType type) {
        String role = u.getRole() == null ? "" : u.getRole().getName();
        return switch (role) {
            case "ROLE_STUDENT" -> switch (type) {
                case NEW_COURSE -> "/student/courses";
                case TIMETABLE_CHANGE -> "/student/timetable";
                case NEW_GRADE -> "/student/grades";
                case ATTENDANCE_UPDATE -> "/student/attendance";
                case DOCUMENT_REQUEST -> "/student/requests";
                default -> "/student/notifications";
            };
            case "ROLE_PROFESSOR" -> switch (type) {
                case TIMETABLE_CHANGE -> "/professor/timetable";
                case DOCUMENT_REQUEST -> "/professor/requests";
                default -> "/professor/notifications";
            };
            case "ROLE_ADMIN_AGENT" -> type == NotificationType.DOCUMENT_REQUEST ? "/admin/requests" : "/admin/notifications";
            default -> "/login";
        };
    }
}