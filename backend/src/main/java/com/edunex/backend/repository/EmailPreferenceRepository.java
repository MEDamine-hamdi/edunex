package com.edunex.backend.repository;

import com.edunex.backend.entity.EmailPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailPreferenceRepository extends JpaRepository<EmailPreference, Long> {
}