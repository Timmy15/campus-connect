package com.tus.campusconnect.controller;

import com.tus.campusconnect.dto.user.UserProfileDTO;
import com.tus.campusconnect.exception.UnauthorizedException;
import com.tus.campusconnect.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserController userController;

    @Test
    void getCurrentUserReturnsProfile() {
        UserProfileDTO dto = new UserProfileDTO("admin", "System Admin", "admin@admin.tus.com", "ROLE_ADMIN");

        when(userService.getCurrentUser(authentication)).thenReturn(dto);

        ResponseEntity<UserProfileDTO> response = userController.getCurrentUser(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getEmail()).isEqualTo("admin@admin.tus.com");
    }

    @Test
    void getCurrentUserReturnsUnauthorizedWhenUserMissing() {
        when(userService.getCurrentUser(authentication))
                .thenThrow(new UnauthorizedException("User not found."));

        ResponseEntity<UserProfileDTO> response = userController.getCurrentUser(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getCurrentUserReturnsUnauthorizedWhenUnauthenticated() {
        when(userService.getCurrentUser(null))
                .thenThrow(new UnauthorizedException("User not found."));

        ResponseEntity<UserProfileDTO> response = userController.getCurrentUser(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
