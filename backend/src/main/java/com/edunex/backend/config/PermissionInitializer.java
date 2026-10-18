package com.edunex.backend.config;

import com.edunex.backend.entity.Permission;
import com.edunex.backend.entity.Role;
import com.edunex.backend.repository.PermissionRepository;
import com.edunex.backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionInitializer implements CommandLineRunner {

    private static final Map<String, String> ROLE_DESCRIPTIONS = Map.of(
            "ROLE_STUDENT", "University student",
            "ROLE_PROFESSOR", "University professor",
            "ROLE_ADMIN_AGENT", "University administration agent",
            "ROLE_PLATFORM_ADMIN", "EduNex platform administrator");

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        for (PermissionCatalog.Entry entry : PermissionCatalog.ENTRIES) {
            if (permissionRepository.findByCode(entry.code()).isPresent()) {
                continue;
            }
            Permission permission = new Permission();
            permission.setCode(entry.code());
            permission.setDescription(entry.description());
            permission = permissionRepository.save(permission);

            for (String roleName : entry.roles()) {
                Role role = roleRepository.findByName(roleName).orElseGet(() -> {
                    Role created = new Role();
                    created.setName(roleName);
                    created.setDescription(ROLE_DESCRIPTIONS.get(roleName));
                    return roleRepository.save(created);
                });
                role.getPermissions().add(permission);
                roleRepository.save(role);
            }
            log.info("Permission created: {}", entry.code());
        }
    }
}