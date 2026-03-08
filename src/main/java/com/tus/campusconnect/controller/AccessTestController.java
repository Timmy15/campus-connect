package com.tus.campusconnect.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Access Tests", description = "Simple endpoints for role-based access checks.")
public class AccessTestController {

    @GetMapping("/api/admin/ping")
    @Operation(summary = "Admin ping")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Admin access confirmed."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Forbidden.")
    })
    public ResponseEntity<String> adminPing() {
        return ResponseEntity.ok("admin-ok");
    }

    @GetMapping("/api/student/ping")
    @Operation(summary = "Student ping")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Student access confirmed."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "403", description = "Forbidden.")
    })
    public ResponseEntity<String> studentPing() {
        return ResponseEntity.ok("student-ok");
    }
}
