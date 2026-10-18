package com.edunex.backend.controller;

import com.edunex.backend.dto.ActivityLogResponse;
import com.edunex.backend.dto.PageResponse;
import com.edunex.backend.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/logs")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class PlatformLogController {

    private final ActivityLogService activityLogService;

    @GetMapping
    public PageResponse<ActivityLogResponse> logs(@RequestParam(required = false) String q,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "25") int size) {
        return activityLogService.search(q, page, size);
    }
}