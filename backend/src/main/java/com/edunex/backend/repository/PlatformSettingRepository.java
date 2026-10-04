package com.edunex.backend.repository;

import com.edunex.backend.entity.PlatformSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, Long> {
    Optional<PlatformSetting> findByConfigKey(String configKey);
}
