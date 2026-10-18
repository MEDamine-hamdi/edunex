package com.edunex.backend.service;

import com.edunex.backend.dto.SessionRequest;
import com.edunex.backend.dto.SessionResponse;
import com.edunex.backend.entity.Professor;
import com.edunex.backend.entity.SchoolClass;
import com.edunex.backend.entity.Subject;
import com.edunex.backend.entity.Timetable;
import com.edunex.backend.entity.TimetableSession;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.ConflictException;
import com.edunex.backend.repository.EnrollmentRepository;
import com.edunex.backend.repository.ProfessorRepository;
import com.edunex.backend.repository.SchoolClassRepository;
import com.edunex.backend.repository.SubjectRepository;
import com.edunex.backend.repository.TimetableRepository;
import com.edunex.backend.repository.TimetableSessionRepository;
import com.edunex.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimetableConflictTest {

    @Mock
    private TimetableRepository timetableRepository;
    @Mock
    private TimetableSessionRepository sessionRepository;
    @Mock
    private SchoolClassRepository classRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private ProfessorRepository professorRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TimetableService service;

    private Timetable timetable;
    private Subject subject;
    private SchoolClass classA;
    private SchoolClass classB;
    private Professor prof1;
    private Professor prof2;

    @BeforeEach
    void setUp() {
        timetable = new Timetable();
        timetable.setId(1L);
        prof1 = professor(1L);
        prof2 = professor(2L);
        Professor prof3 = professor(3L);

        subject = new Subject();
        subject.setId(1L);
        subject.setName("Bases de donnees");
        subject.setCode("BD101");
        subject.setProfessors(new HashSet<>(Arrays.asList(prof1, prof2)));

        classA = schoolClass(1L, subject);
        classB = schoolClass(2L, subject);
        SchoolClass classWithoutSubject = schoolClass(3L);

        lenient().when(timetableRepository.findById(1L)).thenReturn(Optional.of(timetable));
        lenient().when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        lenient().when(classRepository.findById(1L)).thenReturn(Optional.of(classA));
        lenient().when(classRepository.findById(2L)).thenReturn(Optional.of(classB));
        lenient().when(classRepository.findById(3L)).thenReturn(Optional.of(classWithoutSubject));
        lenient().when(professorRepository.findById(1L)).thenReturn(Optional.of(prof1));
        lenient().when(professorRepository.findById(2L)).thenReturn(Optional.of(prof2));
        lenient().when(professorRepository.findById(3L)).thenReturn(Optional.of(prof3));
        lenient().when(sessionRepository.save(any(TimetableSession.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Professor professor(Long id) {
        Professor p = new Professor();
        p.setId(id);
        p.setFirstName("Prof" + id);
        p.setLastName("Test");
        return p;
    }

    private static SchoolClass schoolClass(Long id, Subject... subjects) {
        SchoolClass c = new SchoolClass();
        c.setId(id);
        c.setName("Class " + id);
        c.setSubjects(new HashSet<>(Arrays.asList(subjects)));
        return c;
    }

    private TimetableSession existingMondaySession() {
        TimetableSession s = new TimetableSession();
        s.setId(50L);
        s.setTimetable(timetable);
        s.setDayOfWeek(DayOfWeek.MONDAY);
        s.setStartTime(LocalTime.of(9, 0));
        s.setEndTime(LocalTime.of(10, 30));
        s.setRoom("A101");
        s.setSchoolClass(classA);
        s.setSubject(subject);
        s.setProfessor(prof1);
        return s;
    }

    private SessionRequest request(DayOfWeek day, String start, String end, String room, SchoolClass c, Professor p) {
        return new SessionRequest(day, LocalTime.parse(start), LocalTime.parse(end), room,
                c.getId(), subject.getId(), p.getId());
    }

    private void mondayAlreadyHasASession() {
        when(sessionRepository.findByTimetableIdAndDayOfWeek(1L, DayOfWeek.MONDAY))
                .thenReturn(List.of(existingMondaySession()));
    }

    @Test
    void sameProfessorAtAnOverlappingTimeIsRefused() {
        mondayAlreadyHasASession();

        assertThatThrownBy(() -> service.addSession(1L, request(DayOfWeek.MONDAY, "09:30", "10:30", "C303", classB, prof1)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("professor");
    }

    @Test
    void sameRoomAtAnOverlappingTimeIsRefused() {
        mondayAlreadyHasASession();

        assertThatThrownBy(() -> service.addSession(1L, request(DayOfWeek.MONDAY, "09:00", "10:30", "A101", classB, prof2)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("room");
    }

    @Test
    void sameClassAtAnOverlappingTimeIsRefused() {
        mondayAlreadyHasASession();

        assertThatThrownBy(() -> service.addSession(1L, request(DayOfWeek.MONDAY, "10:00", "11:30", "B202", classA, prof2)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("class");
    }

    @Test
    void backToBackSessionsAreAllowed() {
        mondayAlreadyHasASession();

        SessionResponse result = service.addSession(1L, request(DayOfWeek.MONDAY, "10:30", "12:00", "A101", classA, prof1));

        assertThat(result.startTime()).isEqualTo("10:30");
        assertThat(result.endTime()).isEqualTo("12:00");
    }

    @Test
    void theSameSlotOnAnotherDayIsAllowed() {
        SessionResponse result = service.addSession(1L, request(DayOfWeek.TUESDAY, "09:00", "10:30", "A101", classA, prof1));

        assertThat(result.dayOfWeek()).isEqualTo("TUESDAY");
    }

    @Test
    void endTimeBeforeStartTimeIsRejected() {
        assertThatThrownBy(() -> service.addSession(1L, request(DayOfWeek.MONDAY, "12:00", "11:00", "A101", classA, prof1)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("End time");
    }

    @Test
    void professorNotAssignedToTheSubjectIsRejected() {
        Professor prof3 = professor(3L);

        assertThatThrownBy(() -> service.addSession(1L, request(DayOfWeek.MONDAY, "14:00", "15:30", "A101", classA, prof3)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not assigned");
    }

    @Test
    void subjectNotTaughtInTheClassIsRejected() {
        SchoolClass classWithoutSubject = schoolClass(3L);

        assertThatThrownBy(() -> service.addSession(1L, request(DayOfWeek.MONDAY, "14:00", "15:30", "A101", classWithoutSubject, prof1)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not taught");
    }
}