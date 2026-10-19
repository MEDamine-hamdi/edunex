package com.edunex.backend.service.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TextChunkerTest {

    @Test
    void splitsLongTextWithoutExceedingTheSize() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            text.append("Phrase numero ").append(i).append(" du cours de bases de donnees. ");
        }
        List<String> chunks = TextChunker.chunk(text.toString(), 200, 40);
        assertTrue(chunks.size() > 1);
        chunks.forEach(c -> assertTrue(c.length() <= 200));
    }

    @Test
    void returnsNothingForBlankText() {
        assertTrue(TextChunker.chunk("   \n  ", 200, 40).isEmpty());
    }
}