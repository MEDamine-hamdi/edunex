package com.edunex.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class CertificateDto {

    private CertificateDto() {
    }

    public record AbsenceItem(Long attendanceId, LocalDate date, String subjectName, String status, String justification) {
    }

    public record ExamItem(Long examId, String subjectName, String examType, LocalDate date, LocalTime startTime, String room) {
    }

    public record Coverage(List<AbsenceItem> absences, List<ExamItem> exams) {
    }

    public record RetakeItem(Long id, Long examId, String subjectName, String examType, LocalDate originalDate,
                             Long studentId, String studentName, String studentNumber, String status,
                             LocalDate date, LocalTime startTime, LocalTime endTime, String room) {
    }

    public record Row(Long id, Long studentId, String studentName, String studentNumber, String source, String status,
                      LocalDate startDate, LocalDate endDate, String note, boolean hasFile, String fileName,
                      LocalDateTime createdAt, String createdBy, LocalDateTime decidedAt, String decidedBy,
                      String decisionNote) {
    }

    public record Detail(Row row, List<AbsenceItem> absences, List<ExamItem> exams, List<RetakeItem> retakes) {
    }

    public record DecisionRequest(@Size(max = 1000) String note) {
    }

    public record RejectRequest(@NotBlank @Size(max = 1000) String reason) {
    }

    public record ScheduleRequest(@NotEmpty List<Long> retakeIds, @NotNull LocalDate date, @NotNull LocalTime startTime,
                                  @NotNull LocalTime endTime, @Size(max = 100) String room) {
    }

    public record StudentHit(Long id, String name, String number) {
    }
}