package com.edunex.backend.service;

import com.edunex.backend.dto.RequestDto;
import com.edunex.backend.entity.DocumentRequest;
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
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentRequestService {

    private static final String OTHER = "Autre document";
    private static final List<String> STUDENT_TYPES = List.of("Attestation de scolarité", "Relevé de notes",
            "Attestation de réussite", "Certificat d'assiduité", "Convention de stage",
            "Duplicata de la carte étudiant", OTHER);
    private static final List<String> PROFESSOR_TYPES = List.of("Attestation de travail", "Attestation de salaire",
            "Ordre de mission", OTHER);

    private final DocumentRequestRepository requests;
    private final UserRepository userRepository;
    private final AdministrationAgentRepository agentRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<String> types(String email) {
        return typesFor(requester(email));
    }

    @Transactional
    public RequestDto.View create(String email, RequestDto.Create req) {
        User user = requester(email);
        if (!typesFor(user).contains(req.type())) {
            throw new BadRequestException("Unknown document type");
        }
        if (OTHER.equals(req.type()) && (req.details() == null || req.details().isBlank())) {
            throw new BadRequestException("Please describe the document you need");
        }
        DocumentRequest r = new DocumentRequest();
        r.setRequester(user);
        r.setDocType(req.type());
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

    // ---------- administration ----------

    @Transactional(readOnly = true)
    public List<RequestDto.View> list(RequestStatus status) {
        return (status == null ? requests.findAllByOrderByCreatedAtDesc() : requests.findByStatusOrderByCreatedAtDesc(status))
                .stream().map(this::view).toList();
    }

    @Transactional
    public RequestDto.View update(String agentEmail, Long id, RequestDto.Update req) {
        DocumentRequest r = requests.findById(id).orElseThrow(() -> new ResourceNotFoundException("Request not found"));
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

    // ---------- helpers ----------

    private User requester(String email) {
        User u = userRepository.findByEmail(email).orElseThrow(() -> new AccessDeniedException("Unknown user"));
        if (!(u instanceof Student) && !(u instanceof Professor)) {
            throw new AccessDeniedException("Only students and professors can request documents");
        }
        return u;
    }

    private static List<String> typesFor(User u) {
        return u instanceof Professor ? PROFESSOR_TYPES : STUDENT_TYPES;
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
                u instanceof Professor ? "Professeur" : "Étudiant", u.getEmail(), r.getCreatedAt(), r.getUpdatedAt());
    }
}