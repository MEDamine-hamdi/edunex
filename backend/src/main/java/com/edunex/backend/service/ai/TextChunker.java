package com.edunex.backend.service.ai;

import java.util.ArrayList;
import java.util.List;

/** Cuts text into overlapping chunks, preferring paragraph and sentence boundaries. */
public final class TextChunker {

    private static final String[] DELIMITERS = {"\n\n", ". ", "\n", " "};

    private TextChunker() {
    }

    public static List<String> chunk(String raw, int size, int overlap) {
        if (raw == null) {
            return List.of();
        }
        String text = raw.replace("\r", "")
                .replaceAll("[ \\t\\u00A0]+", " ")
                .replaceAll(" ?\\n ?", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + size, text.length());
            if (end < text.length()) {
                int cut = boundary(text, start + size / 2, end);
                if (cut > 0) {
                    end = cut;
                }
            }
            String piece = text.substring(start, end).trim();
            if (piece.length() >= 20) {
                chunks.add(piece);
            }
            if (end >= text.length()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
        return chunks;
    }

    private static int boundary(String text, int from, int to) {
        for (String d : DELIMITERS) {
            int p = text.lastIndexOf(d, to - 1);
            if (p >= from) {
                return Character.isWhitespace(d.charAt(0)) ? p : p + 1;
            }
        }
        return -1;
    }
}