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

    @GetMapping("/scope-info")
    public ScopeInfo scopeInfo(@RequestParam(required = false) Long subjectId,
                               @RequestParam(required = false) Long courseId,
                               Authentication auth) {
        return assistantService.scopeInfo(auth.getName(), subjectId, courseId);
    }
}