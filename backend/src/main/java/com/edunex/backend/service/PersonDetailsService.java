package com.edunex.backend.service;

import com.edunex.backend.entity.Professor;
import com.edunex.backend.entity.User;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.ConflictException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.ProfessorRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonDetailsService {

    private final UserRepository userRepository;
    private final ProfessorRepository professorRepository;

    @Transactional
    public void setProfessorType(Long id, String type) {
        Professor p = professorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Professor not found"));
        p.setProfessorType(ProfessorTypes.normalize(type));
    }

    @Transactional
    public void setCin(Long userId, String cin) {
        if (cin == null || !cin.matches("^\\d{8}$")) {
            throw new BadRequestException("The CIN must be exactly 8 digits");
        }
        User u = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        userRepository.findByCin(cin).filter(other -> !other.getId().equals(userId)).ifPresent(other -> {
            throw new ConflictException("This CIN is already used by another account");
        });
        u.setCin(cin);
    }
}