package com.edunex.backend.controller;

import com.edunex.backend.dto.LoginRequest;
import com.edunex.backend.dto.LoginResponse;
import com.edunex.backend.entity.User;
import com.edunex.backend.repository.UserRepository;
import com.edunex.backend.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email()).orElseThrow();
        String roleName = user.getRole() != null ? user.getRole().getName() : null;
        String token = jwtService.generateToken(user.getEmail(), roleName);

        return ResponseEntity.ok(new LoginResponse(
                token, "Bearer", user.getId(), user.getEmail(),
                user.getFirstName(), user.getLastName(), roleName));
    }
}
