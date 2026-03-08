package com.tus.campusconnect.controller;

import com.tus.campusconnect.dto.auth.AuthResponseDTO;
import com.tus.campusconnect.dto.auth.LoginRequestDTO;
import com.tus.campusconnect.dto.auth.RegisterRequestDTO;
import com.tus.campusconnect.dto.auth.RegisterResponseDTO;
import com.tus.campusconnect.dto.auth.UsernameAvailabilityDTO;
import com.tus.campusconnect.exception.BadRequestException;
import com.tus.campusconnect.exception.ConflictException;
import com.tus.campusconnect.exception.UnauthorizedException;
import com.tus.campusconnect.model.Role;
import com.tus.campusconnect.service.AuthResult;
import com.tus.campusconnect.service.AuthService;
import com.tus.campusconnect.testsupport.TestUsers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private final TestUsers users = TestUsers.getInstance();

    @Test
    void loginWithEmailReturnsTokenAndRole() {
        LoginRequestDTO request = loginRequest(users.getAdminEmail(), users.getAdminPassword());

        when(authService.login(users.getAdminEmail(), users.getAdminPassword()))
                .thenReturn(new AuthResult("jwt-token", "ADMIN"));

        ResponseEntity<AuthResponseDTO> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isEqualTo("jwt-token");
        assertThat(response.getBody().getRole()).isEqualTo("ADMIN");
        assertThat(response.getBody().getMessage()).isEqualTo("Login successful");
    }

    @Test
    void loginWithUsernameUsesIdentifier() {
        LoginRequestDTO request = loginRequest(users.getStudentUsername(), users.getStudentPassword());

        when(authService.login(users.getStudentUsername(), users.getStudentPassword()))
                .thenReturn(new AuthResult("jwt-token", "STUDENT"));

        ResponseEntity<AuthResponseDTO> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRole()).isEqualTo("STUDENT");
    }

    @Test
    void loginReturnsUnauthorizedWhenCredentialsInvalid() {
        LoginRequestDTO request = loginRequest(users.getStudentEmail(), users.getWrongPassword());

        when(authService.login(users.getStudentEmail(), users.getWrongPassword()))
                .thenThrow(new UnauthorizedException("Wrong email/password combo."));

        ResponseEntity<AuthResponseDTO> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Wrong email/password combo.");
    }

    @Test
    void registerStudentSuccessCreatesUser() {
        RegisterRequestDTO request = registerRequest("timi@student.tus.com", "timi", "abcde");

        when(authService.register("timi@student.tus.com", "timi", "abcde"))
                .thenReturn(Role.STUDENT);

        ResponseEntity<RegisterResponseDTO> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRole()).isEqualTo("STUDENT");
    }

    @Test
    void registerRejectsInvalidEmailDomain() {
        RegisterRequestDTO request = registerRequest("timi@gmail.com", "timi", "abcde");

        when(authService.register("timi@gmail.com", "timi", "abcde"))
                .thenThrow(new BadRequestException("Email must end with @student.tus.com or @admin.tus.com."));

        ResponseEntity<RegisterResponseDTO> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("Email must end with @student.tus.com or @admin.tus.com.");
    }

    @Test
    void registerRejectsShortPassword() {
        RegisterRequestDTO request = registerRequest("timi@student.tus.com", "timi", "abcd");

        when(authService.register("timi@student.tus.com", "timi", "abcd"))
                .thenThrow(new BadRequestException("Password must be at least 5 characters."));

        ResponseEntity<RegisterResponseDTO> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Password must be at least 5 characters.");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequestDTO request = registerRequest("timi@student.tus.com", "timi", "abcde");

        when(authService.register("timi@student.tus.com", "timi", "abcde"))
                .thenThrow(new ConflictException("Email already registered."));

        ResponseEntity<RegisterResponseDTO> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Email already registered.");
    }

    @Test
    void registerRejectsDuplicateUsername() {
        RegisterRequestDTO request = registerRequest("timi@student.tus.com", "timi", "abcde");

        when(authService.register("timi@student.tus.com", "timi", "abcde"))
                .thenThrow(new ConflictException("Username already taken."));

        ResponseEntity<RegisterResponseDTO> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Username already taken.");
    }

    @Test
    void usernameAvailabilityReturnsAvailable() {
        when(authService.usernameAvailable("timi"))
                .thenReturn(true);

        ResponseEntity<UsernameAvailabilityDTO> response = authController.usernameAvailable("timi");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isAvailable()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("Username is available.");
    }

    @Test
    void usernameAvailabilityReturnsUnavailable() {
        when(authService.usernameAvailable("timi"))
                .thenReturn(false);

        ResponseEntity<UsernameAvailabilityDTO> response = authController.usernameAvailable("timi");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isAvailable()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Username already taken.");
    }

    @Test
    void usernameAvailabilityReturnsBadRequestWhenMissing() {
        when(authService.usernameAvailable(""))
                .thenThrow(new BadRequestException("Username is required."));

        ResponseEntity<UsernameAvailabilityDTO> response = authController.usernameAvailable("");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Username is required.");
    }

    private LoginRequestDTO loginRequest(String email, String password) {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private RegisterRequestDTO registerRequest(String email, String username, String password) {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setEmail(email);
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }
}
