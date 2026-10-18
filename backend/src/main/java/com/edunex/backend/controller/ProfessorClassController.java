package com.edunex.backend.controller;

import com.edunex.backend.dto.ProfessorClassResponse;
import com.edunex.backend.service.ProfessorClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/professor/classes")
@PreAuthorize("hasRole('PROFESSOR')")
@RequiredArgsConstructor
public class ProfessorClassController {

    private final ProfessorClassService professorClassService;

    @GetMapping
    public List<ProfessorClassResponse> classes(Authentication auth) {
        return professorClassService.myClasses(auth.getName());
    }
}