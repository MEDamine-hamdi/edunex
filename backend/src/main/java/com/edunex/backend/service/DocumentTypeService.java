package com.edunex.backend.service;

import com.edunex.backend.dto.TypeDto;
import com.edunex.backend.entity.DocumentType;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.ConflictException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.DocumentTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DocumentTypeService {

    private static final Set<String> AUDIENCES = Set.of("STUDENT", "PROFESSOR", "BOTH");

    private final DocumentTypeRepository repository;

    @Transactional(readOnly = true)
    public List<TypeDto.View> list() {
        return repository.findAllByOrderByNameAsc().stream().map(this::view).toList();
    }

    @Transactional
    public TypeDto.View create(TypeDto.Save req) {
        return view(repository.save(apply(new DocumentType(), req, null)));
    }

    @Transactional
    public TypeDto.View update(Long id, TypeDto.Save req) {
        return view(repository.save(apply(find(id), req, id)));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(find(id));
    }

    private DocumentType apply(DocumentType t, TypeDto.Save req, Long selfId) {
        if (!AUDIENCES.contains(req.audience())) {
            throw new BadRequestException("Audience must be STUDENT, PROFESSOR or BOTH");
        }
        String name = req.name().trim();
        repository.findByNameIgnoreCase(name).filter(x -> !x.getId().equals(selfId)).ifPresent(x -> {
            throw new ConflictException("This document type already exists");
        });
        t.setName(name);
        t.setAudience(req.audience());
        t.setActive(req.active());
        t.setRequiresDetails(req.requiresDetails());
        t.setMaxCopies(req.maxCopies());
        return t;
    }

    private DocumentType find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Document type not found"));
    }

    private TypeDto.View view(DocumentType t) {
        return new TypeDto.View(t.getId(), t.getName(), t.getAudience(), t.isActive(), t.isRequiresDetails(), t.getMaxCopies());
    }
}