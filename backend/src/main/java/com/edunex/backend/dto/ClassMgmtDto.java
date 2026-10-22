package com.edunex.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class ClassMgmtDto {

    private ClassMgmtDto() {
    }

    public record GroupRow(Long id, String name, int students) {
    }

    public record ProfRef(Long id, String name) {
    }

    /** custom = professors explicitly set for this class; otherwise the general professors of the subject. */
    public record SubjectRow(Long id, String name, String code, List<ProfRef> professors, boolean custom) {
    }

    public record ProfessorRow(Long id, String name, String email, List<String> subjects) {
    }

    public record StudentRow(Long enrollmentId, Long studentId, String name, String studentNumber, String email,
                             Long groupId, String groupName) {
    }

    public record Overview(Long id, String name, String level, String academicYear, List<GroupRow> groups,
                           List<SubjectRow> subjects, List<ProfessorRow> professors, List<StudentRow> students) {
    }

    public record Candidate(Long id, String name, String studentNumber, String email) {
    }

    public record AddStudents(@NotEmpty List<Long> studentIds, Long groupId) {
    }

    public record AddResult(int added, int skipped, List<String> messages) {
    }

    public record GroupChange(Long groupId) {
    }

    public record GroupName(@NotBlank @Size(max = 100) String name) {
    }
}