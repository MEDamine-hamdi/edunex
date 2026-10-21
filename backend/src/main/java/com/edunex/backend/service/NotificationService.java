package com.edunex.backend.service;

import com.edunex.backend.dto.NotificationResponse;
import com.edunex.backend.entity.Enrollment;
import com.edunex.backend.entity.Notification;
import com.edunex.backend.entity.SchoolClass;
import com.edunex.backend.entity.Student;
import com.edunex.backend.entity.User;
import com.edunex.backend.enums.EnrollmentStatus;
import com.edunex.backend.enums.NotificationType;
import com.edunex.backend.enums.UserStatus;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.EnrollmentRepository;
import com.edunex.backend.repository.NotificationRepository;
import com.edunex.backend.repository.SchoolClassRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SchoolClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final MailOutboxService mailOutbox;

    // ---------- sending ----------

    @Transactional
    public void notifyUsers(Collection<? extends User> users, NotificationType type, String message) {
        String text = message.length() > 500 ? message.substring(0, 497) + "..." : message;
        Set<Long> seen = new HashSet<>();
        List<Notification> batch = new ArrayList<>();
        for (User u : users) {
            if (u.getStatus() != UserStatus.ACTIVE || !seen.add(u.getId())) {
                continue;
            }
            Notification n = new Notification();
            n.setUser(u);
            n.setType(type);
            n.setMessage(text);
            batch.add(n);
        }
        notificationRepository.saveAll(batch);
        mailOutbox.enqueueNotifications(batch);
    }

    @Transactional
    public void notifyClassStudents(Long classId, NotificationType type, String message) {
        notifyUsers(studentsOf(List.of(classId)), type, message);
    }

    @Transactional
    public void notifySubjectStudents(Long subjectId, NotificationType type, String message) {
        List<Long> classIds = classRepository.findBySubjectsId(subjectId).stream()
                .map(SchoolClass::getId)
                .toList();
        notifyUsers(studentsOf(classIds), type, message);
    }

    private List<Student> studentsOf(Collection<Long> classIds) {
        if (classIds.isEmpty()) {
            return List.of();
        }
        return enrollmentRepository.findBySchoolClassIdInAndStatus(classIds, EnrollmentStatus.ACTIVE).stream()
                .map(Enrollment::getStudent)
                .toList();
    }

    // ---------- reading (own notifications only) ----------

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(String email) {
        return notificationRepository.findByUserIdOrderBySentAtDesc(userId(email)).stream()
                .limit(200)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(String email) {
        return notificationRepository.countByUserIdAndReadFalse(userId(email));
    }

    @Transactional
    public NotificationResponse markRead(String email, Long notificationId) {
        Long uid = userId(email);
        Notification n = notificationRepository.findById(notificationId)
                .filter(x -> x.getUser().getId().equals(uid))
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        n.setRead(true);
        return toResponse(notificationRepository.save(n));
    }

    @Transactional
    public void markAllRead(String email) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalse(userId(email));
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    private Long userId(String email) {
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new AccessDeniedException("Unknown user"));
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getMessage(), n.getType().name(), n.isRead(), n.getSentAt());
    }
}