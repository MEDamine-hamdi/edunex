package com.edunex.backend.service;

import com.edunex.backend.dto.MessagingDto;
import com.edunex.backend.entity.Conversation;
import com.edunex.backend.entity.ConversationMember;
import com.edunex.backend.entity.ConversationMessage;
import com.edunex.backend.entity.User;
import com.edunex.backend.repository.ConversationMemberRepository;
import com.edunex.backend.repository.ConversationRepository;
import com.edunex.backend.repository.MessagingQueryRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** The two calls the apps make most often (conversation list and unread counter), answered with set-based queries. */
@Service
@RequiredArgsConstructor
public class MessagingFastService {

    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final MessagingQueryRepository queries;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<MessagingDto.ConvRow> conversationList(String email) {
        User me = userRepository.findByEmail(email).orElseThrow(() -> new AccessDeniedException("Unknown user"));
        List<ConversationMember> mine = members.findByUserIdAndActiveTrue(me.getId());
        if (mine.isEmpty()) {
            return List.of();
        }
        List<Long> ids = mine.stream().map(ConversationMember::getConversationId).toList();
        Map<Long, Conversation> byId = conversations.findAllById(ids).stream().collect(Collectors.toMap(Conversation::getId, c -> c));
        Map<Long, List<ConversationMember>> everyone = queries.activeMembers(ids).stream()
                .collect(Collectors.groupingBy(ConversationMember::getConversationId));
        Map<Long, ConversationMessage> last = queries.lastMessages(ids).stream()
                .collect(Collectors.toMap(ConversationMessage::getConversationId, m -> m, (a, b) -> a));
        Map<Long, Long> unread = new HashMap<>();
        for (Object[] row : queries.unreadByConversation(me.getId())) {
            unread.put((Long) row[0], (Long) row[1]);
        }

        Map<Long, Long> otherOf = new HashMap<>();
        Set<Long> otherIds = new HashSet<>();
        for (Conversation c : byId.values()) {
            if ("DIRECT".equals(c.getType())) {
                everyone.getOrDefault(c.getId(), List.of()).stream()
                        .filter(m -> !m.getUserId().equals(me.getId())).findFirst()
                        .ifPresent(m -> {
                            otherOf.put(c.getId(), m.getUserId());
                            otherIds.add(m.getUserId());
                        });
            }
        }
        Map<Long, User> users = userRepository.findAllById(otherIds).stream().collect(Collectors.toMap(User::getId, u -> u));

        List<MessagingDto.ConvRow> out = new ArrayList<>();
        for (ConversationMember m : mine) {
            Conversation c = byId.get(m.getConversationId());
            if (c == null) {
                continue;
            }
            String title = c.getName();
            Long photoUser = null;
            if ("DIRECT".equals(c.getType())) {
                Long otherId = otherOf.get(c.getId());
                User other = otherId == null ? null : users.get(otherId);
                title = other == null ? "?" : other.getFirstName() + " " + other.getLastName();
                photoUser = other == null ? null : other.getId();
            }
            String text = null;
            LocalDateTime at = c.getCreatedAt();
            ConversationMessage lm = last.get(c.getId());
            if (lm != null) {
                at = lm.getCreatedAt();
                text = lm.getDeletedAt() != null ? "Message supprimé"
                        : lm.getContent() != null ? (lm.getContent().length() > 80 ? lm.getContent().substring(0, 80) + "…" : lm.getContent())
                        : ("IMAGE".equals(lm.getFileKind()) ? "Image : " : "Fichier : ") + lm.getFileName();
            }
            out.add(new MessagingDto.ConvRow(c.getId(), c.getType(), title, photoUser, c.getPhotoKey() != null, text, at,
                    unread.getOrDefault(c.getId(), 0L), everyone.getOrDefault(c.getId(), List.of()).size()));
        }
        out.sort(Comparator.comparing(MessagingDto.ConvRow::lastAt, Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())));
        return out;
    }

    @Transactional(readOnly = true)
    public long unread(String email) {
        User me = userRepository.findByEmail(email).orElseThrow(() -> new AccessDeniedException("Unknown user"));
        return queries.totalUnread(me.getId());
    }
}