package com.edunex.backend.service.ai;

import com.edunex.backend.dto.ChatMessageResponse;
import com.edunex.backend.dto.ChatReply;
import com.edunex.backend.entity.ChatAttachment;
import com.edunex.backend.entity.ChatMessage;
import com.edunex.backend.repository.ChatAttachmentRepository;
import com.edunex.backend.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatAttachmentService {

    private final ChatMessageRepository messageRepository;
    private final ChatAttachmentRepository attachmentRepository;

    /** Links the file to the exchange that was just saved and returns the reply with the file name set. */
    @Transactional
    public ChatReply record(ChatReply reply, String fileName, String text) {
        ChatMessage m = messageRepository.findById(reply.userMessage().id()).orElseThrow();
        m.setAttachmentName(fileName);
        ChatAttachment a = new ChatAttachment();
        a.setChatSessionId(reply.sessionId());
        a.setMessageId(m.getId());
        a.setFileName(fileName);
        a.setContent(text);
        attachmentRepository.save(a);
        ChatMessageResponse u = reply.userMessage();
        return new ChatReply(reply.sessionId(), reply.title(),
                new ChatMessageResponse(u.id(), u.role(), u.content(), u.createdAt(), u.sources(), fileName),
                reply.assistantMessage());
    }

    @Transactional(readOnly = true)
    public List<ChatAttachment> recent(Long sessionId) {
        return attachmentRepository.findTop3ByChatSessionIdOrderByIdDesc(sessionId);
    }
}