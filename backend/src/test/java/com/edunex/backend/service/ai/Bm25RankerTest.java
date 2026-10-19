package com.edunex.backend.service.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Bm25RankerTest {

    @Test
    void ranksTheRelevantChunkFirst() {
        List<String> texts = List.of(
                "La jointure SQL combine des lignes de deux tables avec JOIN.",
                "Le cycle de vie du logiciel comprend la specification et les tests.",
                "Les reseaux utilisent le protocole TCP.");
        double[] scores = Bm25Ranker.score("Comment faire une jointure SQL ?", texts);
        assertTrue(scores[0] > scores[1]);
        assertTrue(scores[0] > scores[2]);
    }

    @Test
    void ignoresAccentsAndPlurals() {
        assertEquals(Bm25Ranker.tokens("donn\u00e9es"), Bm25Ranker.tokens("donnee"));
    }

    @Test
    void scoresZeroWhenNothingMatches() {
        double[] scores = Bm25Ranker.score("photosynthese", List.of("Les reseaux utilisent le protocole TCP."));
        assertEquals(0.0, scores[0]);
    }
}