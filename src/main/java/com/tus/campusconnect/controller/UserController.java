package com.tus.campusconnect.controller;

import com.tus.campusconnect.dto.user.UserProfileDTO;
import com.tus.campusconnect.exception.UnauthorizedException;
import com.tus.campusconnect.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile endpoints.")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    public ResponseEntity<UserProfileDTO> getCurrentUser(Authentication authentication) {
        try {
            UserProfileDTO dto = userService.getCurrentUser(authentication);
            return ResponseEntity.ok(dto);
        } catch (UnauthorizedException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
