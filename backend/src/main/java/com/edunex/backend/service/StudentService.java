package com.edunex.backend.service;

import com.edunex.backend.dto.CreateStudentRequest;
import com.edunex.backend.dto.StudentResponse;
import com.edunex.backend.entity.Student;
import com.edunex.backend.enums.UserStatus;
import com.edunex.backend.exception.ConflictException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.RoleRepository;
import com.edunex.backend.repository.StudentRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public StudentResponse create(CreateStudentRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ConflictException("Email already in use");
        }
        if (studentRepository.existsByStudentNumber(req.studentNumber())) {
            throw new ConflictException("Student number already in use");
        }

        Student s = new Student();
        s.setFirstName(req.firstName());
        s.setLastName(req.lastName());
        s.setEmail(req.email());
        s.setPassword(passwordEncoder.encode(req.password()));
        s.setPhone(req.phone());
        s.setStudentNumber(req.studentNumber());
        s.setEnrollmentYear(req.enrollmentYear());
        s.setLevel(req.level());
        s.setSpecialty(req.specialty());
        s.setRole(roleRepository.findByName("ROLE_STUDENT")
                .orElseThrow(() -> new ResourceNotFoundException("Role ROLE_STUDENT not found")));

        return toResponse(studentRepository.save(s));
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> findAll() {
        return studentRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public StudentResponse updateStatus(Long id, UserStatus status) {
        Student s = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        s.setStatus(status);
        return toResponse(studentRepository.save(s));
    }

    private StudentResponse toResponse(Student s) {
        return new StudentResponse(s.getId(), s.getFirstName(), s.getLastName(), s.getEmail(),
                s.getPhone(), s.getStudentNumber(), s.getEnrollmentYear(), s.getLevel(),
                s.getSpecialty(), s.getStatus().name());
    }
}
