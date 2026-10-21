package com.edunex.backend.repository;

import com.edunex.backend.entity.MailOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MailOutboxRepository extends JpaRepository<MailOutbox, Long> {

    List<MailOutbox> findTop20ByStatusAndNextAttemptAtLessThanEqualOrderByIdAsc(String status, LocalDateTime now);

    long countByStatus(String status);

    List<MailOutbox> findTop8ByStatusOrderByIdDesc(String status);
}