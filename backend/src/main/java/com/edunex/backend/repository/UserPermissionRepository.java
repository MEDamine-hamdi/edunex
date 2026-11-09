package com.edunex.backend.repository;

import com.edunex.backend.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {

    List<UserPermission> findByUserId(Long userId);

    @Query("select p from UserPermission p where p.userId = (select u.id from User u where u.email = :email)")
    List<UserPermission> findByUserEmail(@Param("email") String email);

    @Modifying
    @Query("delete from UserPermission p where p.userId = :userId")
    void deleteByUser(@Param("userId") Long userId);
}