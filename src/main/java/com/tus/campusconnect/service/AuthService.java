package com.tus.campusconnect.service;

import com.tus.campusconnect.config.JwtService;
import com.tus.campusconnect.exception.BadRequestException;
import com.tus.campusconnect.exception.ConflictException;
import com.tus.campusconnect.exception.UnauthorizedException;
import com.tus.campusconnect.model.Role;
import com.tus.campusconnect.model.User;
import com.tus.campusconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AuthResult login(String email, String password) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
        } catch (AuthenticationException ex) {
            throw new UnauthorizedException("Wrong email/password combo.");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails userDetails)) {
            throw new UnauthorizedException("Wrong email/password combo.");
        }
        String token = jwtService.generateToken(userDetails);
        String role = userDetails.getAuthorities()
                .stream()
                .findFirst()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .orElse("STUDENT");
        return new AuthResult(token, role);
    }

    public Role register(String email, String username, String password) {
        String normalizedEmail = email == null ? "" : email.trim();
        String normalizedUsername = username == null ? "" : username.trim();
        String rawPassword = password == null ? "" : password;

        if (normalizedEmail.isEmpty() || normalizedUsername.isEmpty() || rawPassword.isEmpty()) {
            throw new BadRequestException("All fields are required.");
        }

        if (rawPassword.length() < 5) {
            throw new BadRequestException("Password must be at least 5 characters.");
        }

        Role role = determineRoleFromEmail(normalizedEmail);
        if (role == null) {
            throw new BadRequestException("Email must end with @student.tus.com or @admin.tus.com.");
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Email already registered.");
        }

        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new ConflictException("Username already taken.");
        }

        User user = new User();
        user.setFullName(normalizedUsername);
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now(clock));

        userRepository.save(user);

        return role;
    }

    public boolean usernameAvailable(String username) {
        String value = username == null ? "" : username.trim();
        if (value.isEmpty()) {
            throw new BadRequestException("Username is required.");
        }
        return !userRepository.existsByUsernameIgnoreCase(value);
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
