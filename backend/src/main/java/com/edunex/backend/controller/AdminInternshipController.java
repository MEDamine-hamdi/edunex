package com.edunex.backend.controller;

import com.edunex.backend.dto.InternshipDto;
import com.edunex.backend.service.InternshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN_AGENT') and hasAuthority('INTERNSHIPS_MANAGE')")
@RequiredArgsConstructor
public class AdminInternshipController {

    private final InternshipService service;

    @GetMapping("/internships/lookups")
    public InternshipDto.Lookups lookups() {
        return service.lookups();
    }

    @GetMapping("/internships")
    public List<InternshipDto.Row> internships() {
        return service.internships();
    }

    @PostMapping("/internships")
    public ResponseEntity<InternshipDto.Row> create(@Valid @RequestBody InternshipDto.Save request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createInternship(request));
    }

    @PutMapping("/internships/{id}")
    public InternshipDto.Row update(@PathVariable Long id, @Valid @RequestBody InternshipDto.Save request) {
        return service.updateInternship(id, request);
    }

    @DeleteMapping("/internships/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteInternship(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/defenses")
    public List<InternshipDto.DefenseRow> defenses() {
        return service.allDefenses();
    }

    @PostMapping("/defenses")
    public ResponseEntity<InternshipDto.DefenseDetail> createDefense(@Valid @RequestBody InternshipDto.DefenseSave request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.saveDefense(null, request));
    }

    @PutMapping("/defenses/{id}")
    public InternshipDto.DefenseDetail updateDefense(@PathVariable Long id, @Valid @RequestBody InternshipDto.DefenseSave request) {
        return service.saveDefense(id, request);
    }

    @PutMapping("/defenses/{id}/result")
    public InternshipDto.DefenseDetail result(@PathVariable Long id, @Valid @RequestBody InternshipDto.ResultInput request) {
        return service.setResult(id, request);
    }

    @PostMapping(value = "/defenses/{id}/report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InternshipDto.DefenseDetail report(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return service.attach(id, "report", file);
    }

    @PostMapping(value = "/defenses/{id}/record", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InternshipDto.DefenseDetail record(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return service.attach(id, "record", file);
    }

    @DeleteMapping("/defenses/{id}/report")
    public InternshipDto.DefenseDetail removeReport(@PathVariable Long id) {
        return service.removeFile(id, "report");
    }

    @DeleteMapping("/defenses/{id}/record")
    public InternshipDto.DefenseDetail removeRecord(@PathVariable Long id) {
        return service.removeFile(id, "record");
    }
}