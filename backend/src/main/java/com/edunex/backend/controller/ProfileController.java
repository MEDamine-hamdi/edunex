package com.edunex.backend.controller;

import com.edunex.backend.dto.ChangePasswordRequest;
import com.edunex.backend.dto.ProfileResponse;
import com.edunex.backend.dto.ProfileUpdateRequest;
import com.edunex.backend.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ProfileResponse get(Authentication auth) {
        return profileService.get(auth.getName());
    }

    @PutMapping
    public ProfileResponse update(@Valid @RequestBody ProfileUpdateRequest request, Authentication auth) {
        return profileService.update(auth.getName(), request);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                               Authentication auth) {
        profileService.changePassword(auth.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileResponse uploadPhoto(@RequestParam("file") MultipartFile file, Authentication auth) {
        return profileService.uploadPhoto(auth.getName(), file);
    }

    @GetMapping("/photo")
    public ResponseEntity<Resource> photo(Authentication auth) {
        ProfileService.PhotoFile photo = profileService.photo(auth.getName());
        MediaType type = MediaTypeFactory.getMediaType(photo.filename()).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CACHE_CONTROL, "private, no-cache")
                .body(photo.resource());
    }

    @DeleteMapping("/photo")
    public ProfileResponse deletePhoto(Authentication auth) {
        return profileService.deletePhoto(auth.getName());
    }
}