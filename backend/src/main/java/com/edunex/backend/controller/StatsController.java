package com.edunex.backend.controller;

import com.edunex.backend.dto.PlatformStats;
import com.edunex.backend.dto.UniversityStats;
import com.edunex.backend.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/api/admin/stats")
    @PreAuthorize("hasRole('ADMIN_AGENT') and hasAuthority('STATS_VIEW_UNIVERSITY')")
    public UniversityStats university() {
        return statsService.university();
    }

    @GetMapping("/api/platform/stats")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public PlatformStats platform() {
        return statsService.platform();
    }
}