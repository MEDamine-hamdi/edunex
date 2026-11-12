package com.edunex.backend.service;

import com.edunex.backend.dto.CreateAgentRequest;
import com.edunex.backend.entity.University;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.ConflictException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.AdministrationAgentRepository;
import com.edunex.backend.repository.ProfessorRepository;
import com.edunex.backend.repository.StudentRepository;
import com.edunex.backend.repository.UniversityRepository;
import com.edunex.backend.repository.UserRepository;
import com.edunex.backend.tenant.TenantAccess;
import com.edunex.backend.tenant.TenantContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Not transactional on purpose: each call below opens its own transaction inside the right university. */
@Service
@RequiredArgsConstructor
public class UniversityService {

    public record Row(Long id, String name, String code, String city, String status, long agents, long professors, long students) {
    }

    public record AdminRow(Long id, String firstName, String lastName, String email, String employeeNumber, String service, String status) {
    }

    public record SaveRequest(@NotBlank @Size(max = 200) String name, @NotBlank @Size(max = 20) String code, @Size(max = 100) String city) {
    }

    private final UniversityRepository universities;
    private final AdministrationAgentRepository agentRepository;
    private final ProfessorRepository professorRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AdministrationAgentService agentService;
    private final AgentStatusService agentStatus;
    private final TenantAccess tenantAccess;

    public List<Row> list() {
        return universities.findAll().stream().sorted(Comparator.comparing(University::getName)).map(this::row).toList();
    }

    public Row create(SaveRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z0-9_-]{2,20}")) {
            throw new BadRequestException("The code uses 2 to 20 letters, digits, - or _");
        }
        String name = request.name().trim();
        for (University u : universities.findAll()) {
            if (u.getCode().equals(code) || u.getName().equalsIgnoreCase(name)) {
                throw new ConflictException("A university with this name or code already exists");
            }
        }
        University u = new University();
        u.setName(name);
        u.setCode(code);
        u.setCity(request.city() == null || request.city().isBlank() ? null : request.city().trim());
        return row(universities.save(u));
    }

    public void setStatus(Long id, String status) {
        String s = status.trim().toUpperCase(Locale.ROOT);
        if (!s.equals("ACTIVE") && !s.equals("SUSPENDED")) {
            throw new BadRequestException("Invalid status");
        }
        University u = universities.findById(id).orElseThrow(() -> new ResourceNotFoundException("University not found"));
        u.setStatus(s);
        universities.save(u);
        tenantAccess.invalidate();
    }

    public List<AdminRow> admins(Long universityId) {
        universities.findById(universityId).orElseThrow(() -> new ResourceNotFoundException("University not found"));
        return TenantContext.call(universityId, this::adminsHere);
    }

    /** The administrators of the university of the current request. */
    public List<AdminRow> adminsHere() {
        return agentRepository.findAll().stream()
                .sorted(Comparator.comparing(a -> a.getLastName()))
                .map(a -> new AdminRow(a.getId(), a.getFirstName(), a.getLastName(), a.getEmail(), a.getEmployeeNumber(),
                        a.getService(), a.getStatus().name()))
                .toList();
    }

    public Object createAdmin(Long universityId, CreateAgentRequest request) {
        University u = universities.findById(universityId).orElseThrow(() -> new ResourceNotFoundException("University not found"));
        if (!"ACTIVE".equals(u.getStatus())) {
            throw new ConflictException("This university is suspended");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("This e-mail address is already used");
        }
        return TenantContext.call(universityId, () -> agentService.create(request));
    }

    public void setAdminStatus(Long universityId, Long agentId, String status) {
        TenantContext.run(universityId, () -> agentStatus.setStatus(agentId, status, null));
    }

    private Row row(University u) {
        long agents = TenantContext.call(u.getId(), agentRepository::count);
        long professors = TenantContext.call(u.getId(), professorRepository::count);
        long students = TenantContext.call(u.getId(), studentRepository::count);
        return new Row(u.getId(), u.getName(), u.getCode(), u.getCity(), u.getStatus(), agents, professors, students);
    }
}