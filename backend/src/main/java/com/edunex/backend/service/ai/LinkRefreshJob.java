package com.edunex.backend.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Re-reads the web links of the institute guide so the assistant never answers from an old copy. */
@Slf4j
@Component
@RequiredArgsConstructor
public class LinkRefreshJob {

    private final InstituteKnowledgeService knowledge;

    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "PT1H")
    public void run() {
        try {
            for (Long id : knowledge.dueLinks()) {
                try {
                    knowledge.reindex(id);
                } catch (Exception e) {
                    log.warn("Link {} could not be refreshed: {}", id, e.toString());
                }
            }
        } catch (Exception e) {
            log.warn("Link refresh failed: {}", e.toString());
        }
    }
}