package com.edunex.backend.service;

import com.edunex.backend.dto.AgentResponse;
import com.edunex.backend.dto.CreateAgentRequest;
import com.edunex.backend.entity.AdministrationAgent;
import com.edunex.backend.enums.UserStatus;
import com.edunex.backend.exception.ConflictException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.AdministrationAgentRepository;
import com.edunex.backend.repository.RoleRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdministrationAgentService {

    private final AdministrationAgentRepository agentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AgentResponse create(CreateAgentRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ConflictException("Email already in use");
        }
        if (agentRepository.findByEmployeeNumber(req.employeeNumber()).isPresent()) {
            throw new ConflictException("Employee number already in use");
        }

        AdministrationAgent agent = new AdministrationAgent();
        agent.setFirstName(req.firstName());
        agent.setLastName(req.lastName());
        agent.setEmail(req.email());
        agent.setPassword(passwordEncoder.encode(req.password()));
        agent.setPhone(req.phone());
        agent.setEmployeeNumber(req.employeeNumber());
        agent.setService(req.service());
        agent.setRole(roleRepository.findByName("ROLE_ADMIN_AGENT")
                .orElseThrow(() -> new ResourceNotFoundException("Role ROLE_ADMIN_AGENT not found")));

        return toResponse(agentRepository.save(agent));
    }

    @Transactional(readOnly = true)
    public List<AgentResponse> findAll() {
        return agentRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public AgentResponse updateStatus(Long id, UserStatus status) {
        AdministrationAgent agent = agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administration agent not found"));
        agent.setStatus(status);
        return toResponse(agentRepository.save(agent));
    }

    private AgentResponse toResponse(AdministrationAgent a) {
        return new AgentResponse(a.getId(), a.getFirstName(), a.getLastName(), a.getEmail(),
                a.getPhone(), a.getEmployeeNumber(), a.getService(), a.getStatus().name());
    }
}
