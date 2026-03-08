package com.tus.campusconnect.controller;

import com.tus.campusconnect.dto.event.EventRegistrationResponseDTO;
import com.tus.campusconnect.dto.event.EventResponseDTO;
import com.tus.campusconnect.model.Event;
import com.tus.campusconnect.service.EventRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@Tag(name = "Event Registrations", description = "Student event registration endpoints.")
public class EventRegistrationController {

    private final EventRegistrationService eventRegistrationService;

    @PostMapping("/events/{eventId}/register")
    @Operation(summary = "Register for an event", description = "Register the current user for an event.")
    @ApiResponse(responseCode = "201", description = "Registration successful.")
    @ApiResponse(responseCode = "400", description = "Event is inactive.")
    @ApiResponse(responseCode = "401", description = "Unauthorized.")
    @ApiResponse(responseCode = "404", description = "Event not found.")
    @ApiResponse(responseCode = "409", description = "Already registered or capacity reached.")
    public ResponseEntity<EventRegistrationResponseDTO> registerForEvent(@PathVariable Long eventId,
                                                                         Authentication authentication) {
        Event event = eventRegistrationService.registerForEvent(eventId, authentication);
        return ResponseEntity.status(201)
                .body(new EventRegistrationResponseDTO("Registration successful.", toDto(event)));
    }

    private EventResponseDTO toDto(Event event) {
        String clubCategory = event.getClub() != null ? event.getClub().getCategory() : null;
        return new EventResponseDTO(
                event.getId(),
                event.getClub() != null ? event.getClub().getId() : null,
                event.getClub() != null ? event.getClub().getName() : null,
                clubCategory,
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getStartTime(),
                event.getEndTime(),
                event.getCapacity(),
                event.isActive()
        );
    }
}
