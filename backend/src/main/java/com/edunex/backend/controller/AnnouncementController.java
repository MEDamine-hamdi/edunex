package com.edunex.backend.controller;

import com.edunex.backend.dto.AnnouncementResponse;
import com.edunex.backend.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    public List<AnnouncementResponse> visible(Authentication auth) {
        return announcementService.visibleTo(auth.getName());
    }

    @GetMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<Resource> attachment(@PathVariable Long id, @PathVariable Long attachmentId, Authentication auth) {
        AnnouncementService.Download d = announcementService.download(auth.getName(), id, attachmentId);
        MediaType type = MediaTypeFactory.getMediaType(d.filename()).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(d.filename()).build().toString())
                .body(d.resource());
    }
}