package com.edunex.backend.tenant;

import com.edunex.backend.entity.PlatformAdministrator;
import com.edunex.backend.entity.User;
import com.edunex.backend.repository.UniversityRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Which university a signed-in person belongs to, and whether it may be used (not suspended). */
@Component
@RequiredArgsConstructor
public class TenantAccess {

    private record Entry(Long universityId, boolean allowed, long expires) {
    }

    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final Map<String, Entry> cache = new ConcurrentHashMap<>();

    public boolean allows(String email) {
        return entry(email).allowed();
    }

    public Long universityOf(String email) {
        return entry(email).universityId();
    }

    public void invalidate() {
        cache.clear();
    }

    private Entry entry(String email) {
        long now = System.currentTimeMillis();
        Entry cached = cache.get(email);
        if (cached != null && cached.expires() > now) {
            return cached;
        }
        Entry fresh = load(email, now + 20_000);
        cache.put(email, fresh);
        return fresh;
    }

    private Entry load(String email, long expires) {
        User u = userRepository.findByEmail(email).orElse(null);
        if (u == null) {
            return new Entry(null, false, expires);
        }
        if (u instanceof PlatformAdministrator) {
            return new Entry(null, true, expires);
        }
        Long universityId = u.getUniversityId();
        if (universityId == null) {
            return new Entry(null, false, expires);
        }
        boolean active = universityRepository.findById(universityId).map(x -> "ACTIVE".equals(x.getStatus())).orElse(false);
        return new Entry(universityId, active, expires);
    }
}