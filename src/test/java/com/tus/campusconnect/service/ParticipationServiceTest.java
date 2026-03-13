package com.tus.campusconnect.service;

import com.tus.campusconnect.dto.admin.ParticipationClubStatDTO;
import com.tus.campusconnect.dto.admin.ParticipationEventStatDTO;
import com.tus.campusconnect.dto.admin.ParticipationStatsResponseDTO;
import com.tus.campusconnect.model.RegistrationStatus;
import com.tus.campusconnect.repository.EventRegistrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParticipationServiceTest {

    @Mock
    private EventRegistrationRepository eventRegistrationRepository;

    @InjectMocks
    private ParticipationService participationService;

    @Test
    void getParticipationStatsReturnsAggregatedLists() {
        List<ParticipationEventStatDTO> eventStats = List.of(
                new ParticipationEventStatDTO(10L, "Hackathon", "Tech Club", 5L)
        );
        List<ParticipationClubStatDTO> clubStats = List.of(
                new ParticipationClubStatDTO(3L, "Tech Club", 12L)
        );

        when(eventRegistrationRepository.findEventRegistrationStats(RegistrationStatus.REGISTERED))
                .thenReturn(eventStats);
        when(eventRegistrationRepository.findClubRegistrationStats(RegistrationStatus.REGISTERED))
                .thenReturn(clubStats);

        ParticipationStatsResponseDTO response = participationService.getParticipationStats();

        assertThat(response.getRegistrationsPerEvent()).isEqualTo(eventStats);
        assertThat(response.getTopClubs()).isEqualTo(clubStats);
    }

    @Test
    void getParticipationStatsReturnsEmptyWhenNoData() {
        when(eventRegistrationRepository.findEventRegistrationStats(RegistrationStatus.REGISTERED))
                .thenReturn(List.of());
        when(eventRegistrationRepository.findClubRegistrationStats(RegistrationStatus.REGISTERED))
                .thenReturn(List.of());

        ParticipationStatsResponseDTO response = participationService.getParticipationStats();

        assertThat(response.getRegistrationsPerEvent()).isEmpty();
        assertThat(response.getTopClubs()).isEmpty();
    }
}
