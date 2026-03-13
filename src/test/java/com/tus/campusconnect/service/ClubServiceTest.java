package com.tus.campusconnect.service;

import com.tus.campusconnect.exception.NotFoundException;
import com.tus.campusconnect.model.Club;
import com.tus.campusconnect.model.Event;
import com.tus.campusconnect.model.EventRegistration;
import com.tus.campusconnect.model.RegistrationStatus;
import com.tus.campusconnect.model.User;
import com.tus.campusconnect.repository.ClubRepository;
import com.tus.campusconnect.repository.EventRegistrationRepository;
import com.tus.campusconnect.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class ClubServiceTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRegistrationRepository eventRegistrationRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private ClubService clubService;

    private static final ZoneId ZONE = ZoneId.systemDefault();

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
    void getActiveClubsPreservesRepositoryOrdering() {
        Club alpha = new Club();
        alpha.setId(1L);
        alpha.setName("Alpha Club");
        alpha.setActive(true);

        Club beta = new Club();
        beta.setId(2L);
        beta.setName("Beta Club");
        beta.setActive(true);

        when(clubRepository.findAllByIsActiveTrueOrderByNameAsc())
                .thenReturn(List.of(alpha, beta));

        List<Club> result = clubService.getActiveClubs();

        assertThat(result).containsExactly(alpha, beta);
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

        stubClock();
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
    void createClubRejectsMissingAdmin() {
        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase("admin@admin.tus.com", "admin@admin.tus.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubService.createClub(
                new UsernamePasswordAuthenticationToken("admin@admin.tus.com", "pw"),
                "Robotics",
                "Build robots",
                "Tech"
        ))
                .isInstanceOf(com.tus.campusconnect.exception.UnauthorizedException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void createClubRejectsMissingName() {
        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@admin.tus.com");

        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase("admin@admin.tus.com", "admin@admin.tus.com"))
                .thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> clubService.createClub(
                new UsernamePasswordAuthenticationToken("admin@admin.tus.com", "pw"),
                "   ",
                "Build robots",
                "Tech"
        ))
                .isInstanceOf(com.tus.campusconnect.exception.BadRequestException.class)
                .hasMessageContaining("Club name is required");
    }

    @Test
    void createClubRejectsDuplicateName() {
        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@admin.tus.com");

        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase("admin@admin.tus.com", "admin@admin.tus.com"))
                .thenReturn(Optional.of(admin));
        when(clubRepository.existsByNameIgnoreCase("Robotics")).thenReturn(true);

        assertThatThrownBy(() -> clubService.createClub(
                new UsernamePasswordAuthenticationToken("admin@admin.tus.com", "pw"),
                "Robotics",
                "Build robots",
                "Tech"
        ))
                .isInstanceOf(com.tus.campusconnect.exception.ConflictException.class)
                .hasMessageContaining("Club already exists");
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
        when(eventRegistrationRepository.findAllByEventClubIdAndStatus(7L, RegistrationStatus.REGISTERED))
                .thenReturn(List.of());

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

    @Test
    void updateClubRejectsInvalidName() {
        Club club = new Club();
        club.setId(5L);
        club.setName("Robotics");

        when(clubRepository.findById(5L)).thenReturn(Optional.of(club));

        assertThatThrownBy(() -> clubService.updateClub(5L, "   ", null, null))
                .isInstanceOf(com.tus.campusconnect.exception.BadRequestException.class)
                .hasMessageContaining("Invalid club details");
    }

    @Test
    void updateClubRejectsDuplicateName() {
        Club club = new Club();
        club.setId(6L);
        club.setName("Robotics");

        when(clubRepository.findById(6L)).thenReturn(Optional.of(club));
        when(clubRepository.existsByNameIgnoreCase("Chess")).thenReturn(true);

        assertThatThrownBy(() -> clubService.updateClub(6L, "Chess", null, null))
                .isInstanceOf(com.tus.campusconnect.exception.ConflictException.class)
                .hasMessageContaining("Club already exists");
    }

    @Test
    void updateClubThrowsWhenMissing() {
        when(clubRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubService.updateClub(100L, "Chess", null, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Club not found");
    }

    @Test
    void deleteClubRemovesClub() {
        Club club = new Club();
        club.setId(13L);
        club.setName("Old Club");

        when(clubRepository.findById(13L)).thenReturn(Optional.of(club));

        Club deleted = clubService.deleteClub(13L);

        assertThat(deleted).isEqualTo(club);
        verify(clubRepository).delete(club);
    }

    @Test
    void deactivateClubCancelsUpcomingRegistrations() {
        Club club = new Club();
        club.setId(12L);
        club.setName("Events Club");
        club.setActive(true);

        LocalDateTime baseTime = stubClock();
        Event pastEvent = new Event();
        pastEvent.setId(1L);
        pastEvent.setStartTime(baseTime.minusDays(2));
        pastEvent.setEndTime(baseTime.minusDays(1));
        pastEvent.setClub(club);

        Event upcomingEvent = new Event();
        upcomingEvent.setId(2L);
        upcomingEvent.setStartTime(baseTime.plusHours(2));
        upcomingEvent.setEndTime(baseTime.plusHours(3));
        upcomingEvent.setClub(club);

        EventRegistration pastRegistration = new EventRegistration();
        pastRegistration.setId(10L);
        pastRegistration.setStatus(RegistrationStatus.REGISTERED);
        pastRegistration.setEvent(pastEvent);

        EventRegistration upcomingRegistration = new EventRegistration();
        upcomingRegistration.setId(11L);
        upcomingRegistration.setStatus(RegistrationStatus.REGISTERED);
        upcomingRegistration.setEvent(upcomingEvent);

        when(clubRepository.findById(12L)).thenReturn(Optional.of(club));
        when(clubRepository.save(club)).thenReturn(club);
        when(eventRegistrationRepository.findAllByEventClubIdAndStatus(12L, RegistrationStatus.REGISTERED))
                .thenReturn(List.of(pastRegistration, upcomingRegistration));
        when(eventRegistrationRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        Club deactivated = clubService.deactivateClub(12L);

        assertThat(deactivated.isActive()).isFalse();
        assertThat(pastRegistration.getStatus()).isEqualTo(RegistrationStatus.REGISTERED);
        assertThat(upcomingRegistration.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
        assertThat(upcomingRegistration.getCancelledAt()).isEqualTo(baseTime);
        verify(eventRegistrationRepository).saveAll(anyList());
    }

    private LocalDateTime stubClock() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 3, 8, 12, 0);
        Instant instant = baseTime.atZone(ZONE).toInstant();
        when(clock.instant()).thenReturn(instant);
        when(clock.getZone()).thenReturn(ZONE);
        return baseTime;
    }
}
