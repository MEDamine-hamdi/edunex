package com.edunex.backend.service.ai;

import com.edunex.backend.dto.InstituteDocumentResponse;
import com.edunex.backend.entity.InstituteChunk;
import com.edunex.backend.entity.InstituteDocument;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.InstituteChunkRepository;
import com.edunex.backend.repository.InstituteDocumentRepository;
import com.edunex.backend.repository.UserRepository;
import com.edunex.backend.service.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The institute guide: files and web links added by the administration, used by the general assistant. */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstituteKnowledgeService {

    public record Download(Resource resource, String filename) {
    }

    public record SourceInfo(String kind, String url, String title) {
    }

    private static final int TOP_K = 8;

    private final InstituteDocumentRepository documentRepository;
    private final InstituteChunkRepository chunkRepository;
    private final DocumentTextExtractor extractor;
    private final LinkFetcher linkFetcher;
    private final FileStorage fileStorage;
    private final UserRepository userRepository;

    @Value("${app.links.refresh-hours:24}")
    private int refreshHours;

    @Transactional
    public InstituteDocumentResponse upload(String email, String title, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        String original = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = dot < 0 ? "" : original.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!DocumentTextExtractor.isSupported(ext)) {
            throw new BadRequestException("Unsupported file type. Use PDF, DOCX, PPTX, XLSX, ODT, ODP, TXT or MD");
        }
        InstituteDocument doc = new InstituteDocument();
        doc.setSourceKind("FILE");
        doc.setTitle(title != null && !title.isBlank() ? title.trim() : (dot <= 0 ? original : original.substring(0, dot)));
        doc.setFileKey(fileStorage.store(file));
        doc.setType(ext.toUpperCase(Locale.ROOT));
        doc.setSize(file.getSize());
        doc.setUploadedBy(uploader(email));
        doc = documentRepository.save(doc);
        index(doc);
        return toResponse(doc);
    }

    @Transactional
    public InstituteDocumentResponse addLink(String email, String title, String url) {
        try {
            LinkFetcher.validate(url);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
        InstituteDocument doc = new InstituteDocument();
        doc.setSourceKind("LINK");
        doc.setUrl(url.trim());
        doc.setType("LIEN");
        doc.setTitle(title != null && !title.isBlank() ? title.trim() : url.trim());
        doc.setUploadedBy(uploader(email));
        doc = documentRepository.save(doc);
        index(doc);
        return toResponse(doc);
    }

    @Transactional
    public InstituteDocumentResponse reindex(Long id) {
        InstituteDocument doc = find(id);
        index(doc);
        return toResponse(doc);
    }

    @Transactional
    public void delete(Long id) {
        InstituteDocument doc = find(id);
        chunkRepository.deleteByDocument(doc.getId());
        documentRepository.delete(doc);
        if (doc.getFileKey() != null) {
            fileStorage.delete(doc.getFileKey());
        }
    }

    @Transactional(readOnly = true)
    public List<InstituteDocumentResponse> list() {
        return documentRepository.findAllByOrderByUploadedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public boolean hasIndexedGuide() {
        return documentRepository.countByStatus("INDEXED") > 0;
    }

    @Transactional(readOnly = true)
    public Download download(Long id) {
        InstituteDocument doc = find(id);
        if (doc.isLink() || doc.getFileKey() == null) {
            throw new BadRequestException("This source is a link, not a file");
        }
        return new Download(fileStorage.load(doc.getFileKey()), Paths.get(doc.getFileKey()).getFileName().toString());
    }

    @Transactional(readOnly = true)
    public SourceInfo info(Long id) {
        InstituteDocument doc = find(id);
        return new SourceInfo(doc.isLink() ? "LINK" : "FILE", doc.getUrl(), doc.getTitle());
    }

    /** Ids of the links that were not fetched for a while. */
    @Transactional(readOnly = true)
    public List<Long> dueLinks() {
        LocalDateTime limit = LocalDateTime.now().minusHours(refreshHours);
        return documentRepository.findBySourceKind("LINK").stream()
                .filter(d -> d.getLastFetchedAt() == null || d.getLastFetchedAt().isBefore(limit))
                .map(InstituteDocument::getId)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Passage> retrieve(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        Map<Long, InstituteDocument> docs = new HashMap<>();
        documentRepository.findByStatus("INDEXED").forEach(d -> docs.put(d.getId(), d));
        if (docs.isEmpty()) {
            return List.of();
        }
        List<PassageSelector.Candidate> candidates = new ArrayList<>();
        for (InstituteChunk c : chunkRepository.findByDocumentIdIn(docs.keySet())) {
            InstituteDocument d = docs.get(c.getDocumentId());
            candidates.add(new PassageSelector.Candidate(d.getId(), d.getTitle(), "", "", "INSTITUTE",
                    d.getTitle() + " " + c.getContent(), c.getContent()));
        }
        return PassageSelector.select(candidates, query, TOP_K, TOP_K);
    }

    // ---------- helpers ----------

    private void index(InstituteDocument doc) {
        chunkRepository.deleteByDocument(doc.getId());
        int count = 0;
        String status;
        String error = null;
        try {
            String text;
            if (doc.isLink()) {
                LinkFetcher.Fetched fetched = linkFetcher.fetch(doc.getUrl());
                text = fetched.text();
                if ((doc.getTitle() == null || doc.getTitle().equals(doc.getUrl())) && !fetched.title().isBlank()) {
                    doc.setTitle(fetched.title().length() > 190 ? fetched.title().substring(0, 190) : fetched.title());
                }
                doc.setLastFetchedAt(LocalDateTime.now());
            } else {
                text = extractor.extract(doc.getFileKey());
            }
            List<String> chunks = TextChunker.chunk(text, 1200, 150);
            List<InstituteChunk> rows = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                InstituteChunk row = new InstituteChunk();
                row.setDocumentId(doc.getId());
                row.setChunkIndex(i);
                row.setContent(chunks.get(i));
                rows.add(row);
            }
            chunkRepository.saveAll(rows);
            count = rows.size();
            status = count == 0 ? "EMPTY" : "INDEXED";
        } catch (DocumentTextExtractor.UnsupportedFormatException e) {
            status = "UNSUPPORTED";
        } catch (Exception e) {
            status = "FAILED";
            error = e.getMessage() == null ? e.toString() : e.getMessage();
            if (doc.isLink()) {
                doc.setLastFetchedAt(LocalDateTime.now());
            }
            log.warn("Could not index institute source {}: {}", doc.getId(), error);
        }
        doc.setStatus(status);
        doc.setChunkCount(count);
        doc.setLastError(error == null ? null : error.substring(0, Math.min(error.length(), 290)));
        documentRepository.save(doc);
    }

    private String uploader(String email) {
        return userRepository.findByEmail(email).map(u -> u.getFirstName() + " " + u.getLastName()).orElse(null);
    }

    private InstituteDocument find(Long id) {
        return documentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Document not found"));
    }

    private InstituteDocumentResponse toResponse(InstituteDocument d) {
        return new InstituteDocumentResponse(d.getId(), d.getTitle(), d.getType(), d.getSize(), d.getStatus(),
                d.getChunkCount(), d.getUploadedAt(), d.getUploadedBy(), d.isLink() ? "LINK" : "FILE",
                d.getUrl(), d.getLastFetchedAt(), d.getLastError());
    }
}