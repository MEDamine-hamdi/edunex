package com.edunex.backend.service.ai;

/** A piece of text handed to the model. kind is COURSE or INSTITUTE. */
public record Passage(int sourceNumber, String kind, Long documentId, String documentTitle,
                      String courseTitle, String subjectName, String text) {
}