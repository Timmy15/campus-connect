package com.tus.campusconnect.controller;

import com.tus.campusconnect.dto.event.EventRegistrationResponseDTO;
import com.tus.campusconnect.exception.ConflictException;
import com.tus.campusconnect.model.Club;
import com.tus.campusconnect.model.Event;
import com.tus.campusconnect.model.RegistrationStatus;
import com.tus.campusconnect.repository.EventRegistrationRepository;
import com.tus.campusconnect.service.EventRegistrationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventRegistrationControllerTest {

    @Mock
    private EventRegistrationService eventRegistrationService;

    @Mock
    private EventRegistrationRepository eventRegistrationRepository;

    @InjectMocks
    private EventRegistrationController eventRegistrationController;

    @Test
    void registerForEventReturnsCreated() {
        Event event = event(12L, "Launch");
        when(eventRegistrationService.registerForEvent(eq(12L), any(Authentication.class)))
                .thenReturn(event);
        when(eventRegistrationRepository.countByEventIdAndStatus(12L, RegistrationStatus.REGISTERED)).thenReturn(1L);

        ResponseEntity<EventRegistrationResponseDTO> response =
                eventRegistrationController.registerForEvent(12L, mockAuth());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Registration successful.");
        assertThat(response.getBody().getEvent().getId()).isEqualTo(12L);
    }

    @Test
    void registerForEventRejectsWhenAlreadyRegistered() {
        when(eventRegistrationService.registerForEvent(eq(5L), any(Authentication.class)))
                .thenThrow(new ConflictException("You're already registered for this event page"));

        Authentication auth = mockAuth();

        assertThatThrownBy(() -> eventRegistrationController.registerForEvent(5L, auth))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered");
    }

    private Authentication mockAuth() {
        return org.mockito.Mockito.mock(Authentication.class);
    }

    private Event event(Long id, String title) {
        Club club = new Club();
        club.setId(3L);
        club.setName("Community Club");
        club.setCategory("Community");
        club.setActive(true);

        Event event = new Event();
        event.setId(id);
        event.setTitle(title);
        event.setLocation("Main Hall");
        event.setCapacity(10);
        event.setStartTime(LocalDateTime.now().plusDays(1));
        event.setClub(club);
        event.setActive(true);
        return event;
    }
}
