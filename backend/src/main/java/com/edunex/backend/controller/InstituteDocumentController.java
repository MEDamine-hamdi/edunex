package com.edunex.backend.controller;

import com.edunex.backend.service.ai.InstituteKnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The institute guide is official information: any signed-in user may read or download it. */
@RestController
@RequestMapping("/api/institute/documents")
@RequiredArgsConstructor
public class InstituteDocumentController {

    private final InstituteKnowledgeService knowledgeService;

    @GetMapping("/{id}/info")
    public InstituteKnowledgeService.SourceInfo info(@PathVariable Long id) {
        return knowledgeService.info(id);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        InstituteKnowledgeService.Download file = knowledgeService.download(id);
        MediaType type = MediaTypeFactory.getMediaType(file.filename()).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.filename()).build().toString())
                .body(file.resource());
    }
}