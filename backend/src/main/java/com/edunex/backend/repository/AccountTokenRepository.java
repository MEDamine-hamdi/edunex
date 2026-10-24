package com.edunex.backend.repository;

import com.edunex.backend.entity.AccountToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface AccountTokenRepository extends JpaRepository<AccountToken, Long> {

    Optional<AccountToken> findByTokenHash(String tokenHash);

    /** Atomic: returns 1 only for the one request that uses the link first. */
    @Modifying
    @Query("update AccountToken t set t.usedAt = :now where t.id = :id and t.usedAt is null")
    int markUsed(@Param("id") Long id, @Param("now") Instant now);

    @Modifying
    @Query("delete from AccountToken t where t.userId = :userId and t.type = :type and t.usedAt is null")
    void deleteUnused(@Param("userId") Long userId, @Param("type") String type);

    @Modifying
    @Query("delete from AccountToken t where t.expiresAt < :before")
    int purgeExpired(@Param("before") Instant before);
}