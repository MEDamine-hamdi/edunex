package com.edunex.backend.repository;

import com.edunex.backend.entity.LoginThrottle;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface LoginThrottleRepository extends JpaRepository<LoginThrottle, String> {

    /** Creates the row when it does not exist yet (safe when two requests arrive together). */
    @Modifying
    @Query(value = "insert into login_throttle (throttle_key, failures) values (:key, 0) on conflict (throttle_key) do nothing",
            nativeQuery = true)
    void ensure(@Param("key") String key);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from LoginThrottle t where t.id = :key")
    Optional<LoginThrottle> findForUpdate(@Param("key") String key);

    @Modifying
    @Query("delete from LoginThrottle t where t.windowStart < :before and (t.lockedUntil is null or t.lockedUntil < :now)")
    int purge(@Param("before") Instant before, @Param("now") Instant now);
}