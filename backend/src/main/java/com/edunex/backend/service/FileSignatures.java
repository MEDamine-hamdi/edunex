package com.edunex.backend.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/** Checks the first bytes of an upload against its extension. Unknown extensions are left to the caller's allow-list. */
public final class FileSignatures {

    private FileSignatures() {
    }

    public static boolean matches(String ext, MultipartFile file) {
        byte[] h = new byte[512];
        int n;
        try (InputStream in = file.getInputStream()) {
            n = in.readNBytes(h, 0, h.length);
        } catch (IOException e) {
            return false;
        }
        return switch (ext) {
            case "png" -> n >= 4 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
            case "jpg", "jpeg" -> n >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
            case "gif" -> n >= 4 && h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8';
            case "webp" -> n >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                    && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
            case "pdf" -> n >= 4 && h[0] == '%' && h[1] == 'P' && h[2] == 'D' && h[3] == 'F';
            case "docx", "xlsx", "pptx", "odt", "odp" -> n >= 4 && h[0] == 'P' && h[1] == 'K' && h[2] == 3 && h[3] == 4;
            case "doc", "xls", "ppt" -> n >= 4 && (h[0] & 0xFF) == 0xD0 && (h[1] & 0xFF) == 0xCF
                    && (h[2] & 0xFF) == 0x11 && (h[3] & 0xFF) == 0xE0;
            case "txt", "csv", "md" -> noNul(h, n);
            default -> true;
        };
    }

    private static boolean noNul(byte[] h, int n) {
        for (int i = 0; i < n; i++) {
            if (h[i] == 0) {
                return false;
            }
        }
        return true;
    }
}