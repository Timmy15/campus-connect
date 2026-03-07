package com.tus.campusConnect.service;

import com.tus.campusConnect.model.Role;
import com.tus.campusConnect.model.User;
import com.tus.campusConnect.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void loadUserByEmailReturnsDetails() {
        User user = new User();
        user.setEmail("timi@student.tus.com");
        user.setPasswordHash("hash");
        user.setRole(Role.STUDENT);
        user.setActive(true);

        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(
                "timi@student.tus.com",
                "timi@student.tus.com"
        )).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("timi@student.tus.com");

        assertThat(details.getUsername()).isEqualTo("timi@student.tus.com");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_STUDENT");
    }

    @Test
    void loadUserByUsernameReturnsDetails() {
        User user = new User();
        user.setEmail("timi@student.tus.com");
        user.setPasswordHash("hash");
        user.setRole(Role.STUDENT);
        user.setActive(true);

        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase("timi", "timi"))
                .thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("timi");

        assertThat(details.getUsername()).isEqualTo("timi@student.tus.com");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void loadUserByUsernameReturnsDisabledWhenInactive() {
        User user = new User();
        user.setEmail("inactive@student.tus.com");
        user.setPasswordHash("hash");
        user.setRole(Role.STUDENT);
        user.setActive(false);

        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase("inactive", "inactive"))
                .thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("inactive");

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsernameThrowsWhenMissing() {
        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase("missing", "missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }
}
