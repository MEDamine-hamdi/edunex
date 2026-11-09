package com.edunex.backend.controller;

import com.edunex.backend.service.PersonDetailsService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("(hasRole('ADMIN_AGENT') and hasAuthority('ACCOUNTS_MANAGE')) or hasRole('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class AdminPersonDetailsController {

    public record CinRequest(@NotBlank String cin) {
    }

    public record TypeRequest(@NotBlank String type) {
    }

    private final PersonDetailsService service;

    @PutMapping("/users/{id}/cin")
    public ResponseEntity<Void> cin(@PathVariable Long id, @RequestBody CinRequest request) {
        service.setCin(id, request.cin());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/professors/{id}/type")
    public ResponseEntity<Void> type(@PathVariable Long id, @RequestBody TypeRequest request) {
        service.setProfessorType(id, request.type());
        return ResponseEntity.noContent().build();
    }
}