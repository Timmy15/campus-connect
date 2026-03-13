package com.tus.campusconnect.controller;

import com.tus.campusconnect.dto.club.ClubActionResponseDTO;
import com.tus.campusconnect.dto.club.ClubCreateRequestDTO;
import com.tus.campusconnect.dto.club.ClubResponseDTO;
import com.tus.campusconnect.dto.club.ClubUpdateRequestDTO;
import com.tus.campusconnect.model.Club;
import com.tus.campusconnect.service.ClubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Clubs", description = "Club browsing and admin management endpoints.")
public class ClubController {

    private final ClubService clubService;

    @GetMapping("/clubs")
    @Operation(summary = "List active clubs", description = "Return all active clubs for browsing.")
    @ApiResponse(responseCode = "200", description = "Active clubs returned.")
    public List<ClubResponseDTO> getActiveClubs() {
        return clubService.getActiveClubs()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/admin/clubs")
    @Operation(summary = "List all clubs", description = "Return all clubs for admin management.")
    @ApiResponse(responseCode = "200", description = "All clubs returned.")
    @ApiResponse(responseCode = "403", description = "Forbidden.")
    public List<ClubResponseDTO> getAllClubs() {
        return clubService.getAllClubs()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @PostMapping("/admin/clubs")
    @Operation(summary = "Create a club")
    @ApiResponse(responseCode = "201", description = "Club created.")
    @ApiResponse(responseCode = "400", description = "Invalid club details.")
    @ApiResponse(responseCode = "401", description = "Unauthorized.")
    @ApiResponse(responseCode = "409", description = "Club already exists.")
    public ResponseEntity<ClubActionResponseDTO> createClub(@RequestBody ClubCreateRequestDTO request,
                                                            Authentication authentication) {
        Club saved = clubService.createClub(
                authentication,
                request.getName(),
                request.getDescription(),
                request.getCategory()
        );
        return ResponseEntity.status(201)
                .body(new ClubActionResponseDTO("Club created successfully.", toDto(saved)));
    }

    @PutMapping("/admin/clubs/{id}")
    @Operation(summary = "Update a club")
    @ApiResponse(responseCode = "200", description = "Club updated.")
    @ApiResponse(responseCode = "400", description = "Invalid club details.")
    @ApiResponse(responseCode = "404", description = "Club not found.")
    @ApiResponse(responseCode = "409", description = "Club already exists.")
    public ResponseEntity<ClubActionResponseDTO> updateClub(@PathVariable Long id,
                                                            @RequestBody ClubUpdateRequestDTO request) {
        Club saved = clubService.updateClub(
                id,
                request.getName(),
                request.getDescription(),
                request.getCategory()
        );
        return ResponseEntity.ok(new ClubActionResponseDTO("Club updated successfully.", toDto(saved)));
    }

    @DeleteMapping("/admin/clubs/{id}")
    @Operation(summary = "Deactivate a club")
    @ApiResponse(responseCode = "200", description = "Club deactivated.")
    @ApiResponse(responseCode = "404", description = "Club not found.")
    public ResponseEntity<ClubActionResponseDTO> deactivateClub(@PathVariable Long id) {
        Club saved = clubService.deactivateClub(id);
        return ResponseEntity.ok(new ClubActionResponseDTO("Club deactivated successfully.", toDto(saved)));
    }

    @DeleteMapping("/admin/clubs/{id}/delete")
    @Operation(summary = "Delete a club")
    @ApiResponse(responseCode = "200", description = "Club deleted.")
    @ApiResponse(responseCode = "404", description = "Club not found.")
    public ResponseEntity<ClubActionResponseDTO> deleteClub(@PathVariable Long id) {
        Club deleted = clubService.deleteClub(id);
        return ResponseEntity.ok(new ClubActionResponseDTO("Club deleted successfully.", toDto(deleted)));
    }

    @PutMapping("/admin/clubs/{id}/activate")
    @Operation(summary = "Activate a club", description = "Marks a previously deactivated club as active.")
    @ApiResponse(responseCode = "200", description = "Club activated successfully.")
    @ApiResponse(responseCode = "404", description = "Club not found.")
    public ResponseEntity<ClubActionResponseDTO> activateClub(@PathVariable Long id) {
        Club saved = clubService.activateClub(id);
        return ResponseEntity.ok(new ClubActionResponseDTO("Club activated successfully.", toDto(saved)));
    }

    private ClubResponseDTO toDto(Club club) {
        return new ClubResponseDTO(
                club.getId(),
                club.getName(),
                club.getDescription(),
                club.getCategory(),
                club.isActive()
        );
    }

}
