package com.edunex.backend.repository;

import com.edunex.backend.entity.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    List<ConversationMessage> findTop50ByConversationIdOrderByIdDesc(Long conversationId);

    List<ConversationMessage> findTop50ByConversationIdAndIdLessThanOrderByIdDesc(Long conversationId, Long id);

    List<ConversationMessage> findByConversationIdAndUpdatedAtGreaterThanEqualOrderByIdAsc(Long conversationId, LocalDateTime since);

    Optional<ConversationMessage> findTopByConversationIdOrderByIdDesc(Long conversationId);

    long countByConversationIdAndIdGreaterThanAndSenderIdNotAndDeletedAtIsNull(Long conversationId, Long id, Long senderId);
}