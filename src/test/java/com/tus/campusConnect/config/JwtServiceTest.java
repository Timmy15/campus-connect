package com.tus.campusConnect.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        String secret = Base64.getEncoder()
                .encodeToString("supersecretkeysupersecretkey1234".getBytes(StandardCharsets.UTF_8));
        ReflectionTestUtils.setField(jwtService, "jwtSecret", secret);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 1000L * 60 * 60);
    }

    @Test
    void generateTokenAndExtractUsername() {
        UserDetails userDetails = new User(
                "timi@student.tus.com",
                "hash",
                List.of()
        );

        String token = jwtService.generateToken(userDetails);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("timi@student.tus.com");
    }

    @Test
    void tokenValidityMatchesUser() {
        UserDetails userDetails = new User(
                "timi@student.tus.com",
                "hash",
                List.of()
        );
        UserDetails otherUser = new User(
                "other@student.tus.com",
                "hash",
                List.of()
        );

        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }
}
