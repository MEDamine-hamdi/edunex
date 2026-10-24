package com.edunex.backend.repository;

import com.edunex.backend.entity.User;
import com.edunex.backend.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    long countByRoleId(Long roleId);

    long countByStatus(UserStatus status);
}