package com.edunex.backend.repository;

import com.edunex.backend.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatSessionIdOrderByCreatedAtAsc(Long chatSessionId);

    List<ChatMessage> findByChatSessionIdOrderByCreatedAtAscIdAsc(Long chatSessionId);

    List<ChatMessage> findTop12ByChatSessionIdOrderByCreatedAtDescIdDesc(Long chatSessionId);
}