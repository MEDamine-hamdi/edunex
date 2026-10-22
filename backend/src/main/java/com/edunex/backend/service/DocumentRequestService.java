package com.edunex.backend.service;

import com.edunex.backend.dto.RequestDto;
import com.edunex.backend.dto.TypeDto;
import com.edunex.backend.entity.DocumentRequest;
import com.edunex.backend.entity.DocumentType;
import com.edunex.backend.entity.Professor;
import com.edunex.backend.entity.Student;
import com.edunex.backend.entity.User;
import com.edunex.backend.enums.NotificationType;
import com.edunex.backend.enums.RequestStatus;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.ConflictException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.AdministrationAgentRepository;
import com.edunex.backend.repository.DocumentRequestRepository;
import com.edunex.backend.repository.DocumentTypeRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DocumentRequestService {

    public record Download(Resource resource, String filename) {
    }

    private static final Set<String> FILE_TYPES = Set.of("pdf", "doc", "docx", "xls", "xlsx", "odt", "txt", "png", "jpg", "jpeg");
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    private final DocumentRequestRepository requests;
    private final UserRepository userRepository;
    private final AdministrationAgentRepository agentRepository;
    private final NotificationService notificationService;
    private final DocumentTypeRepository typeRepository;
    private final FileStorage fileStorage;

    // ---------- requester (student or professor) ----------

    @Transactional(readOnly = true)
    public List<String> types(String email) {
        return options(email).stream().map(TypeDto.Option::name).toList();
    }

    @Transactional(readOnly = true)
    public List<TypeDto.Option> options(String email) {
        User u = requester(email);
        return typeRepository.findAllByOrderByNameAsc().stream()
                .filter(DocumentType::isActive)
                .filter(t -> matches(t, u))
                .map(t -> new TypeDto.Option(t.getName(), t.isRequiresDetails(), t.getMaxCopies()))
                .toList();
    }

    @Transactional
    public RequestDto.View create(String email, RequestDto.Create req) {
        User user = requester(email);
        DocumentType type = typeRepository.findByNameIgnoreCase(req.type())
                .filter(DocumentType::isActive)
                .filter(t -> matches(t, user))
                .orElseThrow(() -> new BadRequestException("Unknown document type"));
        if (req.copies() > type.getMaxCopies()) {
            throw new BadRequestException("Too many copies for this document");
        }
        if (type.isRequiresDetails() && (req.details() == null || req.details().isBlank())) {
            throw new BadRequestException("Please describe the document you need");
        }
        DocumentRequest r = new DocumentRequest();
        r.setRequester(user);
        r.setDocType(type.getName());
        r.setCopies(req.copies());
        r.setReason(req.reason().trim());
        r.setDetails(req.details() == null || req.details().isBlank() ? null : req.details().trim());
        r = requests.save(r);
        notificationService.notifyUsers(agentRepository.findAll(), NotificationType.DOCUMENT_REQUEST,
                "Nouvelle demande : " + r.getDocType() + " (" + user.getFirstName() + " " + user.getLastName() + ")");
        return view(r);
    }

    @Transactional(readOnly = true)
    public List<RequestDto.View> mine(String email) {
        return requests.findByRequesterIdOrderByCreatedAtDesc(requester(email).getId()).stream().map(this::view).toList();
    }

    @Transactional
    public void withdraw(String email, Long id) {
        User user = requester(email);
        DocumentRequest r = requests.findById(id)
                .filter(x -> x.getRequester().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        if (r.getStatus() != RequestStatus.FILED) {
            throw new ConflictException("A request that is being processed can no longer be withdrawn");
        }
        requests.delete(r);
    }

    @Transactional(readOnly = true)
    public Download downloadOwn(String email, Long id) {
        User user = requester(email);
        DocumentRequest r = requests.findById(id)
                .filter(x -> x.getRequester().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
        return download(r);
    }

    // ---------- administration ----------

    @Transactional(readOnly = true)
    public List<RequestDto.View> list(RequestStatus status) {
        return (status == null ? requests.findAllByOrderByCreatedAtDesc() : requests.findByStatusOrderByCreatedAtDesc(status))
                .stream().map(this::view).toList();
    }

    @Transactional
    public RequestDto.View update(String agentEmail, Long id, RequestDto.Update req) {
        DocumentRequest r = find(id);
        User agent = userRepository.findByEmail(agentEmail).orElseThrow(() -> new AccessDeniedException("Unknown user"));
        boolean changed = r.getStatus() != req.status();
        String note = req.note() == null || req.note().isBlank() ? null : req.note().trim();
        r.setStatus(req.status());
        r.setAdminNote(note);
        r.setHandledBy(agent.getFirstName() + " " + agent.getLastName());
        requests.save(r);
        if (changed) {
            notificationService.notifyUsers(List.of(r.getRequester()), NotificationType.DOCUMENT_REQUEST,
                    "Votre demande « " + r.getDocType() + " » est maintenant : " + label(req.status()) + "."
                            + (note != null ? " Note de l'administration : " + note : ""));
        }
        return view(r);
    }

    /** Sends the document through the app: the requester can download it, and the request becomes ready. */
    @Transactional
    public RequestDto.View attachFile(String agentEmail, Long id, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BadRequestException("File is too large (10 MB maximum)");
        }
        String name = displayName(file);
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!FILE_TYPES.contains(ext)) {
            throw new BadRequestException("Unsupported file type. Use PDF, DOC, DOCX, XLS, XLSX, ODT, TXT, PNG or JPG");
        }
        DocumentRequest r = find(id);
        User agent = userRepository.findByEmail(agentEmail).orElseThrow(() -> new AccessDeniedException("Unknown user"));
        String oldKey = r.getFileKey();
        r.setFileKey(fileStorage.store(file));
        r.setFileName(name);
        r.setFileSize(file.getSize());
        r.setDeliveredAt(LocalDateTime.now());
        r.setStatus(RequestStatus.READY_FOR_PICKUP);
        r.setHandledBy(agent.getFirstName() + " " + agent.getLastName());
        requests.save(r);
        if (oldKey != null) {
            fileStorage.delete(oldKey);
        }
        notificationService.notifyUsers(List.of(r.getRequester()), NotificationType.DOCUMENT_REQUEST,
                "Votre demande « " + r.getDocType() + " » est prête : le document est disponible au téléchargement"
                        + " dans la rubrique Demandes de documents.");
        return view(r);
    }

    @Transactional
    public RequestDto.View removeFile(Long id) {
        DocumentRequest r = find(id);
        if (r.getFileKey() != null) {
            fileStorage.delete(r.getFileKey());
            r.setFileKey(null);
            r.setFileName(null);
            r.setFileSize(null);
            r.setDeliveredAt(null);
            requests.save(r);
        }
        return view(r);
    }

    @Transactional(readOnly = true)
    public Download downloadAsAgent(Long id) {
        return download(find(id));
    }

    // ---------- helpers ----------

    private Download download(DocumentRequest r) {
        if (r.getFileKey() == null) {
            throw new ResourceNotFoundException("No document has been sent for this request yet");
        }
        return new Download(fileStorage.load(r.getFileKey()), r.getFileName());
    }

    private DocumentRequest find(Long id) {
        return requests.findById(id).orElseThrow(() -> new ResourceNotFoundException("Request not found"));
    }

    private User requester(String email) {
        User u = userRepository.findByEmail(email).orElseThrow(() -> new AccessDeniedException("Unknown user"));
        if (!(u instanceof Student) && !(u instanceof Professor)) {
            throw new AccessDeniedException("Only students and professors can request documents");
        }
        return u;
    }

    private static boolean matches(DocumentType t, User u) {
        String a = t.getAudience();
        return "BOTH".equals(a) || (u instanceof Professor ? "PROFESSOR".equals(a) : "STUDENT".equals(a));
    }

    private static String displayName(MultipartFile file) {
        String original = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        String name = original.substring(Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1).trim();
        return name.isEmpty() ? "document" : (name.length() > 200 ? name.substring(name.length() - 200) : name);
    }

    private static String label(RequestStatus s) {
        return switch (s) {
            case FILED -> "Déposée";
            case IN_PROGRESS -> "En cours de traitement";
            case READY_FOR_PICKUP -> "Prête pour retrait";
        };
    }

    private RequestDto.View view(DocumentRequest r) {
        User u = r.getRequester();
        return new RequestDto.View(r.getId(), r.getDocType(), r.getCopies(), r.getReason(), r.getDetails(),
                r.getStatus().name(), r.getAdminNote(), r.getHandledBy(), u.getFirstName() + " " + u.getLastName(),
                u instanceof Professor ? "Professeur" : "Étudiant", u.getEmail(), r.getCreatedAt(), r.getUpdatedAt(),
                r.getFileKey() != null, r.getFileName());
    }
}