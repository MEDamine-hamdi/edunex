package com.edunex.backend.dto;

import java.time.LocalDateTime;

public record InstituteDocumentResponse(Long id, String title, String type, Long size, String status,
                                        int chunkCount, LocalDateTime uploadedAt, String uploadedBy,
                                        String sourceKind, String url, LocalDateTime lastFetchedAt, String lastError) {
}