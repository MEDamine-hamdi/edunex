package com.edunex.backend.repository;

import com.edunex.backend.entity.AbsencePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AbsencePolicyRepository extends JpaRepository<AbsencePolicy, Long> {

    Optional<AbsencePolicy> findBySubjectId(Long subjectId);

    Optional<AbsencePolicy> findBySubjectIdIsNull();

    @org.springframework.data.jpa.repository.Query("select p from AbsencePolicy p where p.subjectId is null and p.universityId = :uid order by p.id")
    java.util.List<AbsencePolicy> defaultsFor(@org.springframework.data.repository.query.Param("uid") Long uid);
}