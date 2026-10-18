package com.edunex.backend.service;

import com.edunex.backend.entity.AdministrationAgent;
import com.edunex.backend.entity.Course;
import com.edunex.backend.entity.CourseDocument;
import com.edunex.backend.entity.Professor;
import com.edunex.backend.entity.Student;
import com.edunex.backend.entity.Subject;
import com.edunex.backend.repository.CourseDocumentRepository;
import com.edunex.backend.repository.CourseRepository;
import com.edunex.backend.repository.SubjectRepository;
import com.edunex.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseDownloadAccessTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseDocumentRepository documentRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentAccessService studentAccessService;
    @Mock
    private FileStorage fileStorage;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CourseService service;

    @BeforeEach
    void setUp() {
        Subject subject = new Subject();
        subject.setId(1L);
        Professor owner = new Professor();
        owner.setId(10L);
        Course course = new Course();
        course.setSubject(subject);
        course.setProfessor(owner);
        CourseDocument document = new CourseDocument();
        document.setId(100L);
        document.setCourse(course);
        document.setFileUrl("abc/notes.txt");

        lenient().when(documentRepository.findById(100L)).thenReturn(Optional.of(document));
        lenient().when(fileStorage.load("abc/notes.txt")).thenReturn(new ByteArrayResource(new byte[]{1}));
    }

    private static Student student(Long id) {
        Student s = new Student();
        s.setId(id);
        return s;
    }

    private static Professor professor(Long id) {
        Professor p = new Professor();
        p.setId(id);
        return p;
    }

    @Test
    void enrolledStudentCanDownload() {
        when(userRepository.findByEmail("s@test.tn")).thenReturn(Optional.of(student(5L)));
        when(studentAccessService.accessibleSubjectIds(5L)).thenReturn(Set.of(1L));

        CourseService.DownloadedFile file = service.download("s@test.tn", 100L);

        assertThat(file.filename()).isEqualTo("notes.txt");
    }

    @Test
    void studentWithoutAccessToTheSubjectIsRefusedAndTheFileIsNeverRead() {
        when(userRepository.findByEmail("s@test.tn")).thenReturn(Optional.of(student(5L)));
        when(studentAccessService.accessibleSubjectIds(5L)).thenReturn(Set.of(2L));

        assertThatThrownBy(() -> service.download("s@test.tn", 100L)).isInstanceOf(AccessDeniedException.class);
        verify(fileStorage, never()).load(anyString());
    }

    @Test
    void owningProfessorCanDownload() {
        when(userRepository.findByEmail("p@test.tn")).thenReturn(Optional.of(professor(10L)));

        assertThat(service.download("p@test.tn", 100L).filename()).isEqualTo("notes.txt");
    }

    @Test
    void otherProfessorIsRefused() {
        when(userRepository.findByEmail("p@test.tn")).thenReturn(Optional.of(professor(11L)));

        assertThatThrownBy(() -> service.download("p@test.tn", 100L)).isInstanceOf(AccessDeniedException.class);
        verify(fileStorage, never()).load(anyString());
    }

    @Test
    void administrationAgentIsRefused() {
        when(userRepository.findByEmail("a@test.tn")).thenReturn(Optional.of(new AdministrationAgent()));

        assertThatThrownBy(() -> service.download("a@test.tn", 100L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unknownUserIsRefused() {
        when(userRepository.findByEmail("x@test.tn")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.download("x@test.tn", 100L)).isInstanceOf(AccessDeniedException.class);
    }
}