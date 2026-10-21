package com.edunex.backend.service.ai;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TimetableMatcherTest {

    private static final List<String> SUBJECTS = List.of("Bases de données", "Réseaux", "Génie logiciel");

    @Test
    void accentsAndCaseDoNotMatter() {
        assertEquals(1.0, TimetableMatcher.similarity("Bases de données", "bases de donnees"));
    }

    @Test
    void picksTheClearBestMatch() {
        assertEquals("Réseaux", TimetableMatcher.best(SUBJECTS, s -> List.of(s), "reseau", 0.5));
        assertEquals("Bases de données", TimetableMatcher.best(SUBJECTS, s -> List.of(s), "Bases de donnees", 0.5));
    }

    @Test
    void anUnknownOrAmbiguousNameGivesNothing() {
        assertNull(TimetableMatcher.best(SUBJECTS, s -> List.of(s), "Chimie organique", 0.5));
        assertNull(TimetableMatcher.best(List.of("Algo 1", "Algo 2"), s -> List.of(s), "Algo", 0.3));
    }

    @Test
    void titlesAreRemovedFromProfessorNames() {
        assertEquals("trabelsi leila", TimetableMatcher.stripTitles("Dr. Trabelsi Leila"));
        assertEquals("trabelsi", TimetableMatcher.stripTitles("Mme TRABELSI"));
    }

    @Test
    void daysAndTimesAreReadInFrenchAndEnglish() {
        assertEquals(DayOfWeek.MONDAY, TimetableMatcher.day("Lundi"));
        assertEquals(DayOfWeek.WEDNESDAY, TimetableMatcher.day("mer."));
        assertEquals(DayOfWeek.FRIDAY, TimetableMatcher.day("FRIDAY"));
        assertNull(TimetableMatcher.day("???"));
        assertEquals(LocalTime.of(8, 30), TimetableMatcher.time("8h30"));
        assertEquals(LocalTime.of(14, 0), TimetableMatcher.time("14:00"));
        assertEquals(LocalTime.of(9, 0), TimetableMatcher.time("9h"));
        assertNull(TimetableMatcher.time("25:00"));
    }

    @Test
    void theModelAnswerIsParsedEvenWithTextAroundTheJson() {
        List<TimetableImportService.RawSession> out = TimetableImportService.parse(
                "Voici :\n{\"sessions\":[{\"day\":\"MONDAY\",\"start\":\"08:30\",\"end\":\"10:00\","
                        + "\"subject\":\"Reseaux\",\"professor\":\"Dr X\",\"room\":\"A1\",\"className\":\"L2\"}]}\nFin");
        assertEquals(1, out.size());
        assertEquals("Reseaux", out.get(0).subject());
        assertEquals(0, TimetableImportService.parse("pas de json").size());
    }
}