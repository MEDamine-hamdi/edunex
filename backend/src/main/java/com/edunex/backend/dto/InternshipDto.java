package com.edunex.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class InternshipDto {

    private InternshipDto() {
    }

    public record PersonRef(Long id, String name, String email, String number) {
    }

    public record Save(@NotNull Long studentId, @NotNull Long supervisorId, @Size(max = 200) String company,
                       @Size(max = 200) String companySupervisor, @NotBlank @Size(max = 300) String title,
                       @Size(max = 40) String kind, LocalDate startDate, LocalDate endDate) {
    }

    public record Row(Long id, PersonRef student, PersonRef supervisor, String company, String companySupervisor,
                      String title, String kind, LocalDate startDate, LocalDate endDate,
                      Long defenseId, String defenseStatus, LocalDateTime defenseAt) {
    }

    public record Lookups(List<PersonRef> students, List<PersonRef> professors) {
    }

    public record JurorInput(@NotNull Long professorId, @NotBlank String role) {
    }

    public record DefenseSave(@NotNull Long internshipId, @NotNull LocalDateTime scheduledAt, @Size(max = 100) String room,
                              @NotNull @Size(min = 2, max = 3) @Valid List<JurorInput> jury) {
    }

    public record ResultInput(@NotNull @DecimalMin("0") @DecimalMax("20") Double grade, @Size(max = 2000) String comments) {
    }

    public record Juror(Long professorId, String name, String email, String role) {
    }

    public record DefenseRow(Long id, String status, LocalDateTime scheduledAt, String room, PersonRef student,
                             PersonRef supervisor, String title, List<Juror> jury, Double grade, String mention) {
    }

    public record DefenseDetail(Long id, String status, LocalDateTime scheduledAt, String room, Double grade,
                                String mention, String decision, String comments, Long internshipId, String title,
                                String company, String companySupervisor, String kind, LocalDate startDate,
                                LocalDate endDate, PersonRef student, PersonRef supervisor, List<Juror> jury,
                                boolean hasReport, String reportName, boolean hasRecord, String recordName) {
    }
}