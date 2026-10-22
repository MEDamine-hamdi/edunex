package com.edunex.backend.service;

import com.edunex.backend.dto.AnnouncementRequest;
import com.edunex.backend.dto.AnnouncementResponse;
import com.edunex.backend.dto.AnnouncementResponse.AttachmentInfo;
import com.edunex.backend.entity.AdministrationAgent;
import com.edunex.backend.entity.Announcement;
import com.edunex.backend.entity.AnnouncementAttachment;
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
import com.edunex.backend.repository.AnnouncementAttachmentRepository;
import com.edunex.backend.repository.AnnouncementRepository;
import com.edunex.backend.repository.EnrollmentRepository;
import com.edunex.backend.repository.ProfessorRepository;
import com.edunex.backend.repository.SchoolClassRepository;
import com.edunex.backend.repository.StudentRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    public record Download(Resource resource, String filename, String contentType) {
    }

    private static final Set<String> IMAGES = Set.of("png", "jpg", "jpeg", "webp", "gif");
    private static final Set<String> DOCUMENTS = Set.of("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "odt", "odp", "txt", "csv");
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private static final int MAX_FILES = 5;

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementAttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ProfessorRepository professorRepository;
    private final AdministrationAgentRepository agentRepository;
    private final SchoolClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationService notificationService;
    private final FileStorage fileStorage;

    @Transactional
    public AnnouncementResponse publish(String email, AnnouncementRequest req) {
        return publish(email, req, List.of());
    }

    @Transactional
    public AnnouncementResponse publish(String email, AnnouncementRequest req, List<MultipartFile> files) {
        AdministrationAgent publisher = userRepository.findByEmail(email)
                .filter(u -> u instanceof AdministrationAgent)
                .map(u -> (AdministrationAgent) u)
                .orElseThrow(() -> new AccessDeniedException("Administration agent account required"));
        List<MultipartFile> uploads = files == null ? List.of()
                : files.stream().filter(f -> f != null && !f.isEmpty()).toList();
        validate(uploads);

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

        List<AttachmentInfo> infos = new ArrayList<>();
        for (MultipartFile f : uploads) {
            AnnouncementAttachment att = new AnnouncementAttachment();
            att.setAnnouncementId(saved.getId());
            att.setFileKey(fileStorage.store(f));
            att.setFileName(displayName(f));
            att.setContentType(f.getContentType());
            att.setSize(f.getSize());
            infos.add(info(attachmentRepository.save(att)));
        }

        List<User> recipients = recipients(req.audience(), target);
        recipients.removeIf(u -> u.getId().equals(publisher.getId()));
        notificationService.notifyUsers(recipients, NotificationType.ANNOUNCEMENT, "Annonce : " + saved.getTitle());
        return toResponse(saved, recipients.size(), Map.of(saved.getId(), infos));
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> listAll() {
        List<Announcement> all = announcementRepository.findAllByOrderByPublishedAtDesc();
        Map<Long, List<AttachmentInfo>> att = attachmentsOf(all);
        return all.stream().map(a -> toResponse(a, null, att)).toList();
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> visibleTo(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new AccessDeniedException("Unknown user"));
        Set<Long> classIds = classIdsOf(user);
        List<Announcement> visible = announcementRepository.findAllByOrderByPublishedAtDesc().stream()
                .filter(a -> isVisible(a, user, classIds))
                .toList();
        Map<Long, List<AttachmentInfo>> att = attachmentsOf(visible);
        return visible.stream().map(a -> toResponse(a, null, att)).toList();
    }

    /** Agents can open every attachment; everybody else only those of announcements addressed to them. */
    @Transactional(readOnly = true)
    public Download download(String email, Long announcementId, Long attachmentId) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new AccessDeniedException("Unknown user"));
        Announcement a = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));
        if (!(user instanceof AdministrationAgent) && !isVisible(a, user, classIdsOf(user))) {
            throw new AccessDeniedException("Not allowed");
        }
        AnnouncementAttachment att = attachmentRepository.findById(attachmentId)
                .filter(x -> x.getAnnouncementId().equals(announcementId))
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));
        return new Download(fileStorage.load(att.getFileKey()), att.getFileName(), att.getContentType());
    }

    // ---------- helpers ----------

    private static void validate(List<MultipartFile> files) {
        if (files.size() > MAX_FILES) {
            throw new BadRequestException("5 files maximum per announcement");
        }
        for (MultipartFile f : files) {
            if (f.getSize() > MAX_FILE_BYTES) {
                throw new BadRequestException("A file is too large (10 MB maximum)");
            }
            String ext = extension(displayName(f));
            if (!IMAGES.contains(ext) && !DOCUMENTS.contains(ext)) {
                throw new BadRequestException("Unsupported file type: use an image (PNG, JPG, WEBP, GIF) or a document (PDF, DOCX, PPTX, XLSX, ODT, TXT)");
            }
        }
    }

    private static String displayName(MultipartFile f) {
        String original = f.getOriginalFilename() == null ? "file" : f.getOriginalFilename();
        String name = original.substring(Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1).trim();
        return name.isEmpty() ? "file" : (name.length() > 200 ? name.substring(name.length() - 200) : name);
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static AttachmentInfo info(AnnouncementAttachment x) {
        return new AttachmentInfo(x.getId(), x.getFileName(), x.getContentType(), x.getSize(),
                IMAGES.contains(extension(x.getFileName())));
    }

    private Map<Long, List<AttachmentInfo>> attachmentsOf(Collection<Announcement> list) {
        if (list.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<AttachmentInfo>> map = new HashMap<>();
        for (AnnouncementAttachment x : attachmentRepository.findByAnnouncementIdIn(
                list.stream().map(Announcement::getId).toList())) {
            map.computeIfAbsent(x.getAnnouncementId(), k -> new ArrayList<>()).add(info(x));
        }
        return map;
    }

    private Set<Long> classIdsOf(User user) {
        Set<Long> classIds = new HashSet<>();
        if (user instanceof Student s) {
            enrollmentRepository.findByStudentIdAndStatus(s.getId(), EnrollmentStatus.ACTIVE)
                    .forEach(e -> classIds.add(e.getSchoolClass().getId()));
        }
        return classIds;
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

    private AnnouncementResponse toResponse(Announcement a, Integer recipients, Map<Long, List<AttachmentInfo>> attachments) {
        SchoolClass c = a.getTargetClass();
        AdministrationAgent p = a.getPublishedBy();
        return new AnnouncementResponse(a.getId(), a.getTitle(), a.getContent(), a.getPublishedAt(),
                a.getTargetAudience().name(), c != null ? c.getId() : null, c != null ? c.getName() : null,
                p.getFirstName() + " " + p.getLastName(), recipients, attachments.getOrDefault(a.getId(), List.of()));
    }
}