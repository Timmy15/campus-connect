package com.tus.campusconnect.controller;

import com.tus.campusconnect.dto.auth.AuthResponseDTO;
import com.tus.campusconnect.dto.auth.LoginRequestDTO;
import com.tus.campusconnect.dto.auth.RegisterRequestDTO;
import com.tus.campusconnect.dto.auth.RegisterResponseDTO;
import com.tus.campusconnect.dto.auth.UsernameAvailabilityDTO;
import com.tus.campusconnect.exception.BadRequestException;
import com.tus.campusconnect.exception.ConflictException;
import com.tus.campusconnect.exception.UnauthorizedException;
import com.tus.campusconnect.service.AuthService;
import com.tus.campusconnect.service.AuthResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody LoginRequestDTO request) {
        try {
            AuthResult result = authService.login(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(new AuthResponseDTO(result.token(), result.role(), "Login successful"));
        } catch (UnauthorizedException ex) {
            return ResponseEntity.status(ex.getStatus())
                    .body(new AuthResponseDTO(null, null, ex.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(@RequestBody RegisterRequestDTO request) {
        try {
            var role = authService.register(
                    request.getEmail(),
                    request.getUsername(),
                    request.getPassword()
            );
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new RegisterResponseDTO("Registration successful. Please log in.", role.name()));
        } catch (BadRequestException | ConflictException ex) {
            return ResponseEntity.status(ex.getStatus())
                    .body(new RegisterResponseDTO(ex.getMessage(), null));
        }
    }

    @GetMapping("/username-available")
    public ResponseEntity<UsernameAvailabilityDTO> usernameAvailable(@RequestParam(required = false) String username) {
        try {
            boolean available = authService.usernameAvailable(username);
            String message = available ? "Username is available." : "Username already taken.";
            return ResponseEntity.ok(new UsernameAvailabilityDTO(available, message));
        } catch (BadRequestException ex) {
            return ResponseEntity.status(ex.getStatus())
                    .body(new UsernameAvailabilityDTO(false, ex.getMessage()));
        }
    }
}
