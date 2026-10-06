package com.edunex.backend.controller;

import com.edunex.backend.dto.CourseRequest;
import com.edunex.backend.dto.CourseResponse;
import com.edunex.backend.dto.RenameDocumentRequest;
import com.edunex.backend.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/professor")
@PreAuthorize("hasRole('PROFESSOR')")
@RequiredArgsConstructor
public class ProfessorCourseController {

    private final CourseService courseService;

    @GetMapping("/subjects")
    public List<CourseResponse.SubjectRef> subjects(Authentication auth) {
        return courseService.professorSubjects(auth.getName());
    }

    @GetMapping("/courses")
    public List<CourseResponse> courses(Authentication auth) {
        return courseService.listMyCourses(auth.getName());
    }

    @PostMapping("/courses")
    public ResponseEntity<CourseResponse> create(@Valid @RequestBody CourseRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.createCourse(auth.getName(), request));
    }

    @PutMapping("/courses/{id}")
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody CourseRequest request, Authentication auth) {
        return courseService.updateCourse(auth.getName(), id, request);
    }

    @DeleteMapping("/courses/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        courseService.deleteCourse(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/courses/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourseResponse> upload(@PathVariable Long id,
                                                 @RequestParam("file") MultipartFile file,
                                                 @RequestParam(value = "title", required = false) String title,
                                                 Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseService.addDocument(auth.getName(), id, file, title));
    }

    @PutMapping("/documents/{id}")
    public CourseResponse rename(@PathVariable Long id, @Valid @RequestBody RenameDocumentRequest request,
                                 Authentication auth) {
        return courseService.renameDocument(auth.getName(), id, request.title());
    }

    @DeleteMapping("/documents/{id}")
    public CourseResponse deleteDocument(@PathVariable Long id, Authentication auth) {
        return courseService.deleteDocument(auth.getName(), id);
    }
}
