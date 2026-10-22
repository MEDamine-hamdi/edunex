package com.edunex.backend.controller;

import com.edunex.backend.dto.AssistantChatRequest;
import com.edunex.backend.dto.ChatMessageResponse;
import com.edunex.backend.dto.ChatReply;
import com.edunex.backend.dto.ChatSessionResponse;
import com.edunex.backend.dto.ScopeInfo;
import com.edunex.backend.service.ai.AssistantService;
import com.edunex.backend.service.ai.ChatHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assistant")
@PreAuthorize("hasRole('STUDENT') and hasAuthority('AI_ASSISTANT_USE')")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;
    private final ChatHistoryService historyService;
    private final com.edunex.backend.service.ai.DocumentTextExtractor extractor;

    @PostMapping("/chat")
    public ChatReply chat(@Valid @RequestBody AssistantChatRequest request, Authentication auth) {
        return assistantService.ask(auth.getName(), request);
    }

    /** No parameter: general conversations. subjectId: that subject. courseId: that course. */
    @GetMapping("/sessions")
    public List<ChatSessionResponse> sessions(@RequestParam(required = false) Long subjectId,
                                              @RequestParam(required = false) Long courseId,
                                              Authentication auth) {
        return historyService.sessions(auth.getName(), subjectId, courseId);
    }

    @GetMapping("/sessions/{id}/messages")
    public List<ChatMessageResponse> messages(@PathVariable Long id, Authentication auth) {
        return historyService.messages(auth.getName(), id);
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        historyService.delete(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/chat-file", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatReply chatWithFile(@RequestParam("message") String message,
                                  @RequestParam(value = "sessionId", required = false) Long sessionId,
                                  @RequestParam(value = "subjectId", required = false) Long subjectId,
                                  @RequestParam(value = "courseId", required = false) Long courseId,
                                  @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                  Authentication auth) {
        if (message == null || message.isBlank() || message.length() > 2000) {
            throw new com.edunex.backend.exception.BadRequestException("Invalid message");
        }
        if (file.isEmpty() || file.getSize() > 10L * 1024 * 1024) {
            throw new com.edunex.backend.exception.BadRequestException("The file is empty or larger than 10 MB");
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String name = original.substring(Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1);
        String text;
        try {
            text = extractor.extractFrom(name, file.getBytes());
        } catch (com.edunex.backend.service.ai.DocumentTextExtractor.UnsupportedFormatException | java.io.IOException e) {
            throw new com.edunex.backend.exception.BadRequestException(
                    "Unsupported file: use PDF, DOCX, PPTX, XLSX, ODT, ODP, TXT or MD (images are not supported)");
        }
        if (text.isBlank()) {
            throw new com.edunex.backend.exception.BadRequestException(
                    "No readable text in this file (scanned documents and images are not supported)");
        }
        if (text.length() > 40_000) {
            text = text.substring(0, 40_000);
        }
        return assistantService.ask(auth.getName(), new AssistantChatRequest(sessionId, subjectId, courseId, message.trim()),
                name.length() > 250 ? name.substring(name.length() - 250) : name, text);
    }

    @GetMapping("/scope-info")
    public ScopeInfo scopeInfo(@RequestParam(required = false) Long subjectId,
                               @RequestParam(required = false) Long courseId,
                               Authentication auth) {
        return assistantService.scopeInfo(auth.getName(), subjectId, courseId);
    }
}