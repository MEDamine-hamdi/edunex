package com.edunex.backend.service;

import com.edunex.backend.dto.AnnouncementRequest;
import com.edunex.backend.dto.AnnouncementResponse;
import com.edunex.backend.entity.AdministrationAgent;
import com.edunex.backend.entity.Announcement;
import com.edunex.backend.entity.Professor;
import com.edunex.backend.entity.SchoolClass;
import com.edunex.backend.entity.Student;
import com.edunex.backend.entity.User;
import com.edunex.backend.enums.EnrollmentStatus;
import com.edunex.backend.enums.NotificationType;
import com.edunex.backend.enums.TargetAudience;
import com.edunex.backend.enums.UserStatus;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.AdministrationAgentRepository;
import com.edunex.backend.repository.AnnouncementRepository;
import com.edunex.backend.repository.EnrollmentRepository;
import com.edunex.backend.repository.ProfessorRepository;
import com.edunex.backend.repository.SchoolClassRepository;
import com.edunex.backend.repository.StudentRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ProfessorRepository professorRepository;
    private final AdministrationAgentRepository agentRepository;
    private final SchoolClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationService notificationService;

    @Transactional
    public AnnouncementResponse publish(String email, AnnouncementRequest req) {
        AdministrationAgent publisher = userRepository.findByEmail(email)
                .filter(u -> u instanceof AdministrationAgent)
                .map(u -> (AdministrationAgent) u)
                .orElseThrow(() -> new AccessDeniedException("Administration agent account required"));

        SchoolClass target = null;
        if (req.audience() == TargetAudience.CLASS) {
            if (req.classId() == null) {
                throw new BadRequestException("classId is required when the audience is CLASS");
            }
            target = classRepository.findById(req.classId())
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        }

        Announcement a = new Announcement();
        a.setTitle(req.title().trim());
        a.setContent(req.content().trim());
        a.setTargetAudience(req.audience());
        a.setTargetClass(target);
        a.setPublishedBy(publisher);
        Announcement saved = announcementRepository.save(a);

        List<User> recipients = recipients(req.audience(), target);
        recipients.removeIf(u -> u.getId().equals(publisher.getId()));
        notificationService.notifyUsers(recipients, NotificationType.ANNOUNCEMENT, "Annonce : " + saved.getTitle());
        return toResponse(saved, recipients.size());
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> listAll() {
        return announcementRepository.findAllByOrderByPublishedAtDesc().stream()
                .map(a -> toResponse(a, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> visibleTo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Unknown user"));
        Set<Long> classIds = new HashSet<>();
        if (user instanceof Student s) {
            enrollmentRepository.findByStudentIdAndStatus(s.getId(), EnrollmentStatus.ACTIVE)
                    .forEach(e -> classIds.add(e.getSchoolClass().getId()));
        }
        return announcementRepository.findAllByOrderByPublishedAtDesc().stream()
                .filter(a -> isVisible(a, user, classIds))
                .map(a -> toResponse(a, null))
                .toList();
    }

    private boolean isVisible(Announcement a, User user, Set<Long> classIds) {
        return switch (a.getTargetAudience()) {
            case ALL -> true;
            case STUDENTS -> user instanceof Student;
            case PROFESSORS -> user instanceof Professor;
            case ADMIN_AGENTS -> user instanceof AdministrationAgent;
            case CLASS -> a.getTargetClass() != null && classIds.contains(a.getTargetClass().getId());
        };
    }

    private List<User> recipients(TargetAudience audience, SchoolClass target) {
        List<User> all = new ArrayList<>();
        switch (audience) {
            case ALL -> all.addAll(userRepository.findAll());
            case STUDENTS -> all.addAll(studentRepository.findAll());
            case PROFESSORS -> all.addAll(professorRepository.findAll());
            case ADMIN_AGENTS -> all.addAll(agentRepository.findAll());
            case CLASS -> enrollmentRepository
                    .findBySchoolClassIdInAndStatus(List.of(target.getId()), EnrollmentStatus.ACTIVE)
                    .forEach(e -> all.add(e.getStudent()));
        }
        return all.stream()
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private AnnouncementResponse toResponse(Announcement a, Integer recipients) {
        SchoolClass c = a.getTargetClass();
        AdministrationAgent p = a.getPublishedBy();
        return new AnnouncementResponse(a.getId(), a.getTitle(), a.getContent(), a.getPublishedAt(),
                a.getTargetAudience().name(), c != null ? c.getId() : null, c != null ? c.getName() : null,
                p.getFirstName() + " " + p.getLastName(), recipients);
    }
}