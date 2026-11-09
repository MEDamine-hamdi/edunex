package com.edunex.backend.controller;

import com.edunex.backend.dto.ExamDto;
import com.edunex.backend.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/professor/exams")
@PreAuthorize("hasRole('PROFESSOR')")
@RequiredArgsConstructor
public class ExamProfessorController {

    private final ExamService service;

    @GetMapping("/kind")
    public ExamDto.MyKind kind(Authentication auth) {
        return new ExamDto.MyKind(service.myKind(auth.getName()));
    }

    @GetMapping("/available")
    public List<ExamDto.AvailableExam> available(Authentication auth) {
        return service.available(auth.getName());
    }

    @GetMapping("/mine")
    public List<ExamDto.MyExam> mine(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                     Authentication auth) {
        return service.mine(auth.getName(), from == null ? LocalDate.now().minusDays(120) : from, to == null ? LocalDate.now().plusDays(365) : to);
    }

    @PostMapping("/{id}/volunteer")
    public ExamDto.ExamItem volunteer(@PathVariable Long id, Authentication auth) {
        return service.volunteer(auth.getName(), id);
    }

    @DeleteMapping("/{id}/volunteer")
    public ResponseEntity<Void> withdraw(@PathVariable Long id, Authentication auth) {
        service.withdraw(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/history")
    public List<ExamDto.EventRow> history(Authentication auth) {
        return service.myHistory(auth.getName());
    }
}