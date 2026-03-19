package com.tus.campusconnect.service;

import com.tus.campusconnect.config.JwtService;
import com.tus.campusconnect.exception.BadRequestException;
import com.tus.campusconnect.exception.ConflictException;
import com.tus.campusconnect.exception.UnauthorizedException;
import com.tus.campusconnect.model.Role;
import com.tus.campusconnect.model.User;
import com.tus.campusconnect.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Clock clock;

    @InjectMocks
    private AuthService authService;

    private static final ZoneId ZONE = ZoneId.systemDefault();

    @Test
    void loginReturnsTokenAndRole() {
        UserDetails details = org.springframework.security.core.userdetails.User
                .withUsername("admin@admin.tus.com")
                .password("hash")
                .authorities("ROLE_ADMIN")
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                details,
                null,
                details.getAuthorities()
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateToken(details)).thenReturn("jwt-token");

        AuthResult result = authService.login("admin@admin.tus.com", "secret");

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.role()).isEqualTo("ADMIN");
    }

    @Test
    void loginThrowsUnauthorizedWhenCredentialsInvalid() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login("bad@tus.com", "wrong"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Wrong email/password combo.");
    }

    @Test
    void loginThrowsUnauthorizedWhenPrincipalUnexpected() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("user", null);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        assertThatThrownBy(() -> authService.login("user", "pw"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Wrong email/password combo.");
    }

    @Test
    void registerCreatesStudentAccount() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 3, 8, 12, 0);
        Instant instant = baseTime.atZone(ZONE).toInstant();
        when(clock.instant()).thenReturn(instant);
        when(clock.getZone()).thenReturn(ZONE);

        when(userRepository.existsByEmailIgnoreCase("timi@student.tus.com")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("timi")).thenReturn(false);
        when(passwordEncoder.encode("abcde")).thenReturn("hash");

        Role role = authService.register("timi@student.tus.com", "timi", "abcde");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo("timi@student.tus.com");
        assertThat(saved.getUsername()).isEqualTo("timi");
        assertThat(saved.getRole()).isEqualTo(Role.STUDENT);
        assertThat(saved.getCreatedAt()).isEqualTo(baseTime);
        assertThat(saved.isActive()).isTrue();
        assertThat(role).isEqualTo(Role.STUDENT);
    }

    @Test
    void registerRejectsInvalidEmailDomain() {
        assertThatThrownBy(() -> authService.register("timi@gmail.com", "timi", "abcde"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email must end with @student.tus.com or @admin.tus.com.");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("timi@student.tus.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("timi@student.tus.com", "timi", "abcde"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already registered.");
    }

    @Test
    void usernameAvailabilityReturnsFalseWhenTaken() {
        when(userRepository.existsByUsernameIgnoreCase("timi")).thenReturn(true);

        assertThat(authService.usernameAvailable("timi")).isFalse();
    }

    @Test
    void usernameAvailabilityRejectsBlank() {
        assertThatThrownBy(() -> authService.usernameAvailable(" "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Username is required.");
    }
}
