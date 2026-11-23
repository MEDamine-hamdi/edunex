package com.edunex.backend.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A value that must be unique already exists (or a record is still in use): a clear answer instead of a 500 error. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DataIntegrityHandler {

    private static final Pattern KEY = Pattern.compile("Key \\(([^)]+)\\)=");

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> conflict(DataIntegrityViolationException e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String text = String.valueOf(root.getMessage());
        if (text.contains("duplicate key")) {
            Matcher m = KEY.matcher(text);
            String column = m.find() ? m.group(1) : null;
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "This value is already used" + (column != null ? " (" + column + ")" : "")));
        }
        if (text.contains("foreign key")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "This record is still used elsewhere"));
        }
        if (text.contains("null value in column")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "A required field is missing"));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "The data conflicts with existing records"));
    }
}