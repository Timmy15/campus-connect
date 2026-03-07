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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthController authController;

    @Test
    void loginWithEmailReturnsTokenAndRole() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("admin@admin.tus.com");
        request.setPassword("Admin123");

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                "admin@admin.tus.com",
                "hash",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("admin@admin.tus.com", "Admin123"));
        when(userDetailsService.loadUserByUsername("admin@admin.tus.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        ResponseEntity<AuthResponseDTO> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isEqualTo("jwt-token");
        assertThat(response.getBody().getRole()).isEqualTo("ADMIN");
        assertThat(response.getBody().getMessage()).isEqualTo("Login successful");
    }

    @Test
    void loginWithUsernameUsesIdentifier() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("timi");
        request.setPassword("Student123");

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                "timi@student.tus.com",
                "hash",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("timi", "Student123"));
        when(userDetailsService.loadUserByUsername("timi")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        ResponseEntity<AuthResponseDTO> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRole()).isEqualTo("STUDENT");
        verify(userDetailsService).loadUserByUsername("timi");
    }

    @Test
    void registerStudentSuccessCreatesUser() {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setEmail("timi@student.tus.com");
        request.setUsername("timi");
        request.setPassword("abcde");

        when(userRepository.existsByEmailIgnoreCase("timi@student.tus.com")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("timi")).thenReturn(false);
        when(passwordEncoder.encode("abcde")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<RegisterResponseDTO> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRole()).isEqualTo("STUDENT");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("timi@student.tus.com");
        assertThat(saved.getUsername()).isEqualTo("timi");
        assertThat(saved.getRole()).isEqualTo(Role.STUDENT);
        assertThat(saved.getPasswordHash()).isEqualTo("hash");
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void registerRejectsInvalidEmailDomain() {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setEmail("timi@gmail.com");
        request.setUsername("timi");
        request.setPassword("abcde");

        ResponseEntity<RegisterResponseDTO> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("Email must end with @student.tus.com or @admin.tus.com.");
    }

    @Test
    void registerRejectsShortPassword() {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setEmail("timi@student.tus.com");
        request.setUsername("timi");
        request.setPassword("abcd");

        ResponseEntity<RegisterResponseDTO> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Password must be at least 5 characters.");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setEmail("timi@student.tus.com");
        request.setUsername("timi");
        request.setPassword("abcde");

        when(userRepository.existsByEmailIgnoreCase("timi@student.tus.com")).thenReturn(true);

        ResponseEntity<RegisterResponseDTO> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Email already registered.");
    }

    @Test
    void registerRejectsDuplicateUsername() {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setEmail("timi@student.tus.com");
        request.setUsername("timi");
        request.setPassword("abcde");

        when(userRepository.existsByEmailIgnoreCase("timi@student.tus.com")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("timi")).thenReturn(true);

        ResponseEntity<RegisterResponseDTO> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Username already taken.");
    }

    @Test
    void usernameAvailabilityReturnsAvailable() {
        when(userRepository.existsByUsernameIgnoreCase("timi")).thenReturn(false);

        ResponseEntity<UsernameAvailabilityDTO> response = authController.usernameAvailable("timi");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isAvailable()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("Username is available.");
    }

    @Test
    void usernameAvailabilityReturnsUnavailable() {
        when(userRepository.existsByUsernameIgnoreCase("timi")).thenReturn(true);

        ResponseEntity<UsernameAvailabilityDTO> response = authController.usernameAvailable("timi");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isAvailable()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Username already taken.");
    }
}
