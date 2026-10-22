package com.edunex.backend.controller;

import com.edunex.backend.dto.AnnouncementRequest;
import com.edunex.backend.dto.AnnouncementResponse;
import com.edunex.backend.service.AnnouncementService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/admin/announcements")
@PreAuthorize("hasRole('ADMIN_AGENT') and hasAuthority('ANNOUNCEMENTS_PUBLISH')")
@RequiredArgsConstructor
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;

    @PostMapping
    public ResponseEntity<AnnouncementResponse> publish(@Valid @RequestBody AnnouncementRequest request,
                                                        Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(announcementService.publish(auth.getName(), request));
    }

    /** Parts: "data" (JSON of the announcement) and "files" (pictures or documents). */
    @PostMapping(value = "/with-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnnouncementResponse> publishWithFiles(
            @RequestPart("data") @Valid AnnouncementRequest data,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(announcementService.publish(auth.getName(), data, files));
    }

    @GetMapping
    public List<AnnouncementResponse> list() {
        return announcementService.listAll();
    }
}