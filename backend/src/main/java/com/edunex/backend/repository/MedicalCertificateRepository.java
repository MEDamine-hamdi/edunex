package com.edunex.backend.repository;

import com.edunex.backend.entity.MedicalCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalCertificateRepository extends JpaRepository<MedicalCertificate, Long> {

    List<MedicalCertificate> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<MedicalCertificate> findAllByOrderByCreatedAtDesc();

    List<MedicalCertificate> findByStatusOrderByCreatedAtAsc(String status);
}