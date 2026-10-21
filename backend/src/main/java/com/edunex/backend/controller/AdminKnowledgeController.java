package com.edunex.backend.controller;

import com.edunex.backend.dto.InstituteDocumentResponse;
import com.edunex.backend.service.ai.InstituteKnowledgeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/knowledge")
@PreAuthorize("hasRole('ADMIN_AGENT') and hasAuthority('KNOWLEDGE_MANAGE')")
@RequiredArgsConstructor
public class AdminKnowledgeController {

    public record LinkRequest(@Size(max = 200) String title, @NotBlank @Size(max = 1000) String url) {
    }

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

    @PostMapping("/link")
    public ResponseEntity<InstituteDocumentResponse> addLink(@Valid @RequestBody LinkRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(knowledgeService.addLink(auth.getName(), request.title(), request.url()));
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