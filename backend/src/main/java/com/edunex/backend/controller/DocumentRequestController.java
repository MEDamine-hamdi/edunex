package com.edunex.backend.controller;

import com.edunex.backend.dto.RequestDto;
import com.edunex.backend.enums.RequestStatus;
import com.edunex.backend.service.DocumentRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DocumentRequestController {

    private static final String ADMIN = "hasRole('ADMIN_AGENT') and hasAuthority('REQUESTS_MANAGE')";

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

    @GetMapping("/api/requests/{id}/file")
    public ResponseEntity<Resource> downloadOwn(@PathVariable Long id, Authentication auth) {
        return file(service.downloadOwn(auth.getName(), id));
    }

    // administration
    @GetMapping("/api/admin/requests")
    @PreAuthorize(ADMIN)
    public List<RequestDto.View> list(@RequestParam(required = false) RequestStatus status) {
        return service.list(status);
    }

    @PatchMapping("/api/admin/requests/{id}")
    @PreAuthorize(ADMIN)
    public RequestDto.View update(@PathVariable Long id, @Valid @RequestBody RequestDto.Update request,
                                  Authentication auth) {
        return service.update(auth.getName(), id, request);
    }

    @PostMapping(value = "/api/admin/requests/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(ADMIN)
    public RequestDto.View attach(@PathVariable Long id, @RequestParam("file") MultipartFile file, Authentication auth) {
        return service.attachFile(auth.getName(), id, file);
    }

    @DeleteMapping("/api/admin/requests/{id}/file")
    @PreAuthorize(ADMIN)
    public RequestDto.View removeFile(@PathVariable Long id) {
        return service.removeFile(id);
    }

    @GetMapping("/api/admin/requests/{id}/file")
    @PreAuthorize(ADMIN)
    public ResponseEntity<Resource> downloadAsAgent(@PathVariable Long id) {
        return file(service.downloadAsAgent(id));
    }

    private static ResponseEntity<Resource> file(DocumentRequestService.Download d) {
        MediaType type = MediaTypeFactory.getMediaType(d.filename()).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(d.filename()).build().toString())
                .body(d.resource());
    }
}