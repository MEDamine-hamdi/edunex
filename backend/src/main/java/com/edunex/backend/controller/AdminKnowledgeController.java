package com.edunex.backend.controller;

import com.edunex.backend.dto.InstituteDocumentResponse;
import com.edunex.backend.service.ai.InstituteKnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Management of the institute guide used by the general assistant. */
@RestController
@RequestMapping("/api/admin/knowledge")
@PreAuthorize("hasRole('ADMIN_AGENT') and hasAuthority('KNOWLEDGE_MANAGE')")
@RequiredArgsConstructor
public class AdminKnowledgeController {

    private final InstituteKnowledgeService knowledgeService;

    @GetMapping
    public List<InstituteDocumentResponse> list() {
        return knowledgeService.list();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InstituteDocumentResponse> upload(@RequestParam("file") MultipartFile file,
                                                            @RequestParam(value = "title", required = false) String title,
                                                            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(knowledgeService.upload(auth.getName(), title, file));
    }

    @PostMapping("/{id}/reindex")
    public InstituteDocumentResponse reindex(@PathVariable Long id) {
        return knowledgeService.reindex(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}