package com.edunex.backend.service.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PassageSelectorTest {

    private static PassageSelector.Candidate candidate(long doc, String title, String text) {
        return new PassageSelector.Candidate(doc, title, "Cours", "Matiere", "COURSE", title + " " + text, text);
    }

    @Test
    void ranksTheRelevantDocumentFirstAndNumbersSourcesPerDocument() {
        List<Passage> out = PassageSelector.select(List.of(
                candidate(1, "Reseaux", "Le protocole TCP etablit une connexion fiable"),
                candidate(2, "SQL", "La jointure SQL combine deux tables"),
                candidate(2, "SQL", "Une jointure externe garde les lignes de la table de gauche")),
                "jointure SQL", 5, 3);

        assertFalse(out.isEmpty());
        assertEquals(2L, out.get(0).documentId().longValue());
        assertEquals(1, out.get(0).sourceNumber());
        assertTrue(out.stream().noneMatch(p -> p.documentId() == 1L));
    }

    @Test
    void capsThePassagesPerDocument() {
        List<PassageSelector.Candidate> many = List.of(
                candidate(1, "SQL", "jointure SQL premier paragraphe"),
                candidate(1, "SQL", "jointure SQL deuxieme paragraphe"),
                candidate(1, "SQL", "jointure SQL troisieme paragraphe"),
                candidate(1, "SQL", "jointure SQL quatrieme paragraphe"));
        assertEquals(2, PassageSelector.select(many, "jointure SQL", 10, 2).size());
    }

    @Test
    void returnsNothingWhenNoTermMatches() {
        assertTrue(PassageSelector.select(
                List.of(candidate(1, "Reseaux", "Le protocole TCP")), "photosynthese", 5, 3).isEmpty());
    }
}