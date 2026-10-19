package com.edunex.backend.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/** Indexes new course documents in the background so the first question is not slow. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentIndexSweeper {

    private final AuthorizedMaterialService materials;
    private final CourseKnowledgeService knowledge;

    @Scheduled(initialDelayString = "PT30S", fixedDelayString = "PT5M")
    public void sweep() {
        try {
            List<AuthorizedMaterialService.DocRef> pending = materials.unindexedDocuments();
            if (!pending.isEmpty()) {
                log.info("Indexing {} new course document(s)", pending.size());
                knowledge.ensureIndexed(pending, Duration.ofMinutes(3));
            }
        } catch (Exception e) {
            log.warn("Document sweep failed: {}", e.toString());
        }
    }
}