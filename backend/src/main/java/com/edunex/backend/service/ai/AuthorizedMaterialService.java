package com.edunex.backend.service.ai;

import com.edunex.backend.entity.Course;
import com.edunex.backend.entity.CourseDocument;
import com.edunex.backend.entity.Enrollment;
import com.edunex.backend.entity.Student;
import com.edunex.backend.entity.Subject;
import com.edunex.backend.enums.EnrollmentStatus;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.CourseDocumentRepository;
import com.edunex.backend.repository.CourseRepository;
import com.edunex.backend.repository.EnrollmentRepository;
import com.edunex.backend.repository.SubjectRepository;
import com.edunex.backend.repository.UserRepository;
import com.edunex.backend.service.StudentAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * The only place that decides which course documents the assistant may read for a student.
 * A subject is accessible when the student has an ACTIVE enrollment in a class that follows it
 * (StudentAccessService), and this is checked again on every call.
 */
@Service
@RequiredArgsConstructor
public class AuthorizedMaterialService {

    public record DocRef(Long id, String title, String storageKey, String courseTitle, String subjectName) {
    }

    public record StudentProfile(Long studentId, String firstName, List<String> classNames, List<String> subjects) {
    }

    public record ScopedMaterial(Long subjectId, Long courseId, String subjectName, String courseTitle,
                                 List<DocRef> documents) {
        public String label() {
            return courseTitle != null ? courseTitle + " · " + subjectName : subjectName;
        }
    }

    private final UserRepository userRepository;
    private final StudentAccessService studentAccessService;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseDocumentRepository documentRepository;
    private final SubjectRepository subjectRepository;

    @Transactional(readOnly = true)
    public StudentProfile profile(String email) {
        Student student = student(email);
        TreeSet<String> classNames = new TreeSet<>();
        TreeSet<String> subjects = new TreeSet<>();
        for (Enrollment e : enrollmentRepository.findByStudentIdAndStatus(student.getId(), EnrollmentStatus.ACTIVE)) {
            classNames.add(e.getSchoolClass().getName());
            for (Subject s : e.getSchoolClass().getSubjects()) {
                subjects.add(s.getName() + " (" + s.getCode() + ")");
            }
        }
        return new StudentProfile(student.getId(), student.getFirstName(),
                List.copyOf(classNames), List.copyOf(subjects));
    }

    /** The documents of one course, or of every course of a subject, if and only if the student may access it. */
    @Transactional(readOnly = true)
    public ScopedMaterial scoped(String email, Long subjectId, Long courseId) {
        Student student = student(email);
        Course single = null;
        Long sid = subjectId;
        if (courseId != null) {
            single = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
            if (sid != null && !sid.equals(single.getSubject().getId())) {
                throw new BadRequestException("The course does not belong to this subject");
            }
            sid = single.getSubject().getId();
        }
        if (sid == null) {
            throw new BadRequestException("A subject or a course is required");
        }
        if (!studentAccessService.accessibleSubjectIds(student.getId()).contains(sid)) {
            throw new AccessDeniedException("You do not have access to this subject");
        }
        Subject subject = subjectRepository.findById(sid)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        List<Course> courses = single != null ? List.of(single) : courseRepository.findBySubjectId(sid);
        List<DocRef> documents = new ArrayList<>();
        for (Course c : courses) {
            for (CourseDocument d : c.getDocuments()) {
                documents.add(ref(d, c));
            }
        }
        return new ScopedMaterial(sid, single != null ? single.getId() : null, subject.getName(),
                single != null ? single.getTitle() : null, documents);
    }

    /** Used by the background sweep: documents that were never indexed. */
    @Transactional(readOnly = true)
    public List<DocRef> unindexedDocuments() {
        return documentRepository.findNotIndexed().stream().map(d -> ref(d, d.getCourse())).toList();
    }

    private Student student(String email) {
        return userRepository.findByEmail(email)
                .filter(u -> u instanceof Student)
                .map(u -> (Student) u)
                .orElseThrow(() -> new AccessDeniedException("Student account required"));
    }

    private static DocRef ref(CourseDocument d, Course c) {
        return new DocRef(d.getId(), d.getTitle(), d.getFileUrl(), c.getTitle(), c.getSubject().getName());
    }
}