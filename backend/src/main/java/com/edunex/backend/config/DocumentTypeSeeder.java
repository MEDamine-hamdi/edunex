package com.edunex.backend.config;

import com.edunex.backend.entity.DocumentType;
import com.edunex.backend.repository.DocumentTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** First start only: the usual documents. The agent can then change the list from the web app. */
@Component
@RequiredArgsConstructor
public class DocumentTypeSeeder implements CommandLineRunner {

    private final DocumentTypeRepository repository;

    @Override
    @Transactional
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        for (String n : List.of("Attestation de scolarité", "Relevé de notes", "Attestation de réussite",
                "Certificat d'assiduité", "Convention de stage", "Duplicata de la carte étudiant")) {
            add(n, "STUDENT", false);
        }
        for (String n : List.of("Attestation de travail", "Attestation de salaire", "Ordre de mission")) {
            add(n, "PROFESSOR", false);
        }
        add("Autre document", "BOTH", true);
    }

    private void add(String name, String audience, boolean requiresDetails) {
        DocumentType t = new DocumentType();
        t.setName(name);
        t.setAudience(audience);
        t.setRequiresDetails(requiresDetails);
        repository.save(t);
    }
}