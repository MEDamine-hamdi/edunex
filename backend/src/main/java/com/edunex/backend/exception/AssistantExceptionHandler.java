package com.edunex.backend.exception;

import com.edunex.backend.controller.AssistantController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Only for the assistant endpoints, and ahead of the global handler so 403 and 503 are not turned into 500. */
@RestControllerAdvice(assignableTypes = AssistantController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AssistantExceptionHandler {

    @ExceptionHandler(AiUnavailableException.class)
    public ResponseEntity<Map<String, String>> unavailable(AiUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> denied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
    }
}