package com.edunex.backend.dto;

import java.util.List;

public final class ImportDto {

    private ImportDto() {
    }

    public record Row(int id, String day, String start, String end, String room, String rawClass, String rawSubject,
                      String rawProfessor, Long classId, String className, Long subjectId, String subjectName,
                      Long professorId, String professorName, List<String> issues, String conflict, boolean include) {
    }

    public record JobView(String id, String status, String message, int totalPages, int donePages,
                          List<String> warnings, List<Row> rows) {
    }

    public record ConfirmRow(Long classId, Long subjectId, Long professorId, String day, String start, String end,
                             String room) {
    }

    public record ConfirmRequest(Long timetableId, String academicYear, String semester, List<ConfirmRow> rows) {
    }

    public record RowResult(int index, boolean created, String error) {
    }

    public record ConfirmResponse(Long timetableId, int created, int failed, List<RowResult> results) {
    }
}