package com.edunex.backend.service;

import com.edunex.backend.exception.BadRequestException;

import java.util.Locale;

public final class ProfessorTypes {

    private ProfessorTypes() {
    }

    /** Empty means PERMANENT. */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "PERMANENT";
        }
        String t = raw.trim().toUpperCase(Locale.ROOT);
        if (t.equals("PERMANENT") || t.equals("VACATAIRE")) {
            return t;
        }
        throw new BadRequestException("The professor type must be Permanent or Vacataire");
    }
}