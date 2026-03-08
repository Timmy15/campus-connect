package com.tus.campusconnect.controller;

import com.tus.campusconnect.dto.event.EventActionResponseDTO;
import com.tus.campusconnect.dto.event.EventCreateRequestDTO;
import com.tus.campusconnect.dto.event.EventResponseDTO;
import com.tus.campusconnect.dto.event.EventUpdateRequestDTO;
import com.tus.campusconnect.exception.BadRequestException;
import com.tus.campusconnect.exception.NotFoundException;
import com.tus.campusconnect.model.Club;
import com.tus.campusconnect.model.Event;
import com.tus.campusconnect.service.EventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventController eventController;

    @Test
    void getActiveEventsReturnsList() {
        Event event = event(1L, "Demo Day", "Robotics");
        when(eventService.getActiveEvents()).thenReturn(List.of(event));

        List<EventResponseDTO> response = eventController.getActiveEvents();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getTitle()).isEqualTo("Demo Day");
        assertThat(response.get(0).getClubName()).isEqualTo("Robotics");
    }

    @Test
    void getClubEventsReturnsList() {
        Event event = event(2L, "Meetup", "Chess");
        when(eventService.getEventsForClub(5L)).thenReturn(List.of(event));

        List<EventResponseDTO> response = eventController.getClubEvents(5L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getClubId()).isEqualTo(5L);
    }

    @Test
    void createEventReturnsCreated() {
        EventCreateRequestDTO request = new EventCreateRequestDTO();
        request.setTitle("Hackathon");
        request.setDescription("Build stuff");
        request.setLocation("Lab");
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        request.setCapacity(30);

        Event saved = event(10L, "Hackathon", "Tech Club");
        when(eventService.createEvent(any(Long.class), any(), any(), any(), any(), any(), any()))
                .thenReturn(saved);

        ResponseEntity<EventActionResponseDTO> response = eventController.createEvent(7L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Event created successfully.");
        assertThat(response.getBody().getEvent().getId()).isEqualTo(10L);
    }

    @Test
    void createEventRejectsInvalidDetails() {
        EventCreateRequestDTO request = new EventCreateRequestDTO();
        request.setTitle("Hackathon");

        when(eventService.createEvent(any(Long.class), any(), any(), any(), any(), any(), any()))
                .thenThrow(new BadRequestException("Capacity must be greater than 0."));

        assertThatThrownBy(() -> eventController.createEvent(7L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Capacity must be greater than 0.");
    }

    @Test
    void updateEventReturnsOk() {
        EventUpdateRequestDTO request = new EventUpdateRequestDTO();
        request.setTitle("Updated");
        request.setLocation("Hall");
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setCapacity(40);

        Event saved = event(12L, "Updated", "Drama");
        when(eventService.updateEvent(any(Long.class), any(), any(), any(), any(), any(), any()))
                .thenReturn(saved);

        ResponseEntity<EventActionResponseDTO> response = eventController.updateEvent(12L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Event updated successfully.");
        assertThat(response.getBody().getEvent().getTitle()).isEqualTo("Updated");
    }

    @Test
    void updateEventReturnsNotFoundWhenMissing() {
        when(eventService.updateEvent(any(Long.class), any(), any(), any(), any(), any(), any()))
                .thenThrow(new NotFoundException("Event not found."));

        EventUpdateRequestDTO request = new EventUpdateRequestDTO();
        assertThatThrownBy(() -> eventController.updateEvent(99L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Event not found.");
    }

    @Test
    void getClubEventsThrowsWhenClubMissing() {
        when(eventService.getEventsForClub(9L))
                .thenThrow(new NotFoundException("Club not found."));

        assertThatThrownBy(() -> eventController.getClubEvents(9L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Club not found.");
    }

    private Event event(Long id, String title, String clubName) {
        Club club = new Club();
        club.setId(5L);
        club.setName(clubName);
        club.setActive(true);

        Event event = new Event();
        event.setId(id);
        event.setTitle(title);
        event.setLocation("Main Hall");
        event.setCapacity(50);
        event.setStartTime(LocalDateTime.now().plusDays(1));
        event.setClub(club);
        event.setActive(true);
        return event;
    }
}
