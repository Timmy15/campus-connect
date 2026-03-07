package com.tus.campusConnect.config;

import com.tus.campusConnect.model.Role;
import com.tus.campusConnect.model.User;
import com.tus.campusConnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitialiser implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createAdminUser();
        createStudentUser();
    }

    private void createAdminUser() {
        String adminEmail = "admin@admin.tus.com";

        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setFullName("System Admin");
            admin.setEmail(adminEmail);
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("Admin123"));
            admin.setRole(Role.ADMIN);
            admin.setActive(true);
            admin.setCreatedAt(LocalDateTime.now());

            userRepository.save(admin);
            System.out.println("Admin user created: " + adminEmail);
        }
    }

    private void createStudentUser() {
        String studentEmail = "student@student.tus.com";

        if (userRepository.findByEmail(studentEmail).isEmpty()) {
            User student = new User();
            student.setFullName("Demo Student");
            student.setEmail(studentEmail);
            student.setUsername("student");
            student.setPasswordHash(passwordEncoder.encode("Student123"));
            student.setRole(Role.STUDENT);
            student.setActive(true);
            student.setCreatedAt(LocalDateTime.now());

            userRepository.save(student);
            System.out.println("Student user created: " + studentEmail);
        }
    }
}
