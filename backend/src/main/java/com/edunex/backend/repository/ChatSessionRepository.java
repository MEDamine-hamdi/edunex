package com.edunex.backend.repository;

import com.edunex.backend.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByStudentIdOrderByUpdatedAtDesc(Long studentId);
}
