package com.edunex.backend.security;

import com.edunex.backend.entity.UserPermission;
import com.edunex.backend.repository.UserPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** The rights of a person = the rights of the role + what was granted to this person - what was denied to this person. */
@Service
@RequiredArgsConstructor
public class UserRightsService {

    private final UserPermissionRepository repository;

    @Transactional(readOnly = true)
    public Collection<? extends GrantedAuthority> effective(UserDetails details) {
        List<UserPermission> overrides = repository.findByUserEmail(details.getUsername());
        if (overrides.isEmpty()) {
            return details.getAuthorities();
        }
        Set<GrantedAuthority> out = new LinkedHashSet<GrantedAuthority>(details.getAuthorities());
        for (UserPermission o : overrides) {
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(o.getCode());
            if ("GRANT".equals(o.getEffect())) {
                out.add(authority);
            } else {
                out.remove(authority);
            }
        }
        return out;
    }
}