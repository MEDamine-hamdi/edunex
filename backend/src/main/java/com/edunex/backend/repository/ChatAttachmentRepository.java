package com.edunex.backend.repository;

import com.edunex.backend.entity.ChatAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatAttachmentRepository extends JpaRepository<ChatAttachment, Long> {

    List<ChatAttachment> findTop3ByChatSessionIdOrderByIdDesc(Long chatSessionId);

    @Modifying
    @Query("delete from ChatAttachment a where a.chatSessionId = :sessionId")
    void deleteBySession(@Param("sessionId") Long sessionId);
}