package com.edunex.backend.repository;

import com.edunex.backend.entity.DocumentRequest;
import com.edunex.backend.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRequestRepository extends JpaRepository<DocumentRequest, Long> {

    List<DocumentRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    List<DocumentRequest> findAllByOrderByCreatedAtDesc();

    List<DocumentRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);
}