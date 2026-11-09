package com.edunex.backend.controller;

import com.edunex.backend.dto.MessagingDto;
import com.edunex.backend.service.MessagingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/messaging")
@PreAuthorize("hasAnyRole('STUDENT','PROFESSOR','ADMIN_AGENT')")
@RequiredArgsConstructor
public class MessagingController {

    private final MessagingService service;

    @GetMapping("/contacts")
    public List<MessagingDto.Contact> contacts(@RequestParam(required = false) String q, Authentication auth) {
        return service.contactList(auth.getName(), q);
    }

    @GetMapping("/conversations")
    public List<MessagingDto.ConvRow> conversations(Authentication auth) {
        return service.conversationList(auth.getName());
    }

    @PostMapping("/conversations/direct")
    public MessagingDto.ConvDetail direct(@Valid @RequestBody MessagingDto.DirectRequest request, Authentication auth) {
        return service.direct(auth.getName(), request.userId());
    }

    @PostMapping("/conversations/group")
    public ResponseEntity<MessagingDto.ConvDetail> group(@Valid @RequestBody MessagingDto.GroupRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.group(auth.getName(), request.name(), request.memberIds()));
    }

    @GetMapping("/conversations/{id}")
    public MessagingDto.ConvDetail detail(@PathVariable Long id, Authentication auth) {
        return service.detail(auth.getName(), id);
    }

    @PutMapping("/conversations/{id}")
    public MessagingDto.ConvDetail rename(@PathVariable Long id, @Valid @RequestBody MessagingDto.RenameRequest request, Authentication auth) {
        return service.rename(auth.getName(), id, request.name());
    }

    @PutMapping(value = "/conversations/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MessagingDto.ConvDetail setPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file, Authentication auth) {
        return service.setPhoto(auth.getName(), id, file);
    }

    @DeleteMapping("/conversations/{id}/photo")
    public MessagingDto.ConvDetail removePhoto(@PathVariable Long id, Authentication auth) {
        return service.removePhoto(auth.getName(), id);
    }

    @PostMapping("/conversations/{id}/members")
    public MessagingDto.ConvDetail addMembers(@PathVariable Long id, @Valid @RequestBody MessagingDto.MembersRequest request, Authentication auth) {
        return service.addMembers(auth.getName(), id, request.userIds());
    }

    @DeleteMapping("/conversations/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable Long userId, Authentication auth) {
        service.removeMember(auth.getName(), id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/conversations/{id}/messages")
    public MessagingDto.MessagePage messages(@PathVariable Long id,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
                                             @RequestParam(required = false) Long before, Authentication auth) {
        return service.messageList(auth.getName(), id, since, before);
    }

    @PostMapping("/conversations/{id}/messages")
    public MessagingDto.MessageView send(@PathVariable Long id, @Valid @RequestBody MessagingDto.TextRequest request, Authentication auth) {
        return service.sendText(auth.getName(), id, request.content());
    }

    @PostMapping(value = "/conversations/{id}/messages/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MessagingDto.MessageView sendFile(@PathVariable Long id, @RequestParam(value = "content", required = false) String content,
                                             @RequestParam("file") MultipartFile file, Authentication auth) {
        return service.sendFile(auth.getName(), id, content, file);
    }

    @PostMapping("/conversations/{id}/read")
    public ResponseEntity<Void> read(@PathVariable Long id, Authentication auth) {
        service.markRead(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread")
    public MessagingDto.UnreadResponse unread(Authentication auth) {
        return new MessagingDto.UnreadResponse(service.unread(auth.getName()));
    }

    @DeleteMapping("/messages/{id}")
    public MessagingDto.MessageView delete(@PathVariable Long id, Authentication auth) {
        return service.delete(auth.getName(), id);
    }

    @GetMapping("/messages/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable Long id, Authentication auth) {
        return respond(service.messageFile(auth.getName(), id), true);
    }

    @GetMapping("/users/{id}/photo")
    public ResponseEntity<Resource> userPhoto(@PathVariable Long id, Authentication auth) {
        return respond(service.userPhoto(auth.getName(), id), false);
    }

    @GetMapping("/conversations/{id}/photo")
    public ResponseEntity<Resource> groupPhoto(@PathVariable Long id, Authentication auth) {
        return respond(service.groupPhoto(auth.getName(), id), false);
    }

    private static ResponseEntity<Resource> respond(MessagingService.FileData d, boolean attachment) {
        ResponseEntity.BodyBuilder b = ResponseEntity.ok().contentType(MediaType.parseMediaType(MessagingService.contentType(d)));
        if (attachment) {
            b.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(d.filename()).build().toString());
        } else {
            b.cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePrivate());
        }
        return b.body(d.resource());
    }
}