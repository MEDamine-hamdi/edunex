package com.edunex.backend.service.ai;

import com.edunex.backend.entity.DocumentChunk;
import com.edunex.backend.entity.DocumentIndexEntry;
import com.edunex.backend.repository.DocumentChunkRepository;
import com.edunex.backend.repository.DocumentIndexRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIndexer {

    private static final Duration RETRY_FAILED_AFTER = Duration.ofHours(1);

    private final DocumentIndexRepository indexRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentTextExtractor extractor;

    public static boolean needsIndexing(DocumentIndexEntry entry) {
        return entry == null
                || ("FAILED".equals(entry.getStatus())
                && entry.getIndexedAt().isBefore(LocalDateTime.now().minus(RETRY_FAILED_AFTER)));
    }

    @Transactional
    public void index(AuthorizedMaterialService.DocRef doc) {
        DocumentIndexEntry existing = indexRepository.findById(doc.id()).orElse(null);
        if (!needsIndexing(existing)) {
            return;
        }
        chunkRepository.deleteByDocument(doc.id());

        String status;
        int count = 0;
        try {
            List<String> chunks = TextChunker.chunk(extractor.extract(doc.storageKey()), 1200, 150);
            List<DocumentChunk> rows = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunk row = new DocumentChunk();
                row.setDocumentId(doc.id());
                row.setChunkIndex(i);
                row.setContent(chunks.get(i));
                rows.add(row);
            }
            chunkRepository.saveAll(rows);
            count = rows.size();
            status = count == 0 ? "EMPTY" : "INDEXED";
        } catch (DocumentTextExtractor.UnsupportedFormatException e) {
            status = "UNSUPPORTED";
            log.info("Document {} not indexed: {}", doc.id(), e.getMessage());
        } catch (Exception e) {
            status = "FAILED";
            log.warn("Could not index document {}: {}", doc.id(), e.toString());
        }

        DocumentIndexEntry entry = existing != null ? existing : new DocumentIndexEntry();
        entry.setDocumentId(doc.id());
        entry.setStatus(status);
        entry.setChunkCount(count);
        entry.setIndexedAt(LocalDateTime.now());
        indexRepository.save(entry);
    }

    /** Removes chunks of documents that were deleted since. */
    @Transactional
    public void purgeOrphans() {
        chunkRepository.deleteOrphans();
        indexRepository.deleteOrphans();
    }
}