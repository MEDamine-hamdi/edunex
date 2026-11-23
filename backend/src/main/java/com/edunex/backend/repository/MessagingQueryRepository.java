package com.edunex.backend.repository;

import com.edunex.backend.entity.ConversationMember;
import com.edunex.backend.entity.ConversationMessage;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/** Set-based queries for the conversation list: a few queries in total, whatever the number of conversations. */
public interface MessagingQueryRepository extends Repository<ConversationMessage, Long> {

    @Query("select m from ConversationMessage m where m.id in "
            + "(select max(x.id) from ConversationMessage x where x.conversationId in :ids group by x.conversationId)")
    List<ConversationMessage> lastMessages(@Param("ids") Collection<Long> ids);

    @Query("select cm from ConversationMember cm where cm.conversationId in :ids and cm.active = true")
    List<ConversationMember> activeMembers(@Param("ids") Collection<Long> ids);

    @Query("select m.conversationId, count(m) from ConversationMessage m, ConversationMember cm "
            + "where cm.conversationId = m.conversationId and cm.userId = :uid and cm.active = true "
            + "and m.senderId <> :uid and m.deletedAt is null and m.id > coalesce(cm.lastReadId, 0L) "
            + "group by m.conversationId")
    List<Object[]> unreadByConversation(@Param("uid") Long userId);

    @Query("select count(m) from ConversationMessage m, ConversationMember cm "
            + "where cm.conversationId = m.conversationId and cm.userId = :uid and cm.active = true "
            + "and m.senderId <> :uid and m.deletedAt is null and m.id > coalesce(cm.lastReadId, 0L)")
    long totalUnread(@Param("uid") Long userId);
}