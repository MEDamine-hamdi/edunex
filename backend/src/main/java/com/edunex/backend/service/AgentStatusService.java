package com.edunex.backend.service;

import com.edunex.backend.entity.AdministrationAgent;
import com.edunex.backend.enums.UserStatus;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.AdministrationAgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AgentStatusService {

    private final AdministrationAgentRepository agents;

    /** Activates or deactivates an administrator of the current university (never the last active one, never yourself). */
    @Transactional
    public void setStatus(Long agentId, String status, String actorEmail) {
        UserStatus target;
        try {
            target = UserStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status");
        }
        AdministrationAgent agent = agents.findById(agentId).orElseThrow(() -> new ResourceNotFoundException("Administrator not found"));
        if (actorEmail != null && agent.getEmail().equalsIgnoreCase(actorEmail)) {
            throw new BadRequestException("You cannot change your own status");
        }
        if (target != UserStatus.ACTIVE && agent.getStatus() == UserStatus.ACTIVE
                && agents.findAll().stream().filter(x -> x.getStatus() == UserStatus.ACTIVE).count() <= 1) {
            throw new BadRequestException("A university needs at least one active administrator");
        }
        agent.setStatus(target);
    }
}