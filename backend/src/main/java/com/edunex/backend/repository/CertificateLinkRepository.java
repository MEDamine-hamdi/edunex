package com.edunex.backend.repository;

import com.edunex.backend.entity.CertificateLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CertificateLinkRepository extends JpaRepository<CertificateLink, Long> {

    List<CertificateLink> findByCertificateId(Long certificateId);

    void deleteByCertificateId(Long certificateId);
}