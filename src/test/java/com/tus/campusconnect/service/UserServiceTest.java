package com.tus.campusconnect.service;

import com.tus.campusconnect.dto.user.UserProfileDTO;
import com.tus.campusconnect.exception.UnauthorizedException;
import com.tus.campusconnect.model.Role;
import com.tus.campusconnect.model.User;
import com.tus.campusconnect.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    @Test
    void getCurrentUserReturnsProfile() {
        User user = new User();
        user.setUsername("admin");
        user.setFullName("System Admin");
        user.setEmail("admin@admin.tus.com");
        user.setRole(Role.ADMIN);

        when(authentication.getName()).thenReturn("admin@admin.tus.com");
        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(
                "admin@admin.tus.com",
                "admin@admin.tus.com"
        )).thenReturn(Optional.of(user));

        UserProfileDTO dto = userService.getCurrentUser(authentication);

        assertThat(dto.getEmail()).isEqualTo("admin@admin.tus.com");
        assertThat(dto.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void getCurrentUserThrowsWhenMissing() {
        when(authentication.getName()).thenReturn("missing@admin.tus.com");
        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(
                "missing@admin.tus.com",
                "missing@admin.tus.com"
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser(authentication))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User not found.");
    }

    @Test
    void getCurrentUserThrowsWhenUnauthenticated() {
        assertThatThrownBy(() -> userService.getCurrentUser(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User not found.");
    }
}
