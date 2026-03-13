package com.tus.campusconnect.controller;

import com.tus.campusconnect.dto.event.EventRegistrationItemDTO;
import com.tus.campusconnect.dto.event.EventRegistrationResponseDTO;
import com.tus.campusconnect.dto.event.EventResponseDTO;
import com.tus.campusconnect.model.Event;
import com.tus.campusconnect.model.EventRegistration;
import com.tus.campusconnect.model.RegistrationStatus;
import com.tus.campusconnect.repository.EventRegistrationRepository;
import com.tus.campusconnect.service.EventRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@Tag(name = "Event Registrations", description = "Student event registration endpoints.")
public class EventRegistrationController {

    private final EventRegistrationService eventRegistrationService;
    private final EventRegistrationRepository eventRegistrationRepository;

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
                .body(new EventRegistrationResponseDTO("Registration successful.", toDto(event, true)));
    }

    @GetMapping("/registrations")
    @Operation(summary = "List my registrations", description = "Return the current user's event registrations.")
    @ApiResponse(responseCode = "200", description = "Registrations returned.")
    public List<EventRegistrationItemDTO> getMyRegistrations(Authentication authentication) {
        return eventRegistrationService.getRegistrations(authentication)
                .stream()
                .map(this::toRegistrationDto)
                .toList();
    }

    private EventResponseDTO toDto(Event event, boolean registered) {
        String clubCategory = event.getClub() != null ? event.getClub().getCategory() : null;
        long registeredCount = eventRegistrationRepository.countByEventIdAndStatus(
                event.getId(),
                RegistrationStatus.REGISTERED
        );
        Integer capacity = event.getCapacity();
        Integer capacityRemaining = capacity == null ? null : (int) Math.max(capacity - registeredCount, 0);
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
                capacity,
                event.isActive(),
                registeredCount,
                capacityRemaining,
                registered
        );
    }

    private EventRegistrationItemDTO toRegistrationDto(EventRegistration registration) {
        Event event = registration.getEvent();
        String clubName = event != null && event.getClub() != null ? event.getClub().getName() : null;
        return new EventRegistrationItemDTO(
                registration.getId(),
                event != null ? event.getId() : null,
                event != null ? event.getTitle() : null,
                clubName,
                event != null ? event.getLocation() : null,
                event != null ? event.getStartTime() : null,
                event != null ? event.getEndTime() : null,
                registration.getStatus() != null ? registration.getStatus().name() : null,
                registration.getRegisteredAt()
        );
    }
}
