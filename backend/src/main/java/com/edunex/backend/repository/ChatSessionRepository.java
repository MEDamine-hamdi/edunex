package com.edunex.backend.repository;

import com.edunex.backend.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findByStudentIdOrderByUpdatedAtDesc(Long studentId);

    Optional<ChatSession> findByIdAndStudentId(Long id, Long studentId);

    /** General conversations. */
    List<ChatSession> findTop100ByStudentIdAndScopeSubjectIdIsNullOrderByUpdatedAtDesc(Long studentId);

    /** Conversations tied to a whole subject. */
    List<ChatSession> findTop100ByStudentIdAndScopeSubjectIdAndScopeCourseIdIsNullOrderByUpdatedAtDesc(Long studentId, Long subjectId);

    /** Conversations tied to one course. */
    List<ChatSession> findTop100ByStudentIdAndScopeCourseIdOrderByUpdatedAtDesc(Long studentId, Long courseId);
}