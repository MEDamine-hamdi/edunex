package com.edunex.backend.repository;

import com.edunex.backend.entity.TokenRevocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenRevocationRepository extends JpaRepository<TokenRevocation, String> {
}