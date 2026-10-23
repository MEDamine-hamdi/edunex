package com.edunex.backend.service;

import com.edunex.backend.dto.ChangePasswordRequest;
import com.edunex.backend.dto.ProfileResponse;
import com.edunex.backend.dto.ProfileUpdateRequest;
import com.edunex.backend.entity.AdministrationAgent;
import com.edunex.backend.entity.Professor;
import com.edunex.backend.entity.Profile;
import com.edunex.backend.entity.Student;
import com.edunex.backend.entity.User;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.ProfileRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final Set<String> PHOTO_TYPES = Set.of("png", "jpg", "jpeg", "webp");
    private static final long MAX_PHOTO_BYTES = 2L * 1024 * 1024;

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorage fileStorage;
    private final com.edunex.backend.security.TokenRevocationService tokenRevocation;

    public record PhotoFile(Resource resource, String filename) {
    }

    @Transactional(readOnly = true)
    public ProfileResponse get(String email) {
        User u = user(email);
        return toResponse(u, profileRepository.findByUserId(u.getId()).orElse(null));
    }

    @Transactional
    public ProfileResponse update(String email, ProfileUpdateRequest req) {
        User u = user(email);
        u.setPhone(blankToNull(req.phone()));
        userRepository.save(u);
        Profile p = profileOf(u);
        p.setAddress(blankToNull(req.address()));
        p.setDateOfBirth(req.dateOfBirth());
        return toResponse(u, profileRepository.save(p));
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest req) {
        User u = user(email);
        if (!passwordEncoder.matches(req.currentPassword(), u.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(req.newPassword(), u.getPassword())) {
            throw new BadRequestException("New password must be different from the current one");
        }
        u.setPassword(passwordEncoder.encode(req.newPassword()));
        tokenRevocation.invalidate(u.getEmail());
        userRepository.save(u);
    }

    @Transactional
    public ProfileResponse uploadPhoto(String email, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        if (file.getSize() > MAX_PHOTO_BYTES) {
            throw new BadRequestException("Photo is too large (2 MB maximum)");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
        if (!PHOTO_TYPES.contains(ext)) {
            throw new BadRequestException("Photo must be a PNG, JPG or WEBP image");
        }
        User u = user(email);
        Profile p = profileOf(u);
        String oldKey = p.getPhotoUrl();
        p.setPhotoUrl(fileStorage.store(file));
        Profile saved = profileRepository.save(p);
        if (oldKey != null) {
            fileStorage.delete(oldKey);
        }
        return toResponse(u, saved);
    }

    @Transactional(readOnly = true)
    public PhotoFile photo(String email) {
        User u = user(email);
        Profile p = profileRepository.findByUserId(u.getId())
                .filter(x -> x.getPhotoUrl() != null)
                .orElseThrow(() -> new ResourceNotFoundException("No photo"));
        return new PhotoFile(fileStorage.load(p.getPhotoUrl()), Paths.get(p.getPhotoUrl()).getFileName().toString());
    }

    @Transactional
    public ProfileResponse deletePhoto(String email) {
        User u = user(email);
        Profile p = profileRepository.findByUserId(u.getId()).orElse(null);
        if (p != null && p.getPhotoUrl() != null) {
            fileStorage.delete(p.getPhotoUrl());
            p.setPhotoUrl(null);
            p = profileRepository.save(p);
        }
        return toResponse(u, p);
    }

    // ---------- helpers ----------

    private User user(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Unknown user"));
    }

    private Profile profileOf(User u) {
        return profileRepository.findByUserId(u.getId()).orElseGet(() -> {
            Profile p = new Profile();
            p.setUser(u);
            return p;
        });
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private ProfileResponse toResponse(User u, Profile profile) {
        return new ProfileResponse(
                u.getId(), u.getFirstName(), u.getLastName(), u.getEmail(), u.getPhone(),
                u.getRole() != null ? u.getRole().getName() : null, u.getStatus().name(),
                profile != null ? profile.getAddress() : null,
                profile != null ? profile.getDateOfBirth() : null,
                profile != null && profile.getPhotoUrl() != null,
                details(u));
    }

    private List<ProfileResponse.Detail> details(User u) {
        List<ProfileResponse.Detail> d = new ArrayList<>();
        if (u instanceof Student s) {
            add(d, "Numéro étudiant", s.getStudentNumber());
            add(d, "Année d'inscription", s.getEnrollmentYear());
            add(d, "Niveau", s.getLevel());
            add(d, "Spécialité", s.getSpecialty());
        } else if (u instanceof Professor p) {
            add(d, "Numéro d'employé", p.getEmployeeNumber());
            add(d, "Département", p.getDepartment());
            add(d, "Spécialité", p.getSpecialty());
        } else if (u instanceof AdministrationAgent a) {
            add(d, "Numéro d'employé", a.getEmployeeNumber());
            add(d, "Service", a.getService());
        }
        return d;
    }

    private static void add(List<ProfileResponse.Detail> list, String label, Object value) {
        if (value != null && !value.toString().isBlank()) {
            list.add(new ProfileResponse.Detail(label, value.toString()));
        }
    }
}