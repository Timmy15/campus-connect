package com.tus.campusconnect.service;

import com.tus.campusconnect.exception.NotFoundException;
import com.tus.campusconnect.model.Club;
import com.tus.campusconnect.model.User;
import com.tus.campusconnect.repository.ClubRepository;
import com.tus.campusconnect.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubServiceTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ClubService clubService;

    @Test
    void getActiveClubsUsesRepository() {
        Club club = new Club();
        club.setId(1L);
        club.setName("Chess Club");
        club.setActive(true);

        when(clubRepository.findAllByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(club));

        List<Club> result = clubService.getActiveClubs();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Chess Club");
    }

    @Test
    void getAllClubsUsesRepository() {
        when(clubRepository.findAllByOrderByNameAsc()).thenReturn(List.of());

        List<Club> result = clubService.getAllClubs();

        assertThat(result).isEmpty();
    }

    @Test
    void createClubPersistsNewClub() {
        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@admin.tus.com");

        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase("admin@admin.tus.com", "admin@admin.tus.com"))
                .thenReturn(Optional.of(admin));
        when(clubRepository.existsByNameIgnoreCase("Robotics")).thenReturn(false);
        when(clubRepository.save(any(Club.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Club created = clubService.createClub(
                new UsernamePasswordAuthenticationToken("admin@admin.tus.com", "pw"),
                "Robotics",
                "Build robots",
                "Tech"
        );

        assertThat(created.getName()).isEqualTo("Robotics");
        assertThat(created.getAdmin()).isEqualTo(admin);
        assertThat(created.isActive()).isTrue();
    }

    @Test
    void activateClubMarksActive() {
        Club club = new Club();
        club.setId(5L);
        club.setName("Drama");
        club.setActive(false);

        when(clubRepository.findById(5L)).thenReturn(Optional.of(club));
        when(clubRepository.save(club)).thenReturn(club);

        Club activated = clubService.activateClub(5L);

        assertThat(activated.isActive()).isTrue();
    }

    @Test
    void deactivateClubMarksInactive() {
        Club club = new Club();
        club.setId(7L);
        club.setName("Chess");
        club.setActive(true);

        when(clubRepository.findById(7L)).thenReturn(Optional.of(club));
        when(clubRepository.save(club)).thenReturn(club);

        Club deactivated = clubService.deactivateClub(7L);

        assertThat(deactivated.isActive()).isFalse();
    }

    @Test
    void activateClubThrowsWhenMissing() {
        when(clubRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubService.activateClub(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Club not found");
    }
}
