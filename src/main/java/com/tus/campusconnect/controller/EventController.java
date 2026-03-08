package com.tus.campusconnect.controller;

import com.tus.campusconnect.dto.event.EventActionResponseDTO;
import com.tus.campusconnect.dto.event.EventCreateRequestDTO;
import com.tus.campusconnect.dto.event.EventResponseDTO;
import com.tus.campusconnect.dto.event.EventUpdateRequestDTO;
import com.tus.campusconnect.model.Event;
import com.tus.campusconnect.model.RegistrationStatus;
import com.tus.campusconnect.model.User;
import com.tus.campusconnect.repository.EventRegistrationRepository;
import com.tus.campusconnect.repository.UserRepository;
import com.tus.campusconnect.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Club event management and browsing endpoints.")
public class EventController {

    private final EventService eventService;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final UserRepository userRepository;

    @GetMapping("/events")
    @Operation(summary = "List active events", description = "Return all active events for browsing.")
    @ApiResponse(responseCode = "200", description = "Active events returned.")
    public List<EventResponseDTO> getActiveEvents(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return eventService.getActiveEvents()
                .stream()
                .map(event -> toDto(event, userId))
                .toList();
    }

    @GetMapping("/admin/clubs/{clubId}/events")
    @Operation(summary = "List club events", description = "Return events linked to a specific club.")
    @ApiResponse(responseCode = "200", description = "Club events returned.")
    @ApiResponse(responseCode = "404", description = "Club not found.")
    public List<EventResponseDTO> getClubEvents(@PathVariable Long clubId) {
        return eventService.getEventsForClub(clubId)
                .stream()
                .map(event -> toDto(event, null))
                .toList();
    }

    @PostMapping("/admin/clubs/{clubId}/events")
    @Operation(summary = "Create an event")
    @ApiResponse(responseCode = "201", description = "Event created.")
    @ApiResponse(responseCode = "400", description = "Invalid event details.")
    @ApiResponse(responseCode = "404", description = "Club not found.")
    public ResponseEntity<EventActionResponseDTO> createEvent(@PathVariable Long clubId,
                                                              @RequestBody EventCreateRequestDTO request) {
        Event saved = eventService.createEvent(
                clubId,
                request.getTitle(),
                request.getDescription(),
                request.getLocation(),
                request.getStartTime(),
                request.getEndTime(),
                request.getCapacity()
        );
        return ResponseEntity.status(201)
                .body(new EventActionResponseDTO("Event created successfully.", toDto(saved)));
    }

    @PutMapping("/admin/events/{eventId}")
    @Operation(summary = "Update an event")
    @ApiResponse(responseCode = "200", description = "Event updated.")
    @ApiResponse(responseCode = "400", description = "Invalid event details.")
    @ApiResponse(responseCode = "404", description = "Event not found.")
    public ResponseEntity<EventActionResponseDTO> updateEvent(@PathVariable Long eventId,
                                                              @RequestBody EventUpdateRequestDTO request) {
        Event saved = eventService.updateEvent(
                eventId,
                request.getTitle(),
                request.getDescription(),
                request.getLocation(),
                request.getStartTime(),
                request.getEndTime(),
                request.getCapacity()
        );
        return ResponseEntity.ok(new EventActionResponseDTO("Event updated successfully.", toDto(saved)));
    }

    private EventResponseDTO toDto(Event event) {
        return toDto(event, null);
    }

    private EventResponseDTO toDto(Event event, Long userId) {
        String clubCategory = event.getClub() != null ? event.getClub().getCategory() : null;
        long registeredCount = eventRegistrationRepository.countByEventIdAndStatus(
                event.getId(),
                RegistrationStatus.REGISTERED
        );
        Integer capacity = event.getCapacity();
        Integer capacityRemaining = capacity == null ? null : (int) Math.max(capacity - registeredCount, 0);
        boolean registered = userId != null && eventRegistrationRepository.existsByEventIdAndUserIdAndStatus(
                event.getId(),
                userId,
                RegistrationStatus.REGISTERED
        );
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

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }

        String identifier = authentication.getName();
        User user = userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier)
                .orElse(null);
        return user != null ? user.getId() : null;
    }
}
