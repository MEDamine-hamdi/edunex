package com.edunex.backend.controller;

import com.edunex.backend.dto.RequestDto;
import com.edunex.backend.enums.RequestStatus;
import com.edunex.backend.service.DocumentRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DocumentRequestController {

    private final DocumentRequestService service;

    // students and professors (the service refuses every other role)
    @GetMapping("/api/requests/types")
    public List<String> types(Authentication auth) {
        return service.types(auth.getName());
    }

    @GetMapping("/api/requests/mine")
    public List<RequestDto.View> mine(Authentication auth) {
        return service.mine(auth.getName());
    }

    @PostMapping("/api/requests")
    public ResponseEntity<RequestDto.View> create(@Valid @RequestBody RequestDto.Create request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(auth.getName(), request));
    }

    @DeleteMapping("/api/requests/{id}")
    public ResponseEntity<Void> withdraw(@PathVariable Long id, Authentication auth) {
        service.withdraw(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    // administration
    @GetMapping("/api/admin/requests")
    @PreAuthorize("hasRole('ADMIN_AGENT') and hasAuthority('REQUESTS_MANAGE')")
    public List<RequestDto.View> list(@RequestParam(required = false) RequestStatus status) {
        return service.list(status);
    }

    @PatchMapping("/api/admin/requests/{id}")
    @PreAuthorize("hasRole('ADMIN_AGENT') and hasAuthority('REQUESTS_MANAGE')")
    public RequestDto.View update(@PathVariable Long id, @Valid @RequestBody RequestDto.Update request,
                                  Authentication auth) {
        return service.update(auth.getName(), id, request);
    }
}