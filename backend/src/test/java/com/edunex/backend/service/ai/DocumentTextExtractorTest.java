package com.edunex.backend.service.ai;

import com.edunex.backend.service.FileStorage;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentTextExtractorTest {

    private static String extract(String fileName, byte[] data) throws IOException {
        FileStorage storage = new FileStorage() {
            @Override
            public String store(MultipartFile file) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Resource load(String key) {
                return new ByteArrayResource(data);
            }

            @Override
            public void delete(String key) {
            }
        };
        return new DocumentTextExtractor(storage).extract("abc/" + fileName);
    }

    private static byte[] zip(Map<String, String> entries) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Map.Entry<String, String> e : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(e.getKey()));
                zip.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    @Test
    void plainTextKeepsItsContentWithoutTheByteOrderMark() throws IOException {
        assertEquals("Reglement interieur", extract("guide.txt", "\uFEFFReglement interieur".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void docxParagraphsAreExtracted() throws IOException {
        String xml = "<w:document><w:body><w:p><w:r><w:t>Premier</w:t></w:r></w:p>"
                + "<w:p><w:r><w:t xml:space=\"preserve\">Second &amp; dernier</w:t></w:r></w:p></w:body></w:document>";
        String text = extract("guide.docx", zip(Map.of("word/document.xml", xml)));
        assertTrue(text.contains("Premier"));
        assertTrue(text.contains("Second & dernier"));
    }

    @Test
    void pptxSlidesAreReadInOrder() throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("ppt/slides/slide2.xml", "<p:sld><a:p><a:r><a:t>Seconde</a:t></a:r></a:p></p:sld>");
        entries.put("ppt/slides/slide1.xml", "<p:sld><a:p><a:r><a:t>Premiere</a:t></a:r></a:p></p:sld>");
        String text = extract("cours.pptx", zip(entries));
        assertTrue(text.indexOf("Premiere") < text.indexOf("Seconde"));
    }

    @Test
    void odtParagraphsAreExtracted() throws IOException {
        String text = extract("guide.odt", zip(Map.of("content.xml",
                "<office:text><text:p>Bonjour</text:p><text:p>Le secretariat ouvre a 9h</text:p></office:text>")));
        assertTrue(text.contains("Bonjour"));
        assertTrue(text.contains("secretariat ouvre"));
    }

    @Test
    void xlsxSharedStringsAreExtracted() throws IOException {
        String text = extract("tarifs.xlsx", zip(Map.of("xl/sharedStrings.xml",
                "<sst><si><t>Cellule A</t></si><si><t>Cellule B</t></si></sst>")));
        assertTrue(text.contains("Cellule A"));
        assertTrue(text.contains("Cellule B"));
    }

    @Test
    void unsupportedFormatsAreReportedNotParsed() {
        assertThrows(DocumentTextExtractor.UnsupportedFormatException.class,
                () -> extract("ancien.doc", new byte[]{1, 2, 3}));
        assertFalse(DocumentTextExtractor.isSupported("exe"));
        assertTrue(DocumentTextExtractor.isSupported("PDF"));
    }
}