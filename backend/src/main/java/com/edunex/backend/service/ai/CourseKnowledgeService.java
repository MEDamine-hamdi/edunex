package com.edunex.backend.service.ai;

import com.edunex.backend.entity.DocumentChunk;
import com.edunex.backend.entity.DocumentIndexEntry;
import com.edunex.backend.repository.DocumentChunkRepository;
import com.edunex.backend.repository.DocumentIndexRepository;
import com.edunex.backend.service.ai.AuthorizedMaterialService.DocRef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Indexes course documents and returns the passages that best answer a question. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseKnowledgeService {

    private static final Object INDEX_LOCK = new Object();
    private static final int TOP_K = 6;
    private static final int MAX_PER_DOCUMENT = 3;
    private static final int MAX_SCORED_CHUNKS = 20_000;

    private final DocumentIndexRepository indexRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentIndexer indexer;

    public void ensureIndexed(Collection<DocRef> documents, Duration budget) {
        if (documents.isEmpty()) {
            return;
        }
        Map<Long, DocumentIndexEntry> known = indexRepository
                .findAllById(documents.stream().map(DocRef::id).toList()).stream()
                .collect(Collectors.toMap(DocumentIndexEntry::getDocumentId, Function.identity()));
        long deadline = System.nanoTime() + budget.toNanos();
        boolean indexedSomething = false;
        for (DocRef doc : documents) {
            if (!DocumentIndexer.needsIndexing(known.get(doc.id()))) {
                continue;
            }
            if (System.nanoTime() > deadline) {
                log.info("Indexing time budget used up, the rest will follow later");
                break;
            }
            synchronized (INDEX_LOCK) {
                indexer.index(doc);
            }
            indexedSomething = true;
        }
        if (indexedSomething) {
            indexer.purgeOrphans();
        }
    }

    /** Only chunks of the given (already authorized) documents are ever loaded. */
    public List<Passage> retrieve(List<DocRef> authorized, String query) {
        if (authorized.isEmpty() || query.isBlank()) {
            return List.of();
        }
        ensureIndexed(authorized, Duration.ofSeconds(15));

        Map<Long, DocRef> byId = new HashMap<>();
        authorized.forEach(d -> byId.put(d.id(), d));
        List<DocumentChunk> chunks = chunkRepository.findByDocumentIdIn(byId.keySet());
        if (chunks.size() > MAX_SCORED_CHUNKS) {
            log.warn("{} chunks to score, keeping the first {}", chunks.size(), MAX_SCORED_CHUNKS);
            chunks = chunks.subList(0, MAX_SCORED_CHUNKS);
        }
        List<PassageSelector.Candidate> candidates = new ArrayList<>(chunks.size());
        for (DocumentChunk c : chunks) {
            DocRef d = byId.get(c.getDocumentId());
            candidates.add(new PassageSelector.Candidate(d.id(), d.title(), d.courseTitle(), d.subjectName(), "COURSE",
                    d.title() + " " + d.courseTitle() + " " + d.subjectName() + " " + c.getContent(), c.getContent()));
        }
        return PassageSelector.select(candidates, query, TOP_K, MAX_PER_DOCUMENT);
    }
}