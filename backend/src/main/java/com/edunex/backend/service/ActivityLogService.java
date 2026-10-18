package com.edunex.backend.service;

import com.edunex.backend.dto.ActivityLogResponse;
import com.edunex.backend.dto.PageResponse;
import com.edunex.backend.entity.ActivityLog;
import com.edunex.backend.entity.User;
import com.edunex.backend.repository.ActivityLogRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository logRepository;
    private final UserRepository userRepository;

    @Transactional
    public void registerForEmail(String action, String entityType, String email, String ip) {
        User user = email == null ? null : userRepository.findByEmail(email).orElse(null);
        ActivityLog log = new ActivityLog();
        log.setAction(action.length() > 200 ? action.substring(0, 200) : action);
        log.setEntityType(entityType);
        log.setIpAddress(ip);
        log.setUser(user);
        logRepository.save(log);
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityLogResponse> search(String q, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<ActivityLog> result = (q == null || q.isBlank())
                ? logRepository.findAllByOrderByTimestampDesc(pageable)
                : logRepository.search(q.trim(), pageable);
        List<ActivityLogResponse> items = result.getContent().stream().map(this::toResponse).toList();
        return new PageResponse<>(items, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    private ActivityLogResponse toResponse(ActivityLog l) {
        User u = l.getUser();
        return new ActivityLogResponse(l.getId(), l.getAction(), l.getEntityType(), l.getTimestamp(),
                l.getIpAddress(), u != null ? u.getEmail() : null,
                u != null ? u.getFirstName() + " " + u.getLastName() : null);
    }
}