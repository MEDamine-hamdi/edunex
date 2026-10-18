package com.edunex.backend.repository;

import com.edunex.backend.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    Page<ActivityLog> findAllByOrderByTimestampDesc(Pageable pageable);

    List<ActivityLog> findByTimestampAfter(LocalDateTime since);

    @Query("select l from ActivityLog l left join l.user u "
            + "where lower(l.action) like lower(concat('%', :q, '%')) "
            + "or lower(u.email) like lower(concat('%', :q, '%')) "
            + "order by l.timestamp desc")
    Page<ActivityLog> search(@Param("q") String q, Pageable pageable);
}