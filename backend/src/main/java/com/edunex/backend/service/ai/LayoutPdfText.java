package com.edunex.backend.service.ai;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Page texts that keep the horizontal layout, so the columns of a timetable grid stay aligned. */
public final class LayoutPdfText {

    private static final float CHAR_WIDTH = 4.6f;
    private static final float LINE_TOLERANCE = 3.0f;
    private static final int MAX_PAGE_CHARS = 9000;

    private LayoutPdfText() {
    }

    private static final class Collector extends PDFTextStripper {
        final Map<Integer, List<TextPosition>> pages = new TreeMap<>();

        Collector() throws IOException {
            super();
        }

        @Override
        protected void processTextPosition(TextPosition text) {
            pages.computeIfAbsent(getCurrentPageNo(), k -> new ArrayList<>()).add(text);
        }
    }

    public static List<String> pages(byte[] pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            if (doc.getNumberOfPages() > 200) {
                throw new IOException("too many pages");
            }
            if (doc.isEncrypted()) {
                throw new IOException("encrypted pdf");
            }
            Collector collector = new Collector();
            collector.setEndPage(Math.min(doc.getNumberOfPages(), 40));
            collector.getText(doc);
            return collector.pages.values().stream().map(LayoutPdfText::render).toList();
        }
    }

    static String render(List<TextPosition> positions) {
        List<TextPosition> sorted = new ArrayList<>(positions);
        sorted.sort(Comparator.comparingDouble(TextPosition::getYDirAdj).thenComparingDouble(TextPosition::getXDirAdj));
        StringBuilder out = new StringBuilder();
        List<TextPosition> row = new ArrayList<>();
        float rowY = Float.NaN;
        for (TextPosition tp : sorted) {
            if (!row.isEmpty() && Math.abs(tp.getYDirAdj() - rowY) > LINE_TOLERANCE) {
                flush(row, out);
                row.clear();
            }
            if (row.isEmpty()) {
                rowY = tp.getYDirAdj();
            }
            row.add(tp);
        }
        flush(row, out);
        String text = out.toString();
        return text.length() > MAX_PAGE_CHARS ? text.substring(0, MAX_PAGE_CHARS) : text;
    }

    private static void flush(List<TextPosition> row, StringBuilder out) {
        if (row.isEmpty()) {
            return;
        }
        row.sort(Comparator.comparingDouble(TextPosition::getXDirAdj));
        StringBuilder line = new StringBuilder();
        for (TextPosition tp : row) {
            int column = Math.round(tp.getXDirAdj() / CHAR_WIDTH);
            while (line.length() < column) {
                line.append(' ');
            }
            line.append(tp.getUnicode());
        }
        String text = line.toString().stripTrailing().replaceAll(" {12,}", "            ");
        if (!text.isBlank()) {
            out.append(text).append('\n');
        }
    }
}