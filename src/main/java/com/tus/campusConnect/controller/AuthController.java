package com.tus.campusConnect.controller;

import com.tus.campusConnect.config.JwtService;
import com.tus.campusConnect.dto.auth.AuthResponseDTO;
import com.tus.campusConnect.dto.auth.LoginRequestDTO;
import com.tus.campusConnect.dto.auth.RegisterRequestDTO;
import com.tus.campusConnect.dto.auth.RegisterResponseDTO;
import com.tus.campusConnect.dto.auth.UsernameAvailabilityDTO;
import com.tus.campusConnect.model.Role;
import com.tus.campusConnect.model.User;
import com.tus.campusConnect.repository.UserRepository;
import com.tus.campusConnect.service.CustomUserDetailsService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody LoginRequestDTO request) {

        // Authenticate credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Load user details
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());

        // Generate JWT
        String token = jwtService.generateToken(userDetails);

        // Extract role from authorities
        String role = userDetails.getAuthorities()
                .stream()
                .findFirst()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .orElse("STUDENT");

        return ResponseEntity.ok(
                new AuthResponseDTO(token, role, "Login successful")
        );
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@RequestBody RegisterRequestDTO request) {
        String email = request.getEmail() == null ? "" : request.getEmail().trim();
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String password = request.getPassword() == null ? "" : request.getPassword();

        if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new RegisterResponseDTO("All fields are required.", null));
        }

        if (password.length() < 5) {
            return ResponseEntity.badRequest()
                    .body(new RegisterResponseDTO("Password must be at least 5 characters.", null));
        }

        Role role = determineRoleFromEmail(email);
        if (role == null) {
            return ResponseEntity.badRequest()
                    .body(new RegisterResponseDTO("Email must end with @student.tus.com or @admin.tus.com.", null));
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RegisterResponseDTO("Email already registered.", null));
        }

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RegisterResponseDTO("Username already taken.", null));
        }

        User user = new User();
        user.setFullName(username);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterResponseDTO("Registration successful. Please log in.", role.name()));
    }

    @GetMapping("/username-available")
    public ResponseEntity<UsernameAvailabilityDTO> usernameAvailable(@RequestParam(required = false) String username) {
        String value = username == null ? "" : username.trim();
        if (value.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new UsernameAvailabilityDTO(false, "Username is required."));
        }

        boolean available = !userRepository.existsByUsernameIgnoreCase(value);
        String message = available ? "Username is available." : "Username already taken.";
        return ResponseEntity.ok(new UsernameAvailabilityDTO(available, message));
    }

    private Role determineRoleFromEmail(String email) {
        String lower = email.toLowerCase();
        if (lower.endsWith("@admin.tus.com")) {
            return Role.ADMIN;
        }
        if (lower.endsWith("@student.tus.com")) {
            return Role.STUDENT;
        }
        return null;
    }
}
