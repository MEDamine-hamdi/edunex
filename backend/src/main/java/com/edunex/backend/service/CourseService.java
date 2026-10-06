package com.edunex.backend.service;

import com.edunex.backend.dto.CourseRequest;
import com.edunex.backend.dto.CourseResponse;
import com.edunex.backend.entity.Course;
import com.edunex.backend.entity.CourseDocument;
import com.edunex.backend.entity.Professor;
import com.edunex.backend.entity.Student;
import com.edunex.backend.entity.Subject;
import com.edunex.backend.entity.User;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.CourseDocumentRepository;
import com.edunex.backend.repository.CourseRepository;
import com.edunex.backend.repository.SubjectRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CourseService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "odt", "odp",
            "txt", "md", "png", "jpg", "jpeg");

    private final CourseRepository courseRepository;
    private final CourseDocumentRepository documentRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final StudentAccessService studentAccessService;
    private final FileStorage fileStorage;

    public record DownloadedFile(Resource resource, String filename) {
    }

    // ---------- professor ----------

    @Transactional(readOnly = true)
    public List<CourseResponse.SubjectRef> professorSubjects(String email) {
        Professor p = professor(email);
        return subjectRepository.findByProfessorsId(p.getId()).stream()
                .sorted(Comparator.comparing(Subject::getName))
                .map(this::toRef)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> listMyCourses(String email) {
        Professor p = professor(email);
        return courseRepository.findByProfessorId(p.getId()).stream()
                .sorted(Comparator.comparing(Course::getCreatedAt, Comparator.reverseOrder()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CourseResponse createCourse(String email, CourseRequest req) {
        Professor p = professor(email);
        if (req.subjectId() == null) {
            throw new BadRequestException("subjectId is required");
        }
        Subject subject = subjectRepository.findById(req.subjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        boolean teaches = subject.getProfessors().stream().anyMatch(x -> x.getId().equals(p.getId()));
        if (!teaches) {
            throw new AccessDeniedException("You are not assigned to this subject");
        }
        Course c = new Course();
        c.setTitle(req.title().trim());
        c.setDescription(req.description());
        c.setSubject(subject);
        c.setProfessor(p);
        return toResponse(courseRepository.save(c));
    }

    @Transactional
    public CourseResponse updateCourse(String email, Long courseId, CourseRequest req) {
        Course c = ownCourse(email, courseId);
        c.setTitle(req.title().trim());
        c.setDescription(req.description());
        return toResponse(courseRepository.save(c));
    }

    @Transactional
    public void deleteCourse(String email, Long courseId) {
        Course c = ownCourse(email, courseId);
        c.getDocuments().forEach(d -> fileStorage.delete(d.getFileUrl()));
        courseRepository.delete(c);
    }

    @Transactional
    public CourseResponse addDocument(String email, Long courseId, MultipartFile file, String title) {
        Course c = ownCourse(email, courseId);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String extension = extensionOf(original);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("File type not supported. Allowed: " + String.join(", ", ALLOWED_EXTENSIONS));
        }

        String key = fileStorage.store(file);
        CourseDocument d = new CourseDocument();
        d.setTitle(title != null && !title.isBlank() ? title.trim() : baseName(original));
        d.setFileUrl(key);
        d.setType(extension.toUpperCase());
        d.setSize(file.getSize());
        d.setCourse(c);
        documentRepository.save(d);
        c.getDocuments().add(d);
        return toResponse(c);
    }

    @Transactional
    public CourseResponse renameDocument(String email, Long documentId, String title) {
        CourseDocument d = ownDocument(email, documentId);
        d.setTitle(title.trim());
        return toResponse(documentRepository.save(d).getCourse());
    }

    @Transactional
    public CourseResponse deleteDocument(String email, Long documentId) {
        CourseDocument d = ownDocument(email, documentId);
        Course c = d.getCourse();
        fileStorage.delete(d.getFileUrl());
        c.getDocuments().remove(d);
        documentRepository.delete(d);
        return toResponse(c);
    }

    // ---------- student ----------

    @Transactional(readOnly = true)
    public List<CourseResponse.SubjectRef> studentSubjects(String email) {
        Student s = student(email);
        Set<Long> ids = studentAccessService.accessibleSubjectIds(s.getId());
        return subjectRepository.findAllById(ids).stream()
                .sorted(Comparator.comparing(Subject::getName))
                .map(this::toRef)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> studentCourses(String email) {
        Student s = student(email);
        Set<Long> ids = studentAccessService.accessibleSubjectIds(s.getId());
        if (ids.isEmpty()) {
            return List.of();
        }
        return courseRepository.findBySubjectIdIn(ids).stream()
                .sorted(Comparator.comparing(Course::getCreatedAt, Comparator.reverseOrder()))
                .map(this::toResponse)
                .toList();
    }

    // ---------- download (professor owner or authorized student) ----------

    @Transactional(readOnly = true)
    public DownloadedFile download(String email, Long documentId) {
        CourseDocument d = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Unknown user"));
        Course c = d.getCourse();

        boolean allowed;
        if (user instanceof Professor p) {
            allowed = c.getProfessor().getId().equals(p.getId());
        } else if (user instanceof Student s) {
            allowed = studentAccessService.accessibleSubjectIds(s.getId()).contains(c.getSubject().getId());
        } else {
            allowed = false;
        }
        if (!allowed) {
            throw new AccessDeniedException("Access denied");
        }

        String filename = Paths.get(d.getFileUrl()).getFileName().toString();
        return new DownloadedFile(fileStorage.load(d.getFileUrl()), filename);
    }

    // ---------- helpers ----------

    private Professor professor(String email) {
        return userRepository.findByEmail(email)
                .filter(u -> u instanceof Professor)
                .map(u -> (Professor) u)
                .orElseThrow(() -> new AccessDeniedException("Professor account required"));
    }

    private Student student(String email) {
        return userRepository.findByEmail(email)
                .filter(u -> u instanceof Student)
                .map(u -> (Student) u)
                .orElseThrow(() -> new AccessDeniedException("Student account required"));
    }

    private Course ownCourse(String email, Long courseId) {
        Professor p = professor(email);
        Course c = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        if (!c.getProfessor().getId().equals(p.getId())) {
            throw new AccessDeniedException("This course belongs to another professor");
        }
        return c;
    }

    private CourseDocument ownDocument(String email, Long documentId) {
        Professor p = professor(email);
        CourseDocument d = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        if (!d.getCourse().getProfessor().getId().equals(p.getId())) {
            throw new AccessDeniedException("This document belongs to another professor");
        }
        return d;
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }

    private static String baseName(String name) {
        int dot = name.lastIndexOf('.');
        return dot <= 0 ? name : name.substring(0, dot);
    }

    private CourseResponse.SubjectRef toRef(Subject s) {
        return new CourseResponse.SubjectRef(s.getId(), s.getName(), s.getCode());
    }

    private CourseResponse toResponse(Course c) {
        List<CourseResponse.DocumentInfo> docs = c.getDocuments().stream()
                .sorted(Comparator.comparing(CourseDocument::getUploadedAt))
                .map(d -> new CourseResponse.DocumentInfo(d.getId(), d.getTitle(), d.getType(), d.getSize(), d.getUploadedAt()))
                .toList();
        Professor p = c.getProfessor();
        return new CourseResponse(c.getId(), c.getTitle(), c.getDescription(), c.getCreatedAt(),
                toRef(c.getSubject()), p.getFirstName() + " " + p.getLastName(), docs);
    }
}
