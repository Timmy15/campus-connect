package com.tus.campusconnect.controller;

import com.tus.campusconnect.dto.event.EventActionResponseDTO;
import com.tus.campusconnect.dto.event.EventCreateRequestDTO;
import com.tus.campusconnect.dto.event.EventResponseDTO;
import com.tus.campusconnect.dto.event.EventUpdateRequestDTO;
import com.tus.campusconnect.exception.BadRequestException;
import com.tus.campusconnect.exception.NotFoundException;
import com.tus.campusconnect.model.Event;
import com.tus.campusconnect.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Club event management and browsing endpoints.")
public class EventController {

    private final EventService eventService;

    @GetMapping("/events")
    @Operation(summary = "List active events", description = "Return all active events for browsing.")
    @ApiResponse(responseCode = "200", description = "Active events returned.")
    public List<EventResponseDTO> getActiveEvents() {
        return eventService.getActiveEvents()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/admin/clubs/{clubId}/events")
    @Operation(summary = "List club events", description = "Return events linked to a specific club.")
    @ApiResponse(responseCode = "200", description = "Club events returned.")
    @ApiResponse(responseCode = "404", description = "Club not found.")
    public List<EventResponseDTO> getClubEvents(@PathVariable Long clubId) {
        return eventService.getEventsForClub(clubId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @PostMapping("/admin/clubs/{clubId}/events")
    @Operation(summary = "Create an event")
    @ApiResponse(responseCode = "201", description = "Event created.")
    @ApiResponse(responseCode = "400", description = "Invalid event details.")
    @ApiResponse(responseCode = "404", description = "Club not found.")
    public ResponseEntity<EventActionResponseDTO> createEvent(@PathVariable Long clubId,
                                                              @RequestBody EventCreateRequestDTO request) {
        try {
            Event saved = eventService.createEvent(
                    clubId,
                    request.getTitle(),
                    request.getDescription(),
                    request.getLocation(),
                    request.getStartTime(),
                    request.getEndTime(),
                    request.getCapacity()
            );
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new EventActionResponseDTO("Event created successfully.", toDto(saved)));
        } catch (BadRequestException | NotFoundException ex) {
            return error(ex.getStatus(), ex.getMessage());
        }
    }

    @PutMapping("/admin/events/{eventId}")
    @Operation(summary = "Update an event")
    @ApiResponse(responseCode = "200", description = "Event updated.")
    @ApiResponse(responseCode = "400", description = "Invalid event details.")
    @ApiResponse(responseCode = "404", description = "Event not found.")
    public ResponseEntity<EventActionResponseDTO> updateEvent(@PathVariable Long eventId,
                                                              @RequestBody EventUpdateRequestDTO request) {
        try {
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
        } catch (BadRequestException | NotFoundException ex) {
            return error(ex.getStatus(), ex.getMessage());
        }
    }

    private EventResponseDTO toDto(Event event) {
        return new EventResponseDTO(
                event.getId(),
                event.getClub() != null ? event.getClub().getId() : null,
                event.getClub() != null ? event.getClub().getName() : null,
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getStartTime(),
                event.getEndTime(),
                event.getCapacity(),
                event.isActive()
        );
    }

    private ResponseEntity<EventActionResponseDTO> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new EventActionResponseDTO(message, null));
    }
}
