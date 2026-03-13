package com.tus.campusconnect.service;

import com.tus.campusconnect.dto.admin.ParticipationClubStatDTO;
import com.tus.campusconnect.dto.admin.ParticipationEventStatDTO;
import com.tus.campusconnect.dto.admin.ParticipationStatsResponseDTO;
import com.tus.campusconnect.model.RegistrationStatus;
import com.tus.campusconnect.repository.EventRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ParticipationService {

    private final EventRegistrationRepository eventRegistrationRepository;

    public ParticipationStatsResponseDTO getParticipationStats() {
        List<ParticipationEventStatDTO> eventStats = eventRegistrationRepository
                .findEventRegistrationStats(RegistrationStatus.REGISTERED);
        List<ParticipationClubStatDTO> clubStats = eventRegistrationRepository
                .findClubRegistrationStats(RegistrationStatus.REGISTERED);
        return new ParticipationStatsResponseDTO(eventStats, clubStats);
    }
}
