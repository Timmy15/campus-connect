package com.tus.campusconnect.config;

import com.tus.campusconnect.model.Role;
import com.tus.campusconnect.model.User;
import com.tus.campusconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DataInitialiser implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SeedProperties seedProperties;

    @Override
    public void run(String... args) {
        seedUser(seedProperties.getAdmin(), Role.ADMIN);
        seedUser(seedProperties.getStudent(), Role.STUDENT);
    }

    private void seedUser(SeedProperties.SeedUser seedUser, Role role) {
        if (seedUser == null || isBlank(seedUser.getEmail()) || isBlank(seedUser.getPassword())) {
            log.warn("Skipping seed user for role {} due to missing credentials.", role);
            return;
        }

        String email = seedUser.getEmail().trim();
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            return;
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(resolveUsername(seedUser));
        user.setFullName(resolveFullName(seedUser));
        user.setPasswordHash(passwordEncoder.encode(seedUser.getPassword()));
        user.setRole(role);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);
        log.info("Seeded {} user: {}", role, email);
    }

    private String resolveUsername(SeedProperties.SeedUser seedUser) {
        if (!isBlank(seedUser.getUsername())) {
            return seedUser.getUsername().trim();
        }
        String email = seedUser.getEmail().trim();
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    private String resolveFullName(SeedProperties.SeedUser seedUser) {
        if (!isBlank(seedUser.getFullName())) {
            return seedUser.getFullName().trim();
        }
        return resolveUsername(seedUser);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
