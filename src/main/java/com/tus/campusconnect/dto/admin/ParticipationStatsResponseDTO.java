package com.tus.campusconnect.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ParticipationStatsResponseDTO {
    private List<ParticipationEventStatDTO> registrationsPerEvent;
    private List<ParticipationClubStatDTO> topClubs;
}
