package com.edunex.backend.controller;

import com.edunex.backend.dto.InternshipDto;
import com.edunex.backend.service.InternshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read access: the agents, the student, the supervisor and the jury (checked in the service). */
@RestController
@RequiredArgsConstructor
public class InternshipViewController {

    private final InternshipService service;

    @GetMapping("/api/internships/mine")
    public List<InternshipDto.Row> mineInternships(Authentication auth) {
        return service.mineInternships(auth.getName());
    }

    @GetMapping("/api/defenses/mine")
    public List<InternshipDto.DefenseRow> mineDefenses(Authentication auth) {
        return service.mineDefenses(auth.getName());
    }

    @GetMapping("/api/defenses/{id}")
    public InternshipDto.DefenseDetail detail(@PathVariable Long id, Authentication auth) {
        return service.detail(auth.getName(), manager(auth), id);
    }

    @GetMapping("/api/defenses/{id}/report")
    public ResponseEntity<Resource> report(@PathVariable Long id, Authentication auth) {
        return file(service.download(auth.getName(), manager(auth), id, "report"));
    }

    @GetMapping("/api/defenses/{id}/record")
    public ResponseEntity<Resource> record(@PathVariable Long id, Authentication auth) {
        return file(service.download(auth.getName(), manager(auth), id, "record"));
    }

    private static boolean manager(Authentication auth) {
        boolean permission = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("INTERNSHIPS_MANAGE"));
        boolean agent = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_AGENT"));
        return permission && agent;
    }

    private static ResponseEntity<Resource> file(InternshipService.Download d) {
        MediaType type = MediaTypeFactory.getMediaType(d.filename()).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(d.filename()).build().toString())
                .body(d.resource());
    }
}