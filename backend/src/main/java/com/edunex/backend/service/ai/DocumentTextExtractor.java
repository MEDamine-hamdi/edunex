package com.edunex.backend.service.ai;

import com.edunex.backend.service.FileStorage;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Reads the text of stored documents: PDF, DOCX, PPTX, XLSX, ODT, ODP and plain text files. */
@Component
@RequiredArgsConstructor
public class DocumentTextExtractor {

    public static class UnsupportedFormatException extends RuntimeException {
        public UnsupportedFormatException(String message) {
            super(message);
        }
    }

    public static final Set<String> SUPPORTED =
            Set.of("pdf", "docx", "pptx", "xlsx", "odt", "odp", "txt", "md", "markdown", "csv", "tex");

    private static final int MAX_FILE_BYTES = 25 * 1024 * 1024;
    private static final int MAX_PART_BYTES = 20 * 1024 * 1024;
    private static final int MAX_CHARS = 400_000;
    private static final Pattern SLIDE = Pattern.compile("ppt/slides/slide(\\d+)\\.xml");

    private final FileStorage fileStorage;

    public static boolean isSupported(String extension) {
        return extension != null && SUPPORTED.contains(extension.toLowerCase(Locale.ROOT));
    }

    public String extract(String storageKey) throws IOException {
        Resource resource = fileStorage.load(storageKey);
        String name = Paths.get(storageKey).getFileName().toString().toLowerCase(Locale.ROOT);
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";

        byte[] bytes;
        try (InputStream in = resource.getInputStream()) {
            bytes = in.readNBytes(MAX_FILE_BYTES + 1);
        }
        if (bytes.length > MAX_FILE_BYTES) {
            throw new UnsupportedFormatException("file too large to index");
        }

        String text = switch (ext) {
            case "pdf" -> pdf(bytes);
            case "docx" -> ooxml(bytes, false);
            case "pptx" -> ooxml(bytes, true);
            case "xlsx" -> xlsx(bytes);
            case "odt", "odp" -> odf(bytes);
            case "txt", "md", "markdown", "csv", "tex" -> new String(bytes, StandardCharsets.UTF_8);
            default -> throw new UnsupportedFormatException("unsupported file type: " + ext);
        };
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        return text.length() > MAX_CHARS ? text.substring(0, MAX_CHARS) : text;
    }

    private String pdf(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) {
                throw new UnsupportedFormatException("encrypted pdf");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setEndPage(400);
            return stripper.getText(document);
        } catch (InvalidPasswordException e) {
            throw new UnsupportedFormatException("password protected pdf");
        }
    }

    private String ooxml(byte[] bytes, boolean slides) throws IOException {
        Map<Integer, String> parts = new TreeMap<>();
        long total = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                int order;
                if (!slides && entry.getName().equals("word/document.xml")) {
                    order = 0;
                } else if (slides) {
                    Matcher m = SLIDE.matcher(entry.getName());
                    if (!m.matches()) {
                        continue;
                    }
                    order = Integer.parseInt(m.group(1));
                } else {
                    continue;
                }
                byte[] data = zip.readNBytes(MAX_PART_BYTES);
                total += data.length;
                if (total > 3L * MAX_PART_BYTES) {
                    throw new UnsupportedFormatException("document too large to index");
                }
                parts.put(order, unescape(new String(data, StandardCharsets.UTF_8)
                        .replace(slides ? "</a:p>" : "</w:p>", "\n")
                        .replaceAll("<w:tab\\s*/>", " ")
                        .replaceAll("<w:br\\s*/>|<a:br\\s*/>", "\n")
                        .replaceAll("<[^>]+>", "")));
            }
        }
        return String.join("\n\n", parts.values());
    }

    /** Spreadsheet text: the shared strings table (labels, headings, text cells). */
    private String xlsx(byte[] bytes) throws IOException {
        String xml = zipEntry(bytes, "xl/sharedStrings.xml");
        return xml == null ? "" : unescape(xml.replace("</si>", "\n").replaceAll("<[^>]+>", ""));
    }

    private String odf(byte[] bytes) throws IOException {
        String xml = zipEntry(bytes, "content.xml");
        return xml == null ? "" : unescape(xml
                .replaceAll("</text:(p|h|list-item)>", "\n")
                .replaceAll("<text:tab\\s*/>", " ")
                .replaceAll("<text:line-break\\s*/>", "\n")
                .replaceAll("<[^>]+>", ""));
    }

    private static String zipEntry(byte[] bytes, String name) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals(name)) {
                    return new String(zip.readNBytes(MAX_PART_BYTES), StandardCharsets.UTF_8);
                }
            }
        }
        return null;
    }

    private static String unescape(String s) {
        return s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&apos;", "'").replace("&amp;", "&");
    }
}