package com.edunex.backend.service;

import com.edunex.backend.dto.PermissionResponse;
import com.edunex.backend.dto.RoleResponse;
import com.edunex.backend.entity.Permission;
import com.edunex.backend.entity.Role;
import com.edunex.backend.exception.ConflictException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.PermissionRepository;
import com.edunex.backend.repository.RoleRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    /** The platform administrator keeps full access, so this role cannot be edited. */
    private static final String LOCKED_ROLE = "ROLE_PLATFORM_ADMIN";

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<RoleResponse> roles() {
        return roleRepository.findAll().stream()
                .sorted(Comparator.comparing(Role::getName))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RoleResponse assignPermissions(Long roleId, List<Long> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        if (LOCKED_ROLE.equals(role.getName())) {
            throw new ConflictException("This role cannot be modified");
        }
        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        if (permissions.size() != new HashSet<>(permissionIds).size()) {
            throw new ResourceNotFoundException("One or more permissions not found");
        }
        role.setPermissions(new HashSet<>(permissions));
        return toResponse(roleRepository.save(role));
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> permissions() {
        Map<Long, List<String>> holders = new HashMap<>();
        for (Role r : roleRepository.findAll()) {
            for (Permission p : r.getPermissions()) {
                holders.computeIfAbsent(p.getId(), k -> new ArrayList<>()).add(r.getName());
            }
        }
        return permissionRepository.findAll().stream()
                .sorted(Comparator.comparing(Permission::getCode))
                .map(p -> new PermissionResponse(p.getId(), p.getCode(), p.getDescription(),
                        holders.getOrDefault(p.getId(), List.of()).stream().sorted().toList()))
                .toList();
    }

    @Transactional
    public PermissionResponse updateDescription(Long id, String description) {
        Permission p = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));
        p.setDescription(description.trim());
        permissionRepository.save(p);
        return permissions().stream()
                .filter(x -> x.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));
    }

    private RoleResponse toResponse(Role r) {
        return new RoleResponse(r.getId(), r.getName(), r.getDescription(), LOCKED_ROLE.equals(r.getName()),
                userRepository.countByRoleId(r.getId()),
                r.getPermissions().stream()
                        .sorted(Comparator.comparing(Permission::getCode))
                        .map(p -> new RoleResponse.PermissionRef(p.getId(), p.getCode(), p.getDescription()))
                        .toList());
    }
}