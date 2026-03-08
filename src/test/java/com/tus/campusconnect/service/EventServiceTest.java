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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.List;

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

    @Mock
    private Clock clock;

    @InjectMocks
    private EventService eventService;

    private static final ZoneId ZONE = ZoneId.systemDefault();

    @Test
    void createEventPersistsForActiveClub() {
        Club club = new Club();
        club.setId(1L);
        club.setName("Robotics");
        club.setActive(true);

        when(clubRepository.findById(1L)).thenReturn(Optional.of(club));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime baseTime = stubClock();
        LocalDateTime start = baseTime.plusDays(1);
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

        LocalDateTime baseTime = stubClock();
        LocalDateTime past = baseTime.minusDays(1);
        LocalDateTime pastEnd = past.plusHours(1);
        assertThatThrownBy(() -> eventService.createEvent(
                2L,
                "Blitz Night",
                "Fast games",
                "Room 1",
                past,
                pastEnd,
                20
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Start time must be in the future");
    }

    @Test
    void createEventRejectsInactiveClub() {
        Club club = new Club();
        club.setId(3L);
        club.setName("Photography");
        club.setActive(false);

        when(clubRepository.findById(3L)).thenReturn(Optional.of(club));

        LocalDateTime start = LocalDateTime.of(2026, 3, 9, 12, 0);
        LocalDateTime end = start.plusHours(1);
        assertThatThrownBy(() -> eventService.createEvent(
                3L,
                "Photo Walk",
                "Lens practice",
                "Campus",
                start,
                end,
                10
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Club is inactive.");
    }

    @Test
    void createEventThrowsWhenClubMissing() {
        when(clubRepository.findById(4L)).thenReturn(Optional.empty());

        LocalDateTime start = LocalDateTime.of(2026, 3, 9, 12, 0);
        assertThatThrownBy(() -> eventService.createEvent(
                4L,
                "Meetup",
                "Networking",
                "Hall",
                start,
                null,
                15
        ))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Club not found");
    }

    @Test
    void updateEventUpdatesDetails() {
        Event event = new Event();
        event.setId(10L);
        event.setTitle("Old");
        event.setLocation("Hall");

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);

        LocalDateTime baseTime = stubClock();
        LocalDateTime start = baseTime.plusDays(2);
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

        LocalDateTime start = LocalDateTime.of(2026, 3, 9, 12, 0);
        LocalDateTime end = start.plusHours(1);
        assertThatThrownBy(() -> eventService.updateEvent(
                11L,
                "Workshop",
                null,
                "Lab",
                start,
                end,
                0
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Capacity must be greater than 0");
    }

    @Test
    void updateEventThrowsWhenMissing() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        LocalDateTime start = LocalDateTime.of(2026, 3, 9, 12, 0);
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

    @Test
    void getActiveEventsFiltersInactiveClubs() {
        Club activeClub = new Club();
        activeClub.setId(1L);
        activeClub.setName("Active Club");
        activeClub.setActive(true);

        Club inactiveClub = new Club();
        inactiveClub.setId(2L);
        inactiveClub.setName("Inactive Club");
        inactiveClub.setActive(false);

        Event activeEvent = new Event();
        activeEvent.setId(1L);
        activeEvent.setTitle("Open Day");
        activeEvent.setActive(true);
        activeEvent.setStartTime(LocalDateTime.now().plusDays(1));
        activeEvent.setClub(activeClub);

        Event hiddenEvent = new Event();
        hiddenEvent.setId(2L);
        hiddenEvent.setTitle("Hidden");
        hiddenEvent.setActive(true);
        hiddenEvent.setStartTime(LocalDateTime.now().plusDays(1));
        hiddenEvent.setClub(inactiveClub);

        when(eventRepository.findAllByIsActiveTrueOrderByStartTimeAsc())
                .thenReturn(List.of(activeEvent, hiddenEvent));

        List<Event> result = eventService.getActiveEvents();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Open Day");
    }

    @Test
    void getEventsForClubThrowsWhenClubMissing() {
        when(clubRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEventsForClub(77L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Club not found");
    }

    private LocalDateTime stubClock() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 3, 8, 12, 0);
        Instant instant = baseTime.atZone(ZONE).toInstant();
        when(clock.instant()).thenReturn(instant);
        when(clock.getZone()).thenReturn(ZONE);
        return baseTime;
    }
}
