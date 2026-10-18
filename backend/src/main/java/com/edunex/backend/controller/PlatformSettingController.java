package com.edunex.backend.controller;

import com.edunex.backend.dto.SettingRequest;
import com.edunex.backend.dto.SettingResponse;
import com.edunex.backend.service.PlatformSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platform/settings")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class PlatformSettingController {

    private final PlatformSettingService settingService;

    @GetMapping
    public List<SettingResponse> list() {
        return settingService.list();
    }

    @PutMapping("/{key}")
    public SettingResponse upsert(@PathVariable String key, @Valid @RequestBody SettingRequest request) {
        return settingService.upsert(key, request.value());
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        settingService.delete(key);
        return ResponseEntity.noContent().build();
    }
}