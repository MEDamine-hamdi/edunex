package com.edunex.backend.dto;

/** kind is COURSE (a professor's document) or INSTITUTE (the institute guide). */
public record SourceRef(int n, Long documentId, String documentTitle, String courseTitle, String subjectName, String kind) {
}