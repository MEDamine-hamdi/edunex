package com.edunex.backend.controller;

import com.edunex.backend.config.PermissionCatalog;
import com.edunex.backend.entity.Permission;
import com.edunex.backend.entity.User;
import com.edunex.backend.entity.UserPermission;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.UserPermissionRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** The platform administrator gives or takes rights from one specific person. */
@RestController
@RequestMapping("/api/platform/user-rights")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class UserRightsAdminController {

    public record Hit(Long id, String name, String email, String role) {
    }

    public record Right(String code, String description, boolean fromRole, String override, boolean effective) {
    }

    public record View(Long id, String name, String email, String role, List<Right> rights) {
    }

    public record Override(String code, String effect) {
    }

    public record SaveRequest(List<Override> overrides) {
    }

    /** Rights that have a screen for a professor. */
    private static final List<String> FOR_PROFESSORS = List.of("GRADES_MANAGE", "TIMETABLE_MANAGE", "ATTENDANCE_MARK");

    private final UserRepository userRepository;
    private final UserPermissionRepository repository;

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public List<Hit> users(@RequestParam(required = false) String q) {
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && (u.getRole().getName().equals("ROLE_PROFESSOR") || u.getRole().getName().equals("ROLE_ADMIN_AGENT")))
                .filter(u -> needle.isEmpty() || (u.getFirstName() + " " + u.getLastName() + " " + u.getEmail()).toLowerCase(Locale.ROOT).contains(needle))
                .sorted(Comparator.comparing(User::getLastName).thenComparing(User::getFirstName))
                .limit(50)
                .map(u -> new Hit(u.getId(), u.getFirstName() + " " + u.getLastName(), u.getEmail(), u.getRole().getName()))
                .toList();
    }

    @GetMapping("/users/{id}")
    @Transactional(readOnly = true)
    public View view(@PathVariable Long id) {
        return build(user(id));
    }

    @PutMapping("/users/{id}")
    @Transactional
    public View save(@PathVariable Long id, @RequestBody SaveRequest request) {
        User u = user(id);
        Set<String> fromRole = roleCodes(u);
        List<String> allowed = allowed(u);
        repository.deleteByUser(u.getId());
        List<UserPermission> rows = new ArrayList<>();
        for (Override o : request.overrides() == null ? List.<Override>of() : request.overrides()) {
            if (o.effect() == null || o.effect().isBlank() || o.effect().equals("DEFAULT")) {
                continue;
            }
            if (!allowed.contains(o.code()) || !(o.effect().equals("GRANT") || o.effect().equals("DENY"))) {
                throw new BadRequestException("This right cannot be given to this person");
            }
            boolean has = fromRole.contains(o.code());
            if ((o.effect().equals("GRANT") && has) || (o.effect().equals("DENY") && !has)) {
                continue;
            }
            UserPermission p = new UserPermission();
            p.setUserId(u.getId());
            p.setCode(o.code());
            p.setEffect(o.effect());
            rows.add(p);
        }
        repository.saveAll(rows);
        return build(u);
    }

    private View build(User u) {
        Set<String> fromRole = roleCodes(u);
        Map<String, String> overrides = repository.findByUserId(u.getId()).stream()
                .collect(Collectors.toMap(UserPermission::getCode, UserPermission::getEffect, (a, b) -> a));
        Map<String, String> descriptions = new HashMap<>();
        PermissionCatalog.ENTRIES.forEach(e -> descriptions.put(e.code(), e.description()));
        List<Right> rights = allowed(u).stream().map(code -> {
            boolean role = fromRole.contains(code);
            String ov = overrides.get(code);
            boolean effective = "GRANT".equals(ov) || (!"DENY".equals(ov) && role);
            return new Right(code, descriptions.getOrDefault(code, code), role, ov, effective);
        }).toList();
        return new View(u.getId(), u.getFirstName() + " " + u.getLastName(), u.getEmail(), u.getRole().getName(), rights);
    }

    private static List<String> allowed(User u) {
        String role = u.getRole() == null ? "" : u.getRole().getName();
        if (role.equals("ROLE_PROFESSOR")) {
            return FOR_PROFESSORS;
        }
        if (role.equals("ROLE_ADMIN_AGENT")) {
            return PermissionCatalog.ENTRIES.stream().map(PermissionCatalog.Entry::code)
                    .filter(c -> !c.equals("ATTENDANCE_MARK") && !c.equals("AI_ASSISTANT_USE")).toList();
        }
        throw new BadRequestException("Rights can only be changed for professors and agents");
    }

    private static Set<String> roleCodes(User u) {
        return u.getRole().getPermissions().stream().map(Permission::getCode).collect(Collectors.toSet());
    }

    private User user(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}