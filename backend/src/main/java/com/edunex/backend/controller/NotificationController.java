package com.edunex.backend.controller;

import com.edunex.backend.dto.NotificationResponse;
import com.edunex.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> list(Authentication auth) {
        return notificationService.list(auth.getName());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Authentication auth) {
        return Map.of("count", notificationService.unreadCount(auth.getName()));
    }

    @PutMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id, Authentication auth) {
        return notificationService.markRead(auth.getName(), id);
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication auth) {
        notificationService.markAllRead(auth.getName());
        return ResponseEntity.noContent().build();
    }
}