package com.tus.campusConnect.controller;

import com.tus.campusConnect.model.Role;
import com.tus.campusConnect.model.User;
import com.tus.campusConnect.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserController userController;

    @Test
    void getCurrentUserReturnsProfile() {
        User user = new User();
        user.setEmail("admin@admin.tus.com");
        user.setUsername("admin");
        user.setFullName("System Admin");
        user.setRole(Role.ADMIN);

        when(authentication.getName()).thenReturn("admin@admin.tus.com");
        when(userRepository.findByEmailIgnoreCase("admin@admin.tus.com")).thenReturn(Optional.of(user));

        ResponseEntity<?> response = userController.getCurrentUser(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void getCurrentUserReturnsUnauthorizedWhenUserMissing() {
        when(authentication.getName()).thenReturn("missing@admin.tus.com");
        when(userRepository.findByEmailIgnoreCase("missing@admin.tus.com")).thenReturn(Optional.empty());

        ResponseEntity<?> response = userController.getCurrentUser(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getCurrentUserReturnsUnauthorizedWhenUnauthenticated() {
        ResponseEntity<?> response = userController.getCurrentUser(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
