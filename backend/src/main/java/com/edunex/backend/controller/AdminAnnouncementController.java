package com.edunex.backend.controller;

import com.edunex.backend.dto.AnnouncementRequest;
import com.edunex.backend.dto.AnnouncementResponse;
import com.edunex.backend.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    public List<AnnouncementResponse> list() {
        return announcementService.listAll();
    }
}