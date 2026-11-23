package com.edunex.backend.controller;

import com.edunex.backend.dto.CertificateDto;
import com.edunex.backend.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentCertificateController {

    private final CertificateService service;

    @GetMapping("/certificates")
    public List<CertificateDto.Row> mine(Authentication auth) {
        return service.mine(auth.getName());
    }

    @GetMapping("/certificates/coverage")
    public CertificateDto.Coverage coverage(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                            Authentication auth) {
        return service.coverage(auth.getName(), from, to);
    }

    @PostMapping(value = "/certificates", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CertificateDto.Row> submit(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String note,
            @RequestParam(value = "examIds", required = false) List<Long> examIds,
            @RequestParam("file") MultipartFile file, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.submit(auth.getName(), startDate, endDate, note, examIds, file));
    }

    @GetMapping("/certificates/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable Long id, Authentication auth) {
        return respond(service.myFile(auth.getName(), id));
    }

    @DeleteMapping("/certificates/{id}")
    public ResponseEntity<Void> withdraw(@PathVariable Long id, Authentication auth) {
        service.withdraw(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/retakes")
    public List<CertificateDto.RetakeItem> retakes(Authentication auth) {
        return service.myRetakes(auth.getName());
    }

    static ResponseEntity<Resource> respond(CertificateService.FileData d) {
        MediaType type = MediaTypeFactory.getMediaType(d.filename()).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(d.filename()).build().toString())
                .body(d.resource());
    }
}