package com.edunex.backend.service;

import com.edunex.backend.entity.Enrollment;
import com.edunex.backend.entity.SchoolClass;
import com.edunex.backend.entity.Subject;
import com.edunex.backend.enums.EnrollmentStatus;
import com.edunex.backend.repository.EnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentAccessServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private StudentAccessService service;

    private static Subject subject(Long id) {
        Subject s = new Subject();
        s.setId(id);
        return s;
    }

    private static Enrollment enrollmentIn(Long classId, Subject... subjects) {
        SchoolClass c = new SchoolClass();
        c.setId(classId);
        c.setSubjects(new HashSet<>(Arrays.asList(subjects)));
        Enrollment e = new Enrollment();
        e.setSchoolClass(c);
        return e;
    }

    @Test
    void studentSeesTheSubjectsOfAllTheirActiveClasses() {
        when(enrollmentRepository.findByStudentIdAndStatus(5L, EnrollmentStatus.ACTIVE)).thenReturn(List.of(
                enrollmentIn(1L, subject(1L), subject(2L)),
                enrollmentIn(2L, subject(2L), subject(3L))));

        assertThat(service.accessibleSubjectIds(5L)).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void noActiveEnrollmentMeansNoAccess() {
        when(enrollmentRepository.findByStudentIdAndStatus(5L, EnrollmentStatus.ACTIVE)).thenReturn(List.of());

        assertThat(service.accessibleSubjectIds(5L)).isEmpty();
    }

    @Test
    void onlyActiveEnrollmentsAreConsulted() {
        when(enrollmentRepository.findByStudentIdAndStatus(5L, EnrollmentStatus.ACTIVE)).thenReturn(List.of());

        service.accessibleSubjectIds(5L);

        verify(enrollmentRepository).findByStudentIdAndStatus(5L, EnrollmentStatus.ACTIVE);
        verifyNoMoreInteractions(enrollmentRepository);
    }
}