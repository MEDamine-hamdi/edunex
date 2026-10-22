package com.edunex.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AnnouncementResponse(
        Long id,
        String title,
        String content,
        LocalDateTime publishedAt,
        String audience,
        Long classId,
        String className,
        String publishedBy,
        Integer recipients,
        List<AttachmentInfo> attachments
) {
    public record AttachmentInfo(Long id, String name, String contentType, Long size, boolean image) {
    }
}