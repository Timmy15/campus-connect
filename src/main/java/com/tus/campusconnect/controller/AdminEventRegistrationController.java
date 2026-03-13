package com.tus.campusconnect.controller;

import com.tus.campusconnect.dto.common.MessageResponseDTO;
import com.tus.campusconnect.dto.event.EventRegistrationDetailDTO;
import com.tus.campusconnect.model.EventRegistration;
import com.tus.campusconnect.model.User;
import com.tus.campusconnect.service.EventRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Event Registrations", description = "Admin event registration endpoints.")
public class AdminEventRegistrationController {

    private final EventRegistrationService eventRegistrationService;

    @GetMapping("/events/{eventId}/registrations")
    @Operation(summary = "List event registrations", description = "Return registrations for a specific event.")
    @ApiResponse(responseCode = "200", description = "Registrations returned.")
    @ApiResponse(responseCode = "404", description = "Event not found.")
    public List<EventRegistrationDetailDTO> getRegistrationsForEvent(@PathVariable Long eventId) {
        return eventRegistrationService.getRegistrationsForEvent(eventId)
                .stream()
                .map(this::toDetailDto)
                .toList();
    }

    @DeleteMapping("/events/{eventId}/registrations/{registrationId}")
    @Operation(summary = "Unregister a student", description = "Cancel a student's registration for an event.")
    @ApiResponse(responseCode = "200", description = "Registration cancelled.")
    @ApiResponse(responseCode = "404", description = "Registration not found.")
    @ApiResponse(responseCode = "409", description = "Registration already cancelled.")
    public ResponseEntity<MessageResponseDTO> unregisterStudent(@PathVariable Long eventId,
                                                                @PathVariable Long registrationId) {
        eventRegistrationService.unregisterRegistration(eventId, registrationId);
        return ResponseEntity.ok(new MessageResponseDTO("Registration cancelled."));
    }

    private EventRegistrationDetailDTO toDetailDto(EventRegistration registration) {
        User user = registration.getUser();
        return new EventRegistrationDetailDTO(
                registration.getId(),
                user != null ? user.getId() : null,
                user != null ? user.getFullName() : null,
                user != null ? user.getEmail() : null,
                user != null ? user.getUsername() : null,
                registration.getStatus() != null ? registration.getStatus().name() : null,
                registration.getRegisteredAt()
        );
    }
}
