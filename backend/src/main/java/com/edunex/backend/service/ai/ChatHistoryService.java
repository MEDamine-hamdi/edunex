package com.edunex.backend.service.ai;

import com.edunex.backend.dto.ChatMessageResponse;
import com.edunex.backend.dto.ChatReply;
import com.edunex.backend.dto.ChatSessionResponse;
import com.edunex.backend.dto.SourceRef;
import com.edunex.backend.entity.ChatMessage;
import com.edunex.backend.entity.ChatSession;
import com.edunex.backend.entity.Student;
import com.edunex.backend.enums.SenderType;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.ChatAttachmentRepository;
import com.edunex.backend.repository.ChatMessageRepository;
import com.edunex.backend.repository.ChatSessionRepository;
import com.edunex.backend.repository.StudentRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/** Chat sessions and messages. A student can only ever reach their own. */
@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    public record Turn(String role, String content) {
    }

    public record Scope(Long subjectId, Long courseId) {
        public boolean isGeneral() {
            return subjectId == null && courseId == null;
        }
    }

    private static final Pattern CITATION = Pattern.compile("\\s*\\[\\d{1,2}]");
    private static final int MAX_TURN_CHARS = 1500;
    private static final int HISTORY_BUDGET_CHARS = 5000;

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatAttachmentRepository attachmentRepository;

    @Transactional(readOnly = true)
    public List<ChatSessionResponse> sessions(String email, Long subjectId, Long courseId) {
        Long studentId = studentId(email);
        List<ChatSession> list;
        if (courseId != null) {
            list = sessionRepository.findTop100ByStudentIdAndScopeCourseIdOrderByUpdatedAtDesc(studentId, courseId);
        } else if (subjectId != null) {
            list = sessionRepository
                    .findTop100ByStudentIdAndScopeSubjectIdAndScopeCourseIdIsNullOrderByUpdatedAtDesc(studentId, subjectId);
        } else {
            list = sessionRepository.findTop100ByStudentIdAndScopeSubjectIdIsNullOrderByUpdatedAtDesc(studentId);
        }
        return list.stream()
                .map(s -> new ChatSessionResponse(s.getId(), s.getTitle(), s.getCreatedAt(), s.getUpdatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> messages(String email, Long sessionId) {
        ChatSession session = owned(studentId(email), sessionId);
        return messageRepository.findByChatSessionIdOrderByCreatedAtAscIdAsc(session.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(String email, Long sessionId) {
        ChatSession doomed = owned(studentId(email), sessionId);
        attachmentRepository.deleteBySession(doomed.getId());
        sessionRepository.delete(doomed);
    }

    /** The scope a conversation was created with: it never changes afterwards. */
    @Transactional(readOnly = true)
    public Scope scopeOf(Long studentId, Long sessionId) {
        ChatSession s = owned(studentId, sessionId);
        return new Scope(s.getScopeSubjectId(), s.getScopeCourseId());
    }

    /** The latest messages of a session as model turns, within a size budget. Old citation numbers are removed. */
    @Transactional(readOnly = true)
    public List<Turn> recentTurns(Long studentId, Long sessionId) {
        owned(studentId, sessionId);
        List<Turn> kept = new ArrayList<>();
        int used = 0;
        for (ChatMessage m : messageRepository.findTop12ByChatSessionIdOrderByCreatedAtDescIdDesc(sessionId)) {
            boolean user = m.getSender() == SenderType.USER;
            String text = clip(user ? m.getContent() : CITATION.matcher(m.getContent()).replaceAll(""));
            if (used + text.length() > HISTORY_BUDGET_CHARS && !kept.isEmpty()) {
                break;
            }
            kept.add(new Turn(user ? "user" : "assistant", text));
            used += text.length();
        }
        Collections.reverse(kept);
        return kept;
    }

    @Transactional
    public ChatReply saveExchange(Long studentId, Long sessionId, Long subjectId, Long courseId,
                                  String question, String answer, List<SourceRef> sources) {
        ChatSession session;
        if (sessionId == null) {
            session = new ChatSession();
            session.setStudent(studentRepository.getReferenceById(studentId));
            session.setTitle(titleFrom(question));
            session.setScopeSubjectId(subjectId);
            session.setScopeCourseId(courseId);
            session = sessionRepository.save(session);
        } else {
            session = owned(studentId, sessionId);
        }

        ChatMessage user = new ChatMessage();
        user.setChatSession(session);
        user.setSender(SenderType.USER);
        user.setContent(question);
        user = messageRepository.save(user);

        ChatMessage ai = new ChatMessage();
        ai.setChatSession(session);
        ai.setSender(SenderType.AI);
        ai.setContent(answer);
        ai.setSources(encode(sources));
        ai = messageRepository.save(ai);

        session.setUpdatedAt(LocalDateTime.now());
        return new ChatReply(session.getId(), session.getTitle(), toResponse(user), toResponse(ai));
    }

    // ---------- helpers ----------

    private Long studentId(String email) {
        return userRepository.findByEmail(email)
                .filter(u -> u instanceof Student)
                .map(u -> u.getId())
                .orElseThrow(() -> new AccessDeniedException("Student account required"));
    }

    private ChatSession owned(Long studentId, Long sessionId) {
        return sessionRepository.findByIdAndStudentId(sessionId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
    }

    private ChatMessageResponse toResponse(ChatMessage m) {
        return new ChatMessageResponse(m.getId(), m.getSender().name(), m.getContent(), m.getCreatedAt(),
                decode(m.getSources()), m.getAttachmentName());
    }

    private static String clip(String s) {
        return s.length() > MAX_TURN_CHARS ? s.substring(0, MAX_TURN_CHARS) : s;
    }

    private static String titleFrom(String question) {
        String t = question.replaceAll("\\s+", " ").trim();
        return t.length() > 60 ? t.substring(0, 57).trim() + "..." : t;
    }

    private static String flat(String s) {
        return s == null ? "" : s.replaceAll("[\\t\\r\\n]+", " ").trim();
    }

    static String encode(List<SourceRef> refs) {
        if (refs == null || refs.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (SourceRef r : refs) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(r.n()).append('\t').append(r.documentId()).append('\t').append(flat(r.documentTitle()))
                    .append('\t').append(flat(r.courseTitle())).append('\t').append(flat(r.subjectName()))
                    .append('\t').append(flat(r.kind()));
        }
        return sb.toString();
    }

    /** Lines written before sources had a kind (5 fields) are course sources. */
    static List<SourceRef> decode(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<SourceRef> out = new ArrayList<>();
        for (String line : raw.split("\n")) {
            String[] p = line.split("\t", -1);
            if (p.length < 5) {
                continue;
            }
            try {
                String kind = p.length >= 6 && !p[5].isBlank() ? p[5] : "COURSE";
                out.add(new SourceRef(Integer.parseInt(p[0]), Long.parseLong(p[1]), p[2], p[3], p[4], kind));
            } catch (NumberFormatException ignored) {
                // skip a malformed line
            }
        }
        return out;
    }
}