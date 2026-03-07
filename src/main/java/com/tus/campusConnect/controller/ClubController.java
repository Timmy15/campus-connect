package com.tus.campusConnect.controller;

import com.tus.campusConnect.dto.club.ClubActionResponseDTO;
import com.tus.campusConnect.dto.club.ClubCreateRequestDTO;
import com.tus.campusConnect.dto.club.ClubResponseDTO;
import com.tus.campusConnect.dto.club.ClubUpdateRequestDTO;
import com.tus.campusConnect.model.Club;
import com.tus.campusConnect.model.User;
import com.tus.campusConnect.repository.UserRepository;
import com.tus.campusConnect.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;
    private final UserRepository userRepository;

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
        User admin = resolveAdmin(authentication);
        if (admin == null) {
            return error(HttpStatus.UNAUTHORIZED, "User not found.");
        }

        String name = normalize(request.getName());
        if (name.isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, "Club name is required.");
        }

        if (clubService.nameExists(name)) {
            return error(HttpStatus.CONFLICT, "Club already exists");
        }

        Club club = new Club();
        club.setName(name);
        club.setDescription(toNullable(request.getDescription()));
        club.setCategory(toNullable(request.getCategory()));
        club.setActive(true);
        club.setCreatedAt(LocalDateTime.now());
        club.setAdmin(admin);

        Club saved = clubService.save(club);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ClubActionResponseDTO("Club created successfully.", toDto(saved)));
    }

    @PutMapping("/admin/clubs/{id}")
    public ResponseEntity<ClubActionResponseDTO> updateClub(@PathVariable Long id,
                                                            @RequestBody ClubUpdateRequestDTO request) {
        Club club = clubService.findById(id).orElse(null);
        if (club == null) {
            return error(HttpStatus.NOT_FOUND, "Club not found.");
        }

        String name = normalize(request.getName());
        if (name.isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, "Invalid club details.");
        }

        if (!name.equalsIgnoreCase(club.getName()) && clubService.nameExists(name)) {
            return error(HttpStatus.CONFLICT, "Club already exists");
        }

        club.setName(name);
        club.setDescription(toNullable(request.getDescription()));
        club.setCategory(toNullable(request.getCategory()));

        Club saved = clubService.save(club);
        return ResponseEntity.ok(new ClubActionResponseDTO("Club updated successfully.", toDto(saved)));
    }

    @DeleteMapping("/admin/clubs/{id}")
    public ResponseEntity<ClubActionResponseDTO> deactivateClub(@PathVariable Long id) {
        Club club = clubService.findById(id).orElse(null);
        if (club == null) {
            return error(HttpStatus.NOT_FOUND, "Club not found.");
        }

        club.setActive(false);
        Club saved = clubService.save(club);
        return ResponseEntity.ok(new ClubActionResponseDTO("Club deactivated successfully.", toDto(saved)));
    }

    private User resolveAdmin(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        return userRepository.findByEmailIgnoreCase(authentication.getName()).orElse(null);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String toNullable(String value) {
        String trimmed = normalize(value);
        return trimmed.isEmpty() ? null : trimmed;
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
