package com.tus.campusconnect.service;

import com.tus.campusconnect.exception.BadRequestException;
import com.tus.campusconnect.exception.ConflictException;
import com.tus.campusconnect.exception.NotFoundException;
import com.tus.campusconnect.exception.UnauthorizedException;
import com.tus.campusconnect.model.Club;
import com.tus.campusconnect.model.Event;
import com.tus.campusconnect.model.EventRegistration;
import com.tus.campusconnect.model.RegistrationStatus;
import com.tus.campusconnect.model.User;
import com.tus.campusconnect.repository.EventRegistrationRepository;
import com.tus.campusconnect.repository.EventRepository;
import com.tus.campusconnect.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventRegistrationServiceTest {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRegistrationRepository eventRegistrationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private EventRegistrationService eventRegistrationService;

    @Test
    void registerForEventStoresRegistration() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("student@student.tus.com");

        User user = new User();
        user.setId(22L);
        user.setEmail("student@student.tus.com");
        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(eq("student@student.tus.com"), eq("student@student.tus.com")))
                .thenReturn(Optional.of(user));

        Club club = new Club();
        club.setId(5L);
        club.setActive(true);

        Event event = new Event();
        event.setId(9L);
        event.setActive(true);
        event.setCapacity(1);
        event.setClub(club);
        when(eventRepository.findById(9L)).thenReturn(Optional.of(event));

        when(eventRegistrationRepository.existsByEventIdAndUserIdAndStatus(9L, 22L, RegistrationStatus.REGISTERED))
                .thenReturn(false);
        when(eventRegistrationRepository.countByEventIdAndStatus(9L, RegistrationStatus.REGISTERED))
                .thenReturn(0L);
        when(eventRegistrationRepository.save(any(EventRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime now = stubClock();

        Event result = eventRegistrationService.registerForEvent(9L, authentication);

        assertThat(result).isEqualTo(event);

        ArgumentCaptor<EventRegistration> captor = ArgumentCaptor.forClass(EventRegistration.class);
        verify(eventRegistrationRepository).save(captor.capture());
        EventRegistration saved = captor.getValue();
        assertThat(saved.getEvent()).isEqualTo(event);
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getStatus()).isEqualTo(RegistrationStatus.REGISTERED);
        assertThat(saved.getRegisteredAt()).isEqualTo(now);
    }

    @Test
    void registerForEventRejectsAlreadyRegistered() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("student@student.tus.com");

        User user = new User();
        user.setId(11L);
        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(eq("student@student.tus.com"), eq("student@student.tus.com")))
                .thenReturn(Optional.of(user));

        Club club = new Club();
        club.setActive(true);
        Event event = new Event();
        event.setId(7L);
        event.setActive(true);
        event.setClub(club);
        event.setCapacity(2);

        when(eventRepository.findById(7L)).thenReturn(Optional.of(event));
        when(eventRegistrationRepository.existsByEventIdAndUserIdAndStatus(7L, 11L, RegistrationStatus.REGISTERED))
                .thenReturn(true);

        assertThatThrownBy(() -> eventRegistrationService.registerForEvent(7L, authentication))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered");

        verify(eventRegistrationRepository, never()).save(any(EventRegistration.class));
    }

    @Test
    void registerForEventRejectsWhenCapacityReached() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("student@student.tus.com");

        User user = new User();
        user.setId(33L);
        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(eq("student@student.tus.com"), eq("student@student.tus.com")))
                .thenReturn(Optional.of(user));

        Club club = new Club();
        club.setActive(true);
        Event event = new Event();
        event.setId(12L);
        event.setActive(true);
        event.setClub(club);
        event.setCapacity(1);

        when(eventRepository.findById(12L)).thenReturn(Optional.of(event));
        when(eventRegistrationRepository.existsByEventIdAndUserIdAndStatus(12L, 33L, RegistrationStatus.REGISTERED))
                .thenReturn(false);
        when(eventRegistrationRepository.countByEventIdAndStatus(12L, RegistrationStatus.REGISTERED))
                .thenReturn(1L);

        assertThatThrownBy(() -> eventRegistrationService.registerForEvent(12L, authentication))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Capacity for this event is reached");

        verify(eventRegistrationRepository, never()).save(any(EventRegistration.class));
    }

    @Test
    void registerForEventRejectsInactiveEvent() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("student@student.tus.com");

        User user = new User();
        user.setId(44L);
        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(eq("student@student.tus.com"), eq("student@student.tus.com")))
                .thenReturn(Optional.of(user));

        Event event = new Event();
        event.setId(21L);
        event.setActive(false);
        when(eventRepository.findById(21L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventRegistrationService.registerForEvent(21L, authentication))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Event is inactive");
    }

    @Test
    void registerForEventThrowsWhenEventMissing() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("student@student.tus.com");

        User user = new User();
        user.setId(55L);
        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(eq("student@student.tus.com"), eq("student@student.tus.com")))
                .thenReturn(Optional.of(user));

        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventRegistrationService.registerForEvent(99L, authentication))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Event not found");
    }

    @Test
    void registerForEventThrowsWhenUserMissing() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("missing@student.tus.com");

        when(userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(eq("missing@student.tus.com"), eq("missing@student.tus.com")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventRegistrationService.registerForEvent(1L, authentication))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User not found");
    }

    private LocalDateTime stubClock() {
        LocalDateTime baseTime = LocalDateTime.of(2026, 3, 8, 12, 0);
        Instant instant = baseTime.atZone(ZONE).toInstant();
        when(clock.instant()).thenReturn(instant);
        when(clock.getZone()).thenReturn(ZONE);
        return baseTime;
    }
}
