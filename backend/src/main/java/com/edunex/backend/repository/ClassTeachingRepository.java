package com.edunex.backend.repository;

import com.edunex.backend.entity.ClassTeaching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassTeachingRepository extends JpaRepository<ClassTeaching, Long> {

    List<ClassTeaching> findByClassId(Long classId);

    List<ClassTeaching> findByProfessorId(Long professorId);

    List<ClassTeaching> findByClassIdAndSubjectId(Long classId, Long subjectId);

    @Modifying
    @Query("delete from ClassTeaching t where t.classId = :classId and t.subjectId = :subjectId")
    void deleteByClassAndSubject(@Param("classId") Long classId, @Param("subjectId") Long subjectId);
}