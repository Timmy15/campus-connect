package com.tus.campusconnect.service;

import com.tus.campusconnect.exception.BadRequestException;
import com.tus.campusconnect.exception.NotFoundException;
import com.tus.campusconnect.model.Club;
import com.tus.campusconnect.model.Event;
import com.tus.campusconnect.repository.ClubRepository;
import com.tus.campusconnect.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ClubRepository clubRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    void createEventPersistsForActiveClub() {
        Club club = new Club();
        club.setId(1L);
        club.setName("Robotics");
        club.setActive(true);

        when(clubRepository.findById(1L)).thenReturn(Optional.of(club));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        Event created = eventService.createEvent(
                1L,
                "Demo Day",
                "Showcase",
                "Lab",
                start,
                start.plusHours(2),
                50
        );

        assertThat(created.getTitle()).isEqualTo("Demo Day");
        assertThat(created.getClub()).isEqualTo(club);
        assertThat(created.getCapacity()).isEqualTo(50);
    }

    @Test
    void createEventRejectsPastDate() {
        Club club = new Club();
        club.setId(2L);
        club.setName("Chess");
        club.setActive(true);

        when(clubRepository.findById(2L)).thenReturn(Optional.of(club));

        LocalDateTime past = LocalDateTime.now().minusDays(1);
        assertThatThrownBy(() -> eventService.createEvent(
                2L,
                "Blitz Night",
                "Fast games",
                "Room 1",
                past,
                past.plusHours(1),
                20
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Start time must be in the future");
    }

    @Test
    void updateEventUpdatesDetails() {
        Event event = new Event();
        event.setId(10L);
        event.setTitle("Old");
        event.setLocation("Hall");

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);

        LocalDateTime start = LocalDateTime.now().plusDays(2);
        Event updated = eventService.updateEvent(
                10L,
                "Updated",
                "Updated description",
                "New Hall",
                start,
                start.plusHours(1),
                80
        );

        assertThat(updated.getTitle()).isEqualTo("Updated");
        assertThat(updated.getLocation()).isEqualTo("New Hall");
        assertThat(updated.getCapacity()).isEqualTo(80);
    }

    @Test
    void updateEventRejectsInvalidCapacity() {
        Event event = new Event();
        event.setId(11L);

        when(eventRepository.findById(11L)).thenReturn(Optional.of(event));

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        assertThatThrownBy(() -> eventService.updateEvent(
                11L,
                "Workshop",
                null,
                "Lab",
                start,
                start.plusHours(1),
                0
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Capacity must be greater than 0");
    }

    @Test
    void updateEventThrowsWhenMissing() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        assertThatThrownBy(() -> eventService.updateEvent(
                99L,
                "Workshop",
                null,
                "Lab",
                start,
                null,
                10
        ))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Event not found");
    }
}
