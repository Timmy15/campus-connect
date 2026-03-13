package com.tus.campusconnect.controller;

import com.tus.campusconnect.dto.admin.ParticipationStatsResponseDTO;
import com.tus.campusconnect.service.ParticipationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Participation Reporting", description = "Admin participation reporting endpoints.")
public class ParticipationController {

    private final ParticipationService participationService;

    @GetMapping("/participation")
    @Operation(
            summary = "Get participation statistics",
            description = "Return registrations per event and top clubs by total registrations."
    )
    @ApiResponse(responseCode = "200", description = "Participation statistics returned.")
    public ParticipationStatsResponseDTO getParticipationStats() {
        return participationService.getParticipationStats();
    }
}
