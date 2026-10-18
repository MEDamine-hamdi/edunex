package com.edunex.backend.service;

import com.edunex.backend.dto.CreateEnrollmentRequest;
import com.edunex.backend.dto.EnrollmentResponse;
import com.edunex.backend.entity.Enrollment;
import com.edunex.backend.entity.SchoolClass;
import com.edunex.backend.entity.Section;
import com.edunex.backend.entity.Student;
import com.edunex.backend.enums.EnrollmentStatus;
import com.edunex.backend.exception.ConflictException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.EnrollmentRepository;
import com.edunex.backend.repository.SchoolClassRepository;
import com.edunex.backend.repository.SectionRepository;
import com.edunex.backend.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository classRepository;
    private final SectionRepository sectionRepository;

    @Transactional
    public EnrollmentResponse create(CreateEnrollmentRequest req) {
        Student student = studentRepository.findById(req.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        SchoolClass schoolClass = classRepository.findById(req.classId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        Section section = null;
        if (req.sectionId() != null) {
            section = sectionRepository.findById(req.sectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
            if (section.getSchoolClass() == null
                    || !section.getSchoolClass().getId().equals(schoolClass.getId())) {
                throw new ConflictException("Section does not belong to this class");
            }
        }

        if (enrollmentRepository.existsByStudentIdAndSchoolClassIdAndAcademicYearAndStatus(
                student.getId(), schoolClass.getId(), schoolClass.getAcademicYear(), EnrollmentStatus.ACTIVE)) {
            throw new ConflictException("Student is already enrolled in this class for this academic year");
        }

        Enrollment e = new Enrollment();
        e.setStudent(student);
        e.setSchoolClass(schoolClass);
        e.setSection(section);
        e.setAcademicYear(schoolClass.getAcademicYear());
        e.setEnrollmentDate(req.enrollmentDate() != null ? req.enrollmentDate() : LocalDate.now());
        e.setStatus(EnrollmentStatus.ACTIVE);

        return toResponse(enrollmentRepository.save(e));
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findAll(Long classId, Long studentId) {
        List<Enrollment> list;
        if (classId != null) {
            list = enrollmentRepository.findBySchoolClassId(classId);
        } else if (studentId != null) {
            list = enrollmentRepository.findByStudentId(studentId);
        } else {
            list = enrollmentRepository.findAll();
        }
        return list.stream()
                .sorted(Comparator.comparing(Enrollment::getId))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EnrollmentResponse updateStatus(Long id, EnrollmentStatus status) {
        Enrollment e = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
        if (status == EnrollmentStatus.ACTIVE && e.getStatus() != EnrollmentStatus.ACTIVE
                && enrollmentRepository.existsByStudentIdAndSchoolClassIdAndAcademicYearAndStatus(
                        e.getStudent().getId(), e.getSchoolClass().getId(), e.getAcademicYear(), EnrollmentStatus.ACTIVE)) {
            throw new ConflictException("Student is already enrolled in this class for this academic year");
        }
        e.setStatus(status);
        return toResponse(enrollmentRepository.save(e));
    }

    private EnrollmentResponse toResponse(Enrollment e) {
        Student s = e.getStudent();
        SchoolClass c = e.getSchoolClass();
        Section sec = e.getSection();
        return new EnrollmentResponse(
                e.getId(),
                s.getId(), s.getFirstName() + " " + s.getLastName(), s.getStudentNumber(),
                c.getId(), c.getName(),
                sec != null ? sec.getId() : null,
                sec != null ? sec.getName() : null,
                e.getAcademicYear(), e.getEnrollmentDate(), e.getStatus().name());
    }
}
