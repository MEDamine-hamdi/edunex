package com.edunex.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailSenderJob {

    private final MailOutboxService outbox;
    private final BrevoClient brevo;

    @Scheduled(initialDelayString = "PT20S", fixedDelayString = "PT10S")
    public void run() {
        if (!brevo.isConfigured()) {
            return;
        }
        try {
            for (MailOutboxService.Due mail : outbox.claimDue()) {
                try {
                    brevo.send(mail);
                    outbox.markSent(mail.id());
                } catch (BrevoClient.PermanentFailure e) {
                    log.warn("E-mail {} rejected by Brevo: {}", mail.id(), e.getMessage());
                    outbox.markFailed(mail.id(), e.getMessage(), true);
                } catch (RuntimeException e) {
                    outbox.markFailed(mail.id(), e.getMessage(), false);
                }
            }
        } catch (Exception e) {
            log.warn("Mail job failed: {}", e.toString());
        }
    }
}