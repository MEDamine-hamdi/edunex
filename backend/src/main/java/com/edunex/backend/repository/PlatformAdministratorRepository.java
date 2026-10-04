package com.edunex.backend.repository;

import com.edunex.backend.entity.PlatformAdministrator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformAdministratorRepository extends JpaRepository<PlatformAdministrator, Long> {

}
