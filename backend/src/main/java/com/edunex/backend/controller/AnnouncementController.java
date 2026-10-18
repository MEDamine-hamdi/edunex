package com.edunex.backend.controller;

import com.edunex.backend.dto.AnnouncementResponse;
import com.edunex.backend.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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
}