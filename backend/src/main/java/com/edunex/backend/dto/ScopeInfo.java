package com.edunex.backend.dto;

/** kind is GENERAL, SUBJECT or COURSE. */
public record ScopeInfo(String kind, String label, int documents, int indexedDocuments, boolean available) {
}