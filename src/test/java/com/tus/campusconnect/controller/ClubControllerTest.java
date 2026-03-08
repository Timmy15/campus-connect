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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubControllerTest {

    @Mock
    private ClubService clubService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ClubController clubController;

    @Test
    void getActiveClubsReturnsActiveList() {
        Club chess = club(1L, "Chess Club", null, null, true);
        Club robotics = club(2L, "Robotics", null, null, true);

        when(clubService.getActiveClubs()).thenReturn(List.of(chess, robotics));

        List<ClubResponseDTO> response = clubController.getActiveClubs();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getName()).isEqualTo("Chess Club");
        assertThat(response.get(1).getName()).isEqualTo("Robotics");
    }

    @Test
    void getAllClubsReturnsList() {
        Club chess = club(1L, "Chess Club", null, null, true);

        when(clubService.getAllClubs()).thenReturn(List.of(chess));

        List<ClubResponseDTO> response = clubController.getAllClubs();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).isActive()).isTrue();
    }

    @Test
    void createClubSuccessPersistsClub() {
        ClubCreateRequestDTO request = createRequest("Robotics", "Build cool robots.", "Tech");
        Club saved = club(10L, "Robotics", "Build cool robots.", "Tech", true);

        when(clubService.createClub(authentication, "Robotics", "Build cool robots.", "Tech"))
                .thenReturn(saved);

        ResponseEntity<ClubActionResponseDTO> response = clubController.createClub(request, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Club created successfully.");
        assertThat(response.getBody().getClub()).isNotNull();
        assertThat(response.getBody().getClub().getId()).isEqualTo(10L);
    }

    @Test
    void createClubRejectsDuplicateName() {
        ClubCreateRequestDTO request = createRequest("Robotics", null, null);

        when(clubService.createClub(authentication, "Robotics", null, null))
                .thenThrow(new ConflictException("Club already exists"));

        ResponseEntity<ClubActionResponseDTO> response = clubController.createClub(request, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Club already exists");
    }

    @Test
    void createClubRejectsMissingName() {
        ClubCreateRequestDTO request = createRequest("   ", null, null);

        when(clubService.createClub(authentication, "   ", null, null))
                .thenThrow(new BadRequestException("Club name is required."));

        ResponseEntity<ClubActionResponseDTO> response = clubController.createClub(request, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Club name is required.");
    }

    @Test
    void createClubRejectsMissingAdmin() {
        ClubCreateRequestDTO request = createRequest("Robotics", null, null);

        when(clubService.createClub(null, "Robotics", null, null))
                .thenThrow(new UnauthorizedException("User not found."));

        ResponseEntity<ClubActionResponseDTO> response = clubController.createClub(request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("User not found.");
    }

    @Test
    void updateClubSuccessUpdatesClub() {
        ClubUpdateRequestDTO request = updateRequest("Chess Society", "Updated", "Recreation");
        Club saved = club(5L, "Chess Society", "Updated", "Recreation", true);

        when(clubService.updateClub(5L, "Chess Society", "Updated", "Recreation"))
                .thenReturn(saved);

        ResponseEntity<ClubActionResponseDTO> response = clubController.updateClub(5L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Club updated successfully.");
        assertThat(response.getBody().getClub().getName()).isEqualTo("Chess Society");
    }

    @Test
    void updateClubRejectsInvalidDetails() {
        ClubUpdateRequestDTO request = updateRequest(" ", null, null);

        when(clubService.updateClub(5L, " ", null, null))
                .thenThrow(new BadRequestException("Invalid club details."));

        ResponseEntity<ClubActionResponseDTO> response = clubController.updateClub(5L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid club details.");
    }

    @Test
    void updateClubRejectsDuplicateName() {
        ClubUpdateRequestDTO request = updateRequest("Robotics", null, null);

        when(clubService.updateClub(5L, "Robotics", null, null))
                .thenThrow(new ConflictException("Club already exists"));

        ResponseEntity<ClubActionResponseDTO> response = clubController.updateClub(5L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Club already exists");
    }

    @Test
    void updateClubReturnsNotFoundWhenMissing() {
        when(clubService.updateClub(any(Long.class), any(), any(), any()))
                .thenThrow(new NotFoundException("Club not found."));

        ResponseEntity<ClubActionResponseDTO> response = clubController.updateClub(10L, new ClubUpdateRequestDTO());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Club not found.");
    }

    @Test
    void deactivateClubSuccessMarksInactive() {
        Club saved = club(7L, "Drama", null, null, false);

        when(clubService.deactivateClub(7L)).thenReturn(saved);

        ResponseEntity<ClubActionResponseDTO> response = clubController.deactivateClub(7L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Club deactivated successfully.");
        assertThat(response.getBody().getClub().isActive()).isFalse();
    }

    @Test
    void deactivateClubReturnsNotFoundWhenMissing() {
        when(clubService.deactivateClub(99L))
                .thenThrow(new NotFoundException("Club not found."));

        ResponseEntity<ClubActionResponseDTO> response = clubController.deactivateClub(99L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Club not found.");
    }

    private ClubCreateRequestDTO createRequest(String name, String description, String category) {
        ClubCreateRequestDTO request = new ClubCreateRequestDTO();
        request.setName(name);
        request.setDescription(description);
        request.setCategory(category);
        return request;
    }

    private ClubUpdateRequestDTO updateRequest(String name, String description, String category) {
        ClubUpdateRequestDTO request = new ClubUpdateRequestDTO();
        request.setName(name);
        request.setDescription(description);
        request.setCategory(category);
        return request;
    }

    private Club club(Long id, String name, String description, String category, boolean active) {
        Club club = new Club();
        club.setId(id);
        club.setName(name);
        club.setDescription(description);
        club.setCategory(category);
        club.setActive(active);
        return club;
    }
}
