package com.edunex.backend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MyPermissionsController {

    /** The rights of the signed-in person (role rights plus personal ones): the menu uses them. */
    @GetMapping("/api/users/me/permissions")
    public List<String> mine(Authentication auth) {
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).filter(a -> !a.startsWith("ROLE_")).sorted().toList();
    }
}