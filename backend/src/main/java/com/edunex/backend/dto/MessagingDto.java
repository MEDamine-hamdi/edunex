package com.edunex.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class MessagingDto {

    private MessagingDto() {
    }

    public record Contact(Long id, String name, String role) {
    }

    public record ConvRow(Long id, String type, String title, Long photoUserId, boolean hasPhoto, String lastText,
                          LocalDateTime lastAt, long unread, int members) {
    }

    public record Member(Long id, String name, String role, boolean admin) {
    }

    public record ConvDetail(Long id, String type, String title, Long photoUserId, boolean hasPhoto, boolean iAmAdmin,
                             List<Member> members) {
    }

    public record MessageView(Long id, Long conversationId, Long senderId, String senderName, String content,
                              LocalDateTime createdAt, boolean deleted, boolean deletedByOther, boolean hasFile,
                              String fileName, String fileKind, String fileType, Long fileSize) {
    }

    public record MessagePage(List<MessageView> messages, LocalDateTime serverTime) {
    }

    public record DirectRequest(@NotNull Long userId) {
    }

    public record GroupRequest(@NotBlank @Size(max = 100) String name, @NotNull @Size(min = 1, max = 49) List<Long> memberIds) {
    }

    public record RenameRequest(@NotBlank @Size(max = 100) String name) {
    }

    public record TextRequest(@NotBlank @Size(max = 4000) String content) {
    }

    public record MembersRequest(@NotNull @Size(min = 1, max = 49) List<Long> userIds) {
    }

    public record UnreadResponse(long unread) {
    }
}