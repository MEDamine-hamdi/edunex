package com.edunex.backend.service;

import com.edunex.backend.dto.SettingResponse;
import com.edunex.backend.entity.PlatformSetting;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.PlatformSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PlatformSettingService {

    private static final Pattern KEY = Pattern.compile("^[A-Za-z0-9._-]{1,150}$");

    private final PlatformSettingRepository repository;

    @Transactional(readOnly = true)
    public List<SettingResponse> list() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(PlatformSetting::getConfigKey))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SettingResponse upsert(String key, String value) {
        if (!KEY.matcher(key).matches()) {
            throw new BadRequestException("Invalid key: use letters, digits, dots, dashes or underscores (150 max)");
        }
        PlatformSetting s = repository.findByConfigKey(key).orElseGet(() -> {
            PlatformSetting n = new PlatformSetting();
            n.setConfigKey(key);
            return n;
        });
        s.setConfigValue(value);
        return toResponse(repository.save(s));
    }

    @Transactional
    public void delete(String key) {
        PlatformSetting s = repository.findByConfigKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Setting not found"));
        repository.delete(s);
    }

    private SettingResponse toResponse(PlatformSetting s) {
        return new SettingResponse(s.getId(), s.getConfigKey(), s.getConfigValue());
    }
}