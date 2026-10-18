package com.edunex.backend.controller;

import com.edunex.backend.dto.SessionRequest;
import com.edunex.backend.dto.SessionResponse;
import com.edunex.backend.dto.TimetableRequest;
import com.edunex.backend.dto.TimetableResponse;
import com.edunex.backend.service.TimetableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN_AGENT') and hasAuthority('TIMETABLE_MANAGE')")
@RequiredArgsConstructor
public class AdminTimetableController {

    private final TimetableService timetableService;

    @PostMapping("/timetables")
    public ResponseEntity<TimetableResponse> create(@Valid @RequestBody TimetableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timetableService.create(request));
    }

    @GetMapping("/timetables")
    public List<TimetableResponse> list() {
        return timetableService.findAll();
    }

    @GetMapping("/timetables/{id}/sessions")
    public List<SessionResponse> sessions(@PathVariable Long id) {
        return timetableService.sessions(id);
    }

    @PostMapping("/timetables/{id}/sessions")
    public ResponseEntity<SessionResponse> addSession(@PathVariable Long id,
                                                      @Valid @RequestBody SessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timetableService.addSession(id, request));
    }

    @PutMapping("/sessions/{sessionId}")
    public SessionResponse updateSession(@PathVariable Long sessionId,
                                         @Valid @RequestBody SessionRequest request) {
        return timetableService.updateSession(sessionId, request);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long sessionId) {
        timetableService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
