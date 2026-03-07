package com.tus.campusConnect.controller;

import com.tus.campusConnect.dto.club.ClubActionResponseDTO;
import com.tus.campusConnect.dto.club.ClubCreateRequestDTO;
import com.tus.campusConnect.dto.club.ClubResponseDTO;
import com.tus.campusConnect.dto.club.ClubUpdateRequestDTO;
import com.tus.campusConnect.model.Club;
import com.tus.campusConnect.model.Role;
import com.tus.campusConnect.model.User;
import com.tus.campusConnect.repository.UserRepository;
import com.tus.campusConnect.service.ClubService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubControllerTest {

    @Mock
    private ClubService clubService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ClubController clubController;

    @Test
    void getActiveClubsReturnsActiveList() {
        Club chess = new Club();
        chess.setId(1L);
        chess.setName("Chess Club");
        chess.setActive(true);

        Club robotics = new Club();
        robotics.setId(2L);
        robotics.setName("Robotics");
        robotics.setActive(true);

        when(clubService.getActiveClubs()).thenReturn(List.of(chess, robotics));

        List<ClubResponseDTO> response = clubController.getActiveClubs();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getName()).isEqualTo("Chess Club");
        assertThat(response.get(1).getName()).isEqualTo("Robotics");
    }

    @Test
    void getAllClubsReturnsList() {
        Club chess = new Club();
        chess.setId(1L);
        chess.setName("Chess Club");
        chess.setActive(true);

        when(clubService.getAllClubs()).thenReturn(List.of(chess));

        List<ClubResponseDTO> response = clubController.getAllClubs();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).isActive()).isTrue();
    }

    @Test
    void createClubSuccessPersistsClub() {
        User admin = buildAdmin();
        ClubCreateRequestDTO request = new ClubCreateRequestDTO();
        request.setName("Robotics");
        request.setDescription("Build cool robots.");
        request.setCategory("Tech");

        when(authentication.getName()).thenReturn("admin@admin.tus.com");
        when(userRepository.findByEmailIgnoreCase("admin@admin.tus.com")).thenReturn(Optional.of(admin));
        when(clubService.nameExists("Robotics")).thenReturn(false);
        when(clubService.save(any(Club.class))).thenAnswer(invocation -> {
            Club club = invocation.getArgument(0);
            club.setId(10L);
            return club;
        });

        ResponseEntity<ClubActionResponseDTO> response = clubController.createClub(request, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Club created successfully.");
        assertThat(response.getBody().getClub()).isNotNull();
        assertThat(response.getBody().getClub().getId()).isEqualTo(10L);

        ArgumentCaptor<Club> captor = ArgumentCaptor.forClass(Club.class);
        verify(clubService).save(captor.capture());
        Club saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Robotics");
        assertThat(saved.getDescription()).isEqualTo("Build cool robots.");
        assertThat(saved.getCategory()).isEqualTo("Tech");
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getAdmin()).isEqualTo(admin);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void createClubTrimsNameAndOptionalFields() {
        User admin = buildAdmin();
        ClubCreateRequestDTO request = new ClubCreateRequestDTO();
        request.setName("  Robotics  ");
        request.setDescription("   ");
        request.setCategory("  Tech  ");

        when(authentication.getName()).thenReturn("admin@admin.tus.com");
        when(userRepository.findByEmailIgnoreCase("admin@admin.tus.com")).thenReturn(Optional.of(admin));
        when(clubService.nameExists("Robotics")).thenReturn(false);
        when(clubService.save(any(Club.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<ClubActionResponseDTO> response = clubController.createClub(request, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ArgumentCaptor<Club> captor = ArgumentCaptor.forClass(Club.class);
        verify(clubService).save(captor.capture());
        Club saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Robotics");
        assertThat(saved.getCategory()).isEqualTo("Tech");
        assertThat(saved.getDescription()).isNull();
    }

    @Test
    void createClubRejectsDuplicateName() {
        ClubCreateRequestDTO request = new ClubCreateRequestDTO();
        request.setName("Robotics");

        when(authentication.getName()).thenReturn("admin@admin.tus.com");
        when(userRepository.findByEmailIgnoreCase("admin@admin.tus.com")).thenReturn(Optional.of(buildAdmin()));
        when(clubService.nameExists("Robotics")).thenReturn(true);

        ResponseEntity<ClubActionResponseDTO> response = clubController.createClub(request, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Club already exists");
        verify(clubService, never()).save(any(Club.class));
    }

    @Test
    void createClubRejectsMissingName() {
        ClubCreateRequestDTO request = new ClubCreateRequestDTO();
        request.setName("   ");

        when(authentication.getName()).thenReturn("admin@admin.tus.com");
        when(userRepository.findByEmailIgnoreCase("admin@admin.tus.com")).thenReturn(Optional.of(buildAdmin()));

        ResponseEntity<ClubActionResponseDTO> response = clubController.createClub(request, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Club name is required.");
        verify(clubService, never()).save(any(Club.class));
    }

    @Test
    void createClubRejectsMissingAdmin() {
        ClubCreateRequestDTO request = new ClubCreateRequestDTO();
        request.setName("Robotics");

        ResponseEntity<ClubActionResponseDTO> response = clubController.createClub(request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("User not found.");
        verify(clubService, never()).save(any(Club.class));
    }

    @Test
    void updateClubSuccessUpdatesClub() {
        Club club = new Club();
        club.setId(5L);
        club.setName("Chess Club");
        club.setDescription("Old");
        club.setCategory("Sports");
        club.setActive(true);

        ClubUpdateRequestDTO request = new ClubUpdateRequestDTO();
        request.setName("Chess Society");
        request.setDescription("Updated");
        request.setCategory("Recreation");

        when(clubService.findById(5L)).thenReturn(Optional.of(club));
        when(clubService.nameExists("Chess Society")).thenReturn(false);
        when(clubService.save(any(Club.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<ClubActionResponseDTO> response = clubController.updateClub(5L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Club updated successfully.");
        assertThat(response.getBody().getClub().getName()).isEqualTo("Chess Society");
    }

    @Test
    void updateClubAllowsSameNameDifferentCase() {
        Club club = new Club();
        club.setId(5L);
        club.setName("Chess Club");
        club.setDescription("Old");

        ClubUpdateRequestDTO request = new ClubUpdateRequestDTO();
        request.setName("  chess club  ");
        request.setDescription("Updated");

        when(clubService.findById(5L)).thenReturn(Optional.of(club));
        when(clubService.save(any(Club.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<ClubActionResponseDTO> response = clubController.updateClub(5L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getClub().getName()).isEqualTo("chess club");
        verify(clubService, never()).nameExists(any(String.class));
    }

    @Test
    void updateClubRejectsInvalidDetails() {
        Club club = new Club();
        club.setId(5L);
        club.setName("Chess Club");

        ClubUpdateRequestDTO request = new ClubUpdateRequestDTO();
        request.setName(" ");

        when(clubService.findById(5L)).thenReturn(Optional.of(club));

        ResponseEntity<ClubActionResponseDTO> response = clubController.updateClub(5L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid club details.");
        verify(clubService, never()).save(any(Club.class));
    }

    @Test
    void updateClubRejectsDuplicateName() {
        Club club = new Club();
        club.setId(5L);
        club.setName("Chess Club");

        ClubUpdateRequestDTO request = new ClubUpdateRequestDTO();
        request.setName("Robotics");

        when(clubService.findById(5L)).thenReturn(Optional.of(club));
        when(clubService.nameExists("Robotics")).thenReturn(true);

        ResponseEntity<ClubActionResponseDTO> response = clubController.updateClub(5L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Club already exists");
        verify(clubService, never()).save(any(Club.class));
    }

    @Test
    void updateClubReturnsNotFoundWhenMissing() {
        when(clubService.findById(10L)).thenReturn(Optional.empty());

        ResponseEntity<ClubActionResponseDTO> response = clubController.updateClub(10L, new ClubUpdateRequestDTO());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Club not found.");
        verify(clubService, never()).save(any(Club.class));
    }

    @Test
    void deactivateClubSuccessMarksInactive() {
        Club club = new Club();
        club.setId(7L);
        club.setName("Drama");
        club.setActive(true);

        when(clubService.findById(7L)).thenReturn(Optional.of(club));
        when(clubService.save(any(Club.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<ClubActionResponseDTO> response = clubController.deactivateClub(7L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Club deactivated successfully.");
        assertThat(response.getBody().getClub().isActive()).isFalse();
    }

    @Test
    void deactivateClubReturnsNotFoundWhenMissing() {
        when(clubService.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<ClubActionResponseDTO> response = clubController.deactivateClub(99L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Club not found.");
    }

    private User buildAdmin() {
        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@admin.tus.com");
        admin.setUsername("admin");
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        return admin;
    }
}
