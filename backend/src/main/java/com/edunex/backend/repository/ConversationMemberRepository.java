package com.edunex.backend.repository;

import com.edunex.backend.entity.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {

    List<ConversationMember> findByUserIdAndActiveTrue(Long userId);

    List<ConversationMember> findByConversationIdAndActiveTrue(Long conversationId);

    Optional<ConversationMember> findByConversationIdAndUserId(Long conversationId, Long userId);
}