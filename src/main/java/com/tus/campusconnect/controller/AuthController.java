package com.tus.campusconnect.controller;

import com.tus.campusconnect.dto.auth.AuthResponseDTO;
import com.tus.campusconnect.dto.auth.LoginRequestDTO;
import com.tus.campusconnect.dto.auth.RegisterRequestDTO;
import com.tus.campusconnect.dto.auth.RegisterResponseDTO;
import com.tus.campusconnect.dto.auth.UsernameAvailabilityDTO;
import com.tus.campusconnect.service.AuthService;
import com.tus.campusconnect.service.AuthResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication and registration endpoints.")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Log in", description = "Authenticate with email/username and password.")
    @ApiResponse(responseCode = "200", description = "Login successful.")
    @ApiResponse(responseCode = "401", description = "Invalid credentials.")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody LoginRequestDTO request) {
        AuthResult result = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(new AuthResponseDTO(result.token(), result.role(), "Login successful"));
    }

    @PostMapping("/register")
    @Operation(summary = "Register", description = "Create a new user account using a valid email domain.")
    @ApiResponse(responseCode = "201", description = "Registration successful.")
    @ApiResponse(responseCode = "400", description = "Invalid registration details.")
    @ApiResponse(responseCode = "409", description = "Email or username already taken.")
    public ResponseEntity<RegisterResponseDTO> register(@RequestBody RegisterRequestDTO request) {
        var role = authService.register(
                request.getEmail(),
                request.getUsername(),
                request.getPassword()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterResponseDTO("Registration successful. Please log in.", role.name()));
    }

    @GetMapping("/username-available")
    @Operation(summary = "Check username availability")
    @ApiResponse(responseCode = "200", description = "Availability returned.")
    @ApiResponse(responseCode = "400", description = "Username is required.")
    public ResponseEntity<UsernameAvailabilityDTO> usernameAvailable(@RequestParam(required = false) String username) {
        boolean available = authService.usernameAvailable(username);
        String message = available ? "Username is available." : "Username already taken.";
        return ResponseEntity.ok(new UsernameAvailabilityDTO(available, message));
    }
}
