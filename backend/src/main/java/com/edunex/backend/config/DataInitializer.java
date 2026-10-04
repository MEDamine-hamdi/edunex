package com.edunex.backend.config;

import com.edunex.backend.entity.PlatformAdministrator;
import com.edunex.backend.entity.Role;
import com.edunex.backend.repository.PlatformAdministratorRepository;
import com.edunex.backend.repository.RoleRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PlatformAdministratorRepository platformAdministratorRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        Map<String, String> roles = Map.of(
                "ROLE_STUDENT", "University student",
                "ROLE_PROFESSOR", "University professor",
                "ROLE_ADMIN_AGENT", "University administration agent",
                "ROLE_PLATFORM_ADMIN", "EduNex platform administrator"
        );

        roles.forEach((name, description) -> {
            if (roleRepository.findByName(name).isEmpty()) {
                Role role = new Role();
                role.setName(name);
                role.setDescription(description);
                roleRepository.save(role);
                log.info("Role created: {}", name);
            }
        });

        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn("app.admin.email / app.admin.password not set: no platform admin created");
            return;
        }

        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        PlatformAdministrator admin = new PlatformAdministrator();
        admin.setFirstName("Platform");
        admin.setLastName("Admin");
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(roleRepository.findByName("ROLE_PLATFORM_ADMIN").orElseThrow());
        platformAdministratorRepository.save(admin);
        log.info("First platform administrator created: {}", adminEmail);
    }
}
