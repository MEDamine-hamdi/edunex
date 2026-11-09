package com.edunex.backend.service;

import com.edunex.backend.dto.MessagingDto.Contact;
import com.edunex.backend.dto.MessagingDto.ConvDetail;
import com.edunex.backend.dto.MessagingDto.ConvRow;
import com.edunex.backend.dto.MessagingDto.Member;
import com.edunex.backend.dto.MessagingDto.MessagePage;
import com.edunex.backend.dto.MessagingDto.MessageView;
import com.edunex.backend.entity.AdministrationAgent;
import com.edunex.backend.entity.Conversation;
import com.edunex.backend.entity.ConversationMember;
import com.edunex.backend.entity.ConversationMessage;
import com.edunex.backend.entity.Professor;
import com.edunex.backend.entity.Student;
import com.edunex.backend.entity.Subject;
import com.edunex.backend.entity.User;
import com.edunex.backend.enums.EnrollmentStatus;
import com.edunex.backend.enums.UserStatus;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.exception.TooManyRequestsException;
import com.edunex.backend.repository.AdministrationAgentRepository;
import com.edunex.backend.repository.ConversationMemberRepository;
import com.edunex.backend.repository.ConversationMessageRepository;
import com.edunex.backend.repository.ConversationRepository;
import com.edunex.backend.repository.EnrollmentRepository;
import com.edunex.backend.repository.ProfessorRepository;
import com.edunex.backend.repository.ProfileRepository;
import com.edunex.backend.repository.SchoolClassRepository;
import com.edunex.backend.repository.StudentRepository;
import com.edunex.backend.repository.SubjectRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaTypeFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** Private conversations and groups between the people who are allowed to talk to each other. */
@Service
@RequiredArgsConstructor
public class MessagingService {

    public record FileData(Resource resource, String filename, String contentType) {
    }

    private static final Set<String> IMAGES = Set.of("png", "jpg", "jpeg", "webp", "gif");
    private static final Set<String> DOCS = Set.of("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "odt", "odp", "txt", "csv");
    private static final long MAX_FILE = 10L * 1024 * 1024;
    private static final long MAX_PHOTO = 2L * 1024 * 1024;

    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final ConversationMessageRepository messages;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ProfessorRepository professorRepository;
    private final AdministrationAgentRepository agentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SubjectRepository subjectRepository;
    private final SchoolClassRepository classRepository;
    private final StudentAccessService studentAccessService;
    private final ProfileRepository profileRepository;
    private final FileStorage fileStorage;
    private final Map<Long, Deque<Long>> sendLog = new ConcurrentHashMap<>();

    // ---------- who may talk to whom ----------

    private Map<Long, User> contacts(User me) {
        Map<Long, User> out = new LinkedHashMap<>();
        if (me instanceof Student) {
            Set<Long> classIds = enrollmentRepository.findByStudentIdAndStatus(me.getId(), EnrollmentStatus.ACTIVE).stream()
                    .map(e -> e.getSchoolClass().getId()).collect(Collectors.toSet());
            if (!classIds.isEmpty()) {
                enrollmentRepository.findBySchoolClassIdInAndStatus(classIds, EnrollmentStatus.ACTIVE)
                        .forEach(e -> out.putIfAbsent(e.getStudent().getId(), e.getStudent()));
            }
            subjectRepository.findAllById(studentAccessService.accessibleSubjectIds(me.getId()))
                    .forEach(s -> s.getProfessors().forEach(p -> out.putIfAbsent(p.getId(), p)));
        } else if (me instanceof Professor) {
            Set<Long> classIds = new HashSet<>();
            for (Subject s : subjectRepository.findByProfessorsId(me.getId())) {
                classRepository.findBySubjectsId(s.getId()).forEach(c -> classIds.add(c.getId()));
            }
            if (!classIds.isEmpty()) {
                enrollmentRepository.findBySchoolClassIdInAndStatus(classIds, EnrollmentStatus.ACTIVE)
                        .forEach(e -> out.putIfAbsent(e.getStudent().getId(), e.getStudent()));
            }
            professorRepository.findAll().forEach(p -> out.putIfAbsent(p.getId(), p));
        } else if (me instanceof AdministrationAgent) {
            studentRepository.findAll().forEach(s -> out.putIfAbsent(s.getId(), s));
            professorRepository.findAll().forEach(p -> out.putIfAbsent(p.getId(), p));
            agentRepository.findAll().forEach(a -> out.putIfAbsent(a.getId(), a));
        }
        out.remove(me.getId());
        out.values().removeIf(u -> u.getStatus() != UserStatus.ACTIVE);
        return out;
    }

    @Transactional(readOnly = true)
    public List<Contact> contactList(String email, String q) {
        User me = user(email);
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        return contacts(me).values().stream()
                .filter(u -> needle.isEmpty() || name(u).toLowerCase(Locale.ROOT).contains(needle))
                .sorted(Comparator.comparing(User::getLastName).thenComparing(User::getFirstName))
                .limit(60)
                .map(u -> new Contact(u.getId(), name(u), roleLabel(u)))
                .toList();
    }

    // ---------- conversations ----------

    @Transactional(readOnly = true)
    public List<ConvRow> conversationList(String email) {
        User me = user(email);
        List<ConvRow> out = new ArrayList<>();
        for (ConversationMember m : members.findByUserIdAndActiveTrue(me.getId())) {
            Conversation c = conversations.findById(m.getConversationId()).orElse(null);
            if (c != null) {
                out.add(row(me, c, m));
            }
        }
        out.sort(Comparator.comparing(ConvRow::lastAt, Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())));
        return out;
    }

    @Transactional
    public ConvDetail direct(String email, Long otherId) {
        User me = user(email);
        User other = contacts(me).get(otherId);
        if (other == null) {
            throw new AccessDeniedException("You cannot message this person");
        }
        String key = Math.min(me.getId(), otherId) + "-" + Math.max(me.getId(), otherId);
        Conversation c = conversations.findByDirectKey(key).orElseGet(() -> {
            Conversation n = new Conversation();
            n.setType("DIRECT");
            n.setDirectKey(key);
            n.setCreatedById(me.getId());
            return conversations.save(n);
        });
        join(c.getId(), me.getId(), false);
        join(c.getId(), other.getId(), false);
        return detail(me, c);
    }

    @Transactional
    public ConvDetail group(String email, String name, List<Long> memberIds) {
        User me = user(email);
        Map<Long, User> allowed = contacts(me);
        Set<Long> ids = new HashSet<>(memberIds);
        ids.remove(me.getId());
        if (ids.isEmpty()) {
            throw new BadRequestException("Choose at least one person");
        }
        for (Long id : ids) {
            if (!allowed.containsKey(id)) {
                throw new AccessDeniedException("You cannot add this person");
            }
        }
        Conversation c = new Conversation();
        c.setType("GROUP");
        c.setName(name.trim());
        c.setCreatedById(me.getId());
        c = conversations.save(c);
        join(c.getId(), me.getId(), true);
        for (Long id : ids) {
            join(c.getId(), id, false);
        }
        return detail(me, c);
    }

    @Transactional(readOnly = true)
    public ConvDetail detail(String email, Long id) {
        User me = user(email);
        requireMember(id, me);
        return detail(me, conversation(id));
    }

    @Transactional
    public ConvDetail rename(String email, Long id, String name) {
        User me = user(email);
        ConversationMember m = requireMember(id, me);
        Conversation c = groupOf(id);
        requireAdmin(m);
        c.setName(name.trim());
        return detail(me, c);
    }

    @Transactional
    public ConvDetail setPhoto(String email, Long id, MultipartFile file) {
        User me = user(email);
        requireAdmin(requireMember(id, me));
        Conversation c = groupOf(id);
        String name = displayName(file);
        String ext = extension(name);
        if (file.isEmpty() || file.getSize() > MAX_PHOTO || !Set.of("png", "jpg", "jpeg", "webp").contains(ext) || !signatureOk(ext, file)) {
            throw new BadRequestException("The picture must be a PNG, JPG or WEBP image of 2 MB maximum");
        }
        String old = c.getPhotoKey();
        c.setPhotoKey(fileStorage.store(file));
        if (old != null) {
            fileStorage.delete(old);
        }
        return detail(me, c);
    }

    @Transactional
    public ConvDetail removePhoto(String email, Long id) {
        User me = user(email);
        requireAdmin(requireMember(id, me));
        Conversation c = groupOf(id);
        if (c.getPhotoKey() != null) {
            fileStorage.delete(c.getPhotoKey());
            c.setPhotoKey(null);
        }
        return detail(me, c);
    }

    @Transactional
    public ConvDetail addMembers(String email, Long id, List<Long> userIds) {
        User me = user(email);
        requireAdmin(requireMember(id, me));
        Conversation c = groupOf(id);
        Map<Long, User> allowed = contacts(me);
        for (Long uid : new HashSet<>(userIds)) {
            if (!allowed.containsKey(uid)) {
                throw new AccessDeniedException("You cannot add this person");
            }
            join(id, uid, false);
        }
        return detail(me, c);
    }

    /** An admin removes someone, or a person leaves (userId = themselves). */
    @Transactional
    public void removeMember(String email, Long id, Long userId) {
        User me = user(email);
        ConversationMember mine = requireMember(id, me);
        groupOf(id);
        if (!me.getId().equals(userId)) {
            requireAdmin(mine);
        }
        ConversationMember target = members.findByConversationIdAndUserId(id, userId)
                .filter(ConversationMember::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        target.setActive(false);
        target.setAdmin(false);
        List<ConversationMember> left = members.findByConversationIdAndActiveTrue(id);
        if (!left.isEmpty() && left.stream().noneMatch(ConversationMember::isAdmin)) {
            left.stream().min(Comparator.comparing(ConversationMember::getId)).ifPresent(x -> x.setAdmin(true));
        }
    }

    // ---------- messages ----------

    @Transactional(readOnly = true)
    public MessagePage messageList(String email, Long convId, LocalDateTime since, Long before) {
        User me = user(email);
        requireMember(convId, me);
        LocalDateTime now = LocalDateTime.now();
        List<ConversationMessage> list;
        if (since != null) {
            list = messages.findByConversationIdAndUpdatedAtGreaterThanEqualOrderByIdAsc(convId, since.minusSeconds(2));
        } else {
            list = new ArrayList<>(before != null
                    ? messages.findTop50ByConversationIdAndIdLessThanOrderByIdDesc(convId, before)
                    : messages.findTop50ByConversationIdOrderByIdDesc(convId));
            Collections.reverse(list);
        }
        return new MessagePage(views(list), now);
    }

    @Transactional
    public MessageView sendText(String email, Long convId, String content) {
        return persist(user(email), convId, content, null);
    }

    @Transactional
    public MessageView sendFile(String email, Long convId, String content, MultipartFile file) {
        return persist(user(email), convId, content, file);
    }

    @Transactional
    public MessageView delete(String email, Long messageId) {
        User me = user(email);
        ConversationMessage m = messages.findById(messageId).orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        ConversationMember member = requireMember(m.getConversationId(), me);
        Conversation c = conversation(m.getConversationId());
        boolean own = m.getSenderId().equals(me.getId());
        if (!own && !("GROUP".equals(c.getType()) && member.isAdmin())) {
            throw new AccessDeniedException("You cannot delete this message");
        }
        if (m.getDeletedAt() == null) {
            if (m.getFileKey() != null) {
                fileStorage.delete(m.getFileKey());
            }
            m.setContent(null);
            m.setFileKey(null);
            m.setFileName(null);
            m.setFileKind(null);
            m.setFileType(null);
            m.setFileSize(null);
            m.setDeletedAt(LocalDateTime.now());
            m.setDeletedBy(me.getId());
            m.setUpdatedAt(LocalDateTime.now());
        }
        return views(List.of(m)).get(0);
    }

    @Transactional
    public void markRead(String email, Long convId) {
        User me = user(email);
        ConversationMember m = requireMember(convId, me);
        messages.findTopByConversationIdOrderByIdDesc(convId).ifPresent(last -> m.setLastReadId(last.getId()));
    }

    @Transactional(readOnly = true)
    public long unread(String email) {
        User me = user(email);
        long total = 0;
        for (ConversationMember m : members.findByUserIdAndActiveTrue(me.getId())) {
            total += messages.countByConversationIdAndIdGreaterThanAndSenderIdNotAndDeletedAtIsNull(
                    m.getConversationId(), m.getLastReadId() == null ? 0L : m.getLastReadId(), me.getId());
        }
        return total;
    }

    // ---------- files and pictures ----------

    @Transactional(readOnly = true)
    public FileData messageFile(String email, Long messageId) {
        User me = user(email);
        ConversationMessage m = messages.findById(messageId).orElseThrow(() -> new ResourceNotFoundException("Message not found"));
        requireMember(m.getConversationId(), me);
        if (m.getFileKey() == null) {
            throw new ResourceNotFoundException("No file");
        }
        return new FileData(fileStorage.load(m.getFileKey()), m.getFileName(), m.getFileType());
    }

    /** A person's profile picture: yours, or someone you share a conversation with. */
    @Transactional(readOnly = true)
    public FileData userPhoto(String email, Long userId) {
        User me = user(email);
        if (!me.getId().equals(userId)) {
            Set<Long> mine = members.findByUserIdAndActiveTrue(me.getId()).stream().map(ConversationMember::getConversationId).collect(Collectors.toSet());
            boolean shared = members.findByUserIdAndActiveTrue(userId).stream().anyMatch(m -> mine.contains(m.getConversationId()));
            if (!shared) {
                throw new AccessDeniedException("Not allowed");
            }
        }
        String key = profileRepository.findByUserId(userId).map(p -> p.getPhotoUrl()).orElse(null);
        if (key == null) {
            throw new ResourceNotFoundException("No photo");
        }
        return new FileData(fileStorage.load(key), Paths.get(key).getFileName().toString(), null);
    }

    @Transactional(readOnly = true)
    public FileData groupPhoto(String email, Long convId) {
        User me = user(email);
        requireMember(convId, me);
        String key = conversation(convId).getPhotoKey();
        if (key == null) {
            throw new ResourceNotFoundException("No picture");
        }
        return new FileData(fileStorage.load(key), Paths.get(key).getFileName().toString(), null);
    }

    // ---------- helpers ----------

    private MessageView persist(User me, Long convId, String content, MultipartFile file) {
        ConversationMember member = requireMember(convId, me);
        rate(me.getId());
        ConversationMessage m = new ConversationMessage();
        m.setConversationId(convId);
        m.setSenderId(me.getId());
        if (content != null && !content.isBlank()) {
            m.setContent(content.trim());
        }
        if (file != null && !file.isEmpty()) {
            String name = displayName(file);
            String ext = extension(name);
            boolean image = IMAGES.contains(ext);
            if (file.getSize() > MAX_FILE || !(image || DOCS.contains(ext)) || !signatureOk(ext, file)) {
                throw new BadRequestException("Only pictures and documents are allowed (10 MB maximum)");
            }
            m.setFileKey(fileStorage.store(file));
            m.setFileName(name);
            m.setFileKind(image ? "IMAGE" : "DOCUMENT");
            m.setFileType(file.getContentType());
            m.setFileSize(file.getSize());
        }
        if (m.getContent() == null && m.getFileKey() == null) {
            throw new BadRequestException("The message is empty");
        }
        m = messages.save(m);
        conversation(convId).setUpdatedAt(LocalDateTime.now());
        member.setLastReadId(m.getId());
        return views(List.of(m)).get(0);
    }

    private void rate(Long userId) {
        Deque<Long> q = sendLog.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (q) {
            long now = System.currentTimeMillis();
            while (!q.isEmpty() && q.peekFirst() < now - 60_000) {
                q.pollFirst();
            }
            if (q.size() >= 60) {
                throw new TooManyRequestsException("Too many messages. Slow down.", 10);
            }
            q.addLast(now);
        }
    }

    private List<MessageView> views(List<ConversationMessage> list) {
        Map<Long, User> users = userRepository.findAllById(list.stream().map(ConversationMessage::getSenderId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, u -> u));
        return list.stream().map(m -> {
            boolean deleted = m.getDeletedAt() != null;
            User s = users.get(m.getSenderId());
            return new MessageView(m.getId(), m.getConversationId(), m.getSenderId(), s == null ? "?" : name(s),
                    deleted ? null : m.getContent(), m.getCreatedAt(), deleted,
                    deleted && m.getDeletedBy() != null && !m.getDeletedBy().equals(m.getSenderId()),
                    m.getFileKey() != null, m.getFileName(), m.getFileKind(), m.getFileType(), m.getFileSize());
        }).toList();
    }

    private ConvRow row(User me, Conversation c, ConversationMember mine) {
        String title = c.getName();
        Long photoUser = null;
        if ("DIRECT".equals(c.getType())) {
            User other = otherOf(c.getId(), me.getId());
            title = other == null ? "?" : name(other);
            photoUser = other == null ? null : other.getId();
        }
        String last = null;
        LocalDateTime lastAt = c.getCreatedAt();
        ConversationMessage lm = messages.findTopByConversationIdOrderByIdDesc(c.getId()).orElse(null);
        if (lm != null) {
            lastAt = lm.getCreatedAt();
            last = lm.getDeletedAt() != null ? "Message supprimé"
                    : lm.getContent() != null ? (lm.getContent().length() > 80 ? lm.getContent().substring(0, 80) + "…" : lm.getContent())
                    : ("IMAGE".equals(lm.getFileKind()) ? "Image : " : "Fichier : ") + lm.getFileName();
        }
        long unread = messages.countByConversationIdAndIdGreaterThanAndSenderIdNotAndDeletedAtIsNull(
                c.getId(), mine.getLastReadId() == null ? 0L : mine.getLastReadId(), me.getId());
        return new ConvRow(c.getId(), c.getType(), title, photoUser, c.getPhotoKey() != null, last, lastAt, unread,
                members.findByConversationIdAndActiveTrue(c.getId()).size());
    }

    private ConvDetail detail(User me, Conversation c) {
        ConversationMember mine = members.findByConversationIdAndUserId(c.getId(), me.getId()).orElse(null);
        List<ConversationMember> active = members.findByConversationIdAndActiveTrue(c.getId());
        Map<Long, User> users = userRepository.findAllById(active.stream().map(ConversationMember::getUserId).toList())
                .stream().collect(Collectors.toMap(User::getId, u -> u));
        List<Member> list = active.stream().filter(m -> users.containsKey(m.getUserId()))
                .map(m -> new Member(m.getUserId(), name(users.get(m.getUserId())), roleLabel(users.get(m.getUserId())), m.isAdmin()))
                .sorted(Comparator.comparing(Member::name)).toList();
        String title = c.getName();
        Long photoUser = null;
        if ("DIRECT".equals(c.getType())) {
            User other = otherOf(c.getId(), me.getId());
            title = other == null ? "?" : name(other);
            photoUser = other == null ? null : other.getId();
        }
        return new ConvDetail(c.getId(), c.getType(), title, photoUser, c.getPhotoKey() != null,
                mine != null && mine.isAdmin(), list);
    }

    private User otherOf(Long convId, Long myId) {
        return members.findByConversationIdAndActiveTrue(convId).stream()
                .filter(m -> !m.getUserId().equals(myId)).findFirst()
                .flatMap(m -> userRepository.findById(m.getUserId())).orElse(null);
    }

    private void join(Long convId, Long userId, boolean admin) {
        ConversationMember m = members.findByConversationIdAndUserId(convId, userId).orElseGet(() -> {
            ConversationMember n = new ConversationMember();
            n.setConversationId(convId);
            n.setUserId(userId);
            return n;
        });
        m.setActive(true);
        if (admin) {
            m.setAdmin(true);
        }
        members.save(m);
    }

    private ConversationMember requireMember(Long convId, User me) {
        return members.findByConversationIdAndUserId(convId, me.getId()).filter(ConversationMember::isActive)
                .orElseThrow(() -> new AccessDeniedException("You are not in this conversation"));
    }

    private static void requireAdmin(ConversationMember m) {
        if (!m.isAdmin()) {
            throw new AccessDeniedException("Only a group admin can do this");
        }
    }

    private Conversation groupOf(Long id) {
        Conversation c = conversation(id);
        if (!"GROUP".equals(c.getType())) {
            throw new BadRequestException("This is not a group");
        }
        return c;
    }

    private Conversation conversation(Long id) {
        return conversations.findById(id).orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
    }

    private User user(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new AccessDeniedException("Unknown user"));
    }

    private static String name(User u) {
        return u.getFirstName() + " " + u.getLastName();
    }

    private static String roleLabel(User u) {
        return u instanceof Student ? "Étudiant" : u instanceof Professor ? "Professeur" : "Administration";
    }

    private static String displayName(MultipartFile f) {
        String original = f.getOriginalFilename() == null ? "file" : f.getOriginalFilename();
        String n = original.substring(Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1).trim();
        return n.isEmpty() ? "file" : (n.length() > 200 ? n.substring(n.length() - 200) : n);
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** The first bytes must match the extension (a "png" must really be a PNG). */
    private static boolean signatureOk(String ext, MultipartFile file) {
        byte[] h = new byte[512];
        int n;
        try (InputStream in = file.getInputStream()) {
            n = in.readNBytes(h, 0, h.length);
        } catch (IOException e) {
            return false;
        }
        switch (ext) {
            case "png":
                return n >= 4 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
            case "jpg":
            case "jpeg":
                return n >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
            case "gif":
                return n >= 4 && h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8';
            case "webp":
                return n >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F' && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
            case "pdf":
                return n >= 4 && h[0] == '%' && h[1] == 'P' && h[2] == 'D' && h[3] == 'F';
            case "docx":
            case "xlsx":
            case "pptx":
            case "odt":
            case "odp":
                return n >= 4 && h[0] == 'P' && h[1] == 'K' && h[2] == 3 && h[3] == 4;
            case "doc":
            case "xls":
            case "ppt":
                return n >= 4 && (h[0] & 0xFF) == 0xD0 && (h[1] & 0xFF) == 0xCF && (h[2] & 0xFF) == 0x11 && (h[3] & 0xFF) == 0xE0;
            case "txt":
            case "csv":
                for (int i = 0; i < n; i++) {
                    if (h[i] == 0) {
                        return false;
                    }
                }
                return true;
            default:
                return false;
        }
    }

    public static String contentType(FileData d) {
        if (d.contentType() != null && !d.contentType().isBlank()) {
            return d.contentType();
        }
        return MediaTypeFactory.getMediaType(d.filename()).map(Object::toString).orElse("application/octet-stream");
    }
}