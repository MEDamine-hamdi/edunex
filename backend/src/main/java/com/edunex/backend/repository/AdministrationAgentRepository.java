package com.edunex.backend.repository;

import com.edunex.backend.entity.AdministrationAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdministrationAgentRepository extends JpaRepository<AdministrationAgent, Long> {
    Optional<AdministrationAgent> findByEmployeeNumber(String employeeNumber);
}
