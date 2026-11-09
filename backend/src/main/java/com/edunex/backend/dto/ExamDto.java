package com.edunex.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class ExamDto {

    private ExamDto() {
    }

    public record ClassItem(Long id, String name) {
    }

    public record SubjectItem(Long id, String name, String code, List<Long> classIds) {
    }

    public record Lookups(List<SubjectItem> subjects, List<ClassItem> classes) {
    }

    public record SupervisorItem(Long professorId, String name, String email, String kind, String mode, String assignedBy) {
    }

    public record ExamItem(Long id, String examType, Long subjectId, String subjectName, String subjectCode,
                           List<ClassItem> classes, LocalDate date, LocalTime startTime, LocalTime endTime, String room,
                           int needed, String status, String cancelReason, List<SupervisorItem> supervisors, int students) {
    }

    public record SaveExam(@NotNull Long subjectId, @NotEmpty List<Long> classIds, @NotBlank @Size(max = 40) String examType,
                           @NotNull LocalDate date, @NotNull LocalTime startTime, @NotNull LocalTime endTime,
                           @Size(max = 100) String room, @Min(1) @Max(20) int supervisorsNeeded) {
    }

    public record AssignRequest(@NotNull Long professorId) {
    }

    public record ReasonRequest(@Size(max = 500) String reason) {
    }

    public record KindRequest(@NotBlank String kind) {
    }

    public record ProfessorRow(Long id, String name, String email, String kind, long upcoming, long past) {
    }

    public record EventRow(Long id, Long examId, String exam, Long professorId, String professor, String action,
                           String actor, String reason, LocalDateTime at) {
    }

    public record AvailableExam(ExamItem exam, boolean canJoin, String blocked) {
    }

    public record MyExam(ExamItem exam, String mode, boolean canWithdraw, boolean past) {
    }

    public record StudentExam(Long id, String examType, String subjectName, String subjectCode, LocalDate date,
                              LocalTime startTime, LocalTime endTime, String room, String status) {
    }

    public record AssignResult(ExamItem exam, String warning) {
    }

    public record MyKind(String kind) {
    }
}