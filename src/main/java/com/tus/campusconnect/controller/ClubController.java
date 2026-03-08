package com.tus.campusconnect.controller;

import com.tus.campusconnect.dto.club.ClubActionResponseDTO;
import com.tus.campusconnect.dto.club.ClubCreateRequestDTO;
import com.tus.campusconnect.dto.club.ClubResponseDTO;
import com.tus.campusconnect.dto.club.ClubUpdateRequestDTO;
import com.tus.campusconnect.exception.BadRequestException;
import com.tus.campusconnect.exception.ConflictException;
import com.tus.campusconnect.exception.NotFoundException;
import com.tus.campusconnect.exception.UnauthorizedException;
import com.tus.campusconnect.model.Club;
import com.tus.campusconnect.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;

    @GetMapping("/clubs")
    public List<ClubResponseDTO> getActiveClubs() {
        return clubService.getActiveClubs()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/admin/clubs")
    public List<ClubResponseDTO> getAllClubs() {
        return clubService.getAllClubs()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @PostMapping("/admin/clubs")
    public ResponseEntity<ClubActionResponseDTO> createClub(@RequestBody ClubCreateRequestDTO request,
                                                            Authentication authentication) {
        try {
            Club saved = clubService.createClub(
                    authentication,
                    request.getName(),
                    request.getDescription(),
                    request.getCategory()
            );
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ClubActionResponseDTO("Club created successfully.", toDto(saved)));
        } catch (BadRequestException | ConflictException | UnauthorizedException ex) {
            return error(ex.getStatus(), ex.getMessage());
        }
    }

    @PutMapping("/admin/clubs/{id}")
    public ResponseEntity<ClubActionResponseDTO> updateClub(@PathVariable Long id,
                                                            @RequestBody ClubUpdateRequestDTO request) {
        try {
            Club saved = clubService.updateClub(
                    id,
                    request.getName(),
                    request.getDescription(),
                    request.getCategory()
            );
            return ResponseEntity.ok(new ClubActionResponseDTO("Club updated successfully.", toDto(saved)));
        } catch (BadRequestException | ConflictException | NotFoundException ex) {
            return error(ex.getStatus(), ex.getMessage());
        }
    }

    @DeleteMapping("/admin/clubs/{id}")
    public ResponseEntity<ClubActionResponseDTO> deactivateClub(@PathVariable Long id) {
        try {
            Club saved = clubService.deactivateClub(id);
            return ResponseEntity.ok(new ClubActionResponseDTO("Club deactivated successfully.", toDto(saved)));
        } catch (NotFoundException ex) {
            return error(ex.getStatus(), ex.getMessage());
        }
    }

    @PutMapping("/admin/clubs/{id}/activate")
    public ResponseEntity<ClubActionResponseDTO> activateClub(@PathVariable Long id) {
        try {
            Club saved = clubService.activateClub(id);
            return ResponseEntity.ok(new ClubActionResponseDTO("Club activated successfully.", toDto(saved)));
        } catch (NotFoundException ex) {
            return error(ex.getStatus(), ex.getMessage());
        }
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

    private ResponseEntity<ClubActionResponseDTO> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ClubActionResponseDTO(message, null));
    }
}
