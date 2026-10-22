package com.edunex.backend.controller;

import com.edunex.backend.dto.TypeDto;
import com.edunex.backend.service.DocumentRequestService;
import com.edunex.backend.service.DocumentTypeService;
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
public class DocumentTypeController {

    private static final String ADMIN = "hasRole('ADMIN_AGENT') and hasAuthority('REQUESTS_MANAGE')";

    private final DocumentTypeService typeService;
    private final DocumentRequestService requestService;

    /** The active types the signed-in student or professor may request. */
    @GetMapping("/api/requests/type-options")
    public List<TypeDto.Option> options(Authentication auth) {
        return requestService.options(auth.getName());
    }

    @GetMapping("/api/admin/request-types")
    @PreAuthorize(ADMIN)
    public List<TypeDto.View> list() {
        return typeService.list();
    }

    @PostMapping("/api/admin/request-types")
    @PreAuthorize(ADMIN)
    public ResponseEntity<TypeDto.View> create(@Valid @RequestBody TypeDto.Save request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(typeService.create(request));
    }

    @PutMapping("/api/admin/request-types/{id}")
    @PreAuthorize(ADMIN)
    public TypeDto.View update(@PathVariable Long id, @Valid @RequestBody TypeDto.Save request) {
        return typeService.update(id, request);
    }

    @DeleteMapping("/api/admin/request-types/{id}")
    @PreAuthorize(ADMIN)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        typeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}