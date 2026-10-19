package com.edunex.backend.service.ai;

import com.edunex.backend.dto.SourceRef;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatSourceCodecTest {

    @Test
    void roundTripKeepsEverything() {
        List<SourceRef> in = List.of(
                new SourceRef(1, 10L, "Notes", "Cours SQL", "Bases de donnees", "COURSE"),
                new SourceRef(2, 11L, "Guide", "", "", "INSTITUTE"));
        assertEquals(in, ChatHistoryService.decode(ChatHistoryService.encode(in)));
    }

    @Test
    void linesWrittenBeforeTheKindExistedAreCourseSources() {
        List<SourceRef> out = ChatHistoryService.decode("1\t5\tDoc\tCours\tMatiere");
        assertEquals("COURSE", out.get(0).kind());
    }

    @Test
    void tabsAndNewlinesInTitlesCannotBreakTheFormat() {
        List<SourceRef> out = ChatHistoryService.decode(ChatHistoryService.encode(
                List.of(new SourceRef(1, 3L, "Titre\tavec\ntabulation", "C", "M", "COURSE"))));
        assertEquals(1, out.size());
        assertEquals("Titre avec tabulation", out.get(0).documentTitle());
    }

    @Test
    void nothingToEncodeGivesNullAndNothingToDecode() {
        assertNull(ChatHistoryService.encode(List.of()));
        assertTrue(ChatHistoryService.decode(null).isEmpty());
    }

    @Test
    void generalScopeHasNoSubjectAndNoCourse() {
        assertTrue(new ChatHistoryService.Scope(null, null).isGeneral());
        assertEquals(false, new ChatHistoryService.Scope(4L, null).isGeneral());
    }
}