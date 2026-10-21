package com.edunex.backend.controller;

import com.edunex.backend.dto.ImportDto;
import com.edunex.backend.service.ai.TimetableImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/timetable-imports")
@PreAuthorize("hasRole('ADMIN_AGENT') and hasAuthority('TIMETABLE_MANAGE')")
@RequiredArgsConstructor
public class AdminTimetableImportController {

    private final TimetableImportService importService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportDto.JobView> start(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(value = "timetableId", required = false) Long timetableId,
                                                   @RequestParam(value = "classId", required = false) Long classId,
                                                   Authentication auth) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(importService.start(auth.getName(), file, timetableId, classId));
    }

    @GetMapping("/{id}")
    public ImportDto.JobView get(@PathVariable String id, Authentication auth) {
        return importService.get(auth.getName(), id);
    }

    @PostMapping("/{id}/confirm")
    public ImportDto.ConfirmResponse confirm(@PathVariable String id, @RequestBody ImportDto.ConfirmRequest request,
                                             Authentication auth) {
        return importService.confirm(auth.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> discard(@PathVariable String id, Authentication auth) {
        importService.discard(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }
}