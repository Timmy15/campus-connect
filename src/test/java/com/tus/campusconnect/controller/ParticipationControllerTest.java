package com.tus.campusconnect.controller;

import com.tus.campusconnect.dto.admin.ParticipationClubStatDTO;
import com.tus.campusconnect.dto.admin.ParticipationEventStatDTO;
import com.tus.campusconnect.dto.admin.ParticipationStatsResponseDTO;
import com.tus.campusconnect.service.ParticipationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParticipationControllerTest {

    @Mock
    private ParticipationService participationService;

    @InjectMocks
    private ParticipationController participationController;

    @Test
    void getParticipationStatsReturnsResponse() {
        ParticipationStatsResponseDTO stats = new ParticipationStatsResponseDTO(
                List.of(new ParticipationEventStatDTO(1L, "Open Day", "Chess Club", 4L)),
                List.of(new ParticipationClubStatDTO(2L, "Chess Club", 7L))
        );
        when(participationService.getParticipationStats()).thenReturn(stats);

        ParticipationStatsResponseDTO response = participationController.getParticipationStats();

        assertThat(response.getRegistrationsPerEvent()).hasSize(1);
        assertThat(response.getTopClubs()).hasSize(1);
        assertThat(response.getRegistrationsPerEvent().get(0).getEventTitle()).isEqualTo("Open Day");
        assertThat(response.getTopClubs().get(0).getClubName()).isEqualTo("Chess Club");
    }
}
