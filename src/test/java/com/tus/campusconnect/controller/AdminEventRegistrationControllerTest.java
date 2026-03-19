package com.tus.campusconnect.controller;

import com.tus.campusconnect.dto.common.MessageResponseDTO;
import com.tus.campusconnect.dto.event.EventRegistrationDetailDTO;
import com.tus.campusconnect.model.EventRegistration;
import com.tus.campusconnect.model.RegistrationStatus;
import com.tus.campusconnect.model.User;
import com.tus.campusconnect.service.EventRegistrationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminEventRegistrationControllerTest {

    @Mock
    private EventRegistrationService eventRegistrationService;

    @InjectMocks
    private AdminEventRegistrationController adminEventRegistrationController;

    @Test
    void getRegistrationsForEventReturnsDetails() {
        User user = new User();
        user.setId(8L);
        user.setFullName("Alex Student");
        user.setEmail("alex@student.tus.com");
        user.setUsername("alexs");

        EventRegistration registration = new EventRegistration();
        registration.setId(4L);
        registration.setUser(user);
        registration.setStatus(RegistrationStatus.REGISTERED);
        registration.setRegisteredAt(LocalDateTime.of(2026, 3, 9, 10, 0));

        when(eventRegistrationService.getRegistrationsForEvent(3L)).thenReturn(List.of(registration));

        List<EventRegistrationDetailDTO> result = adminEventRegistrationController.getRegistrationsForEvent(3L);

        assertThat(result).hasSize(1);
        EventRegistrationDetailDTO dto = result.get(0);
        assertThat(dto.getRegistrationId()).isEqualTo(4L);
        assertThat(dto.getUserId()).isEqualTo(8L);
        assertThat(dto.getFullName()).isEqualTo("Alex Student");
        assertThat(dto.getEmail()).isEqualTo("alex@student.tus.com");
        assertThat(dto.getUsername()).isEqualTo("alexs");
        assertThat(dto.getStatus()).isEqualTo("REGISTERED");
    }

    @Test
    void unregisterStudentReturnsMessage() {
        ResponseEntity<MessageResponseDTO> response = adminEventRegistrationController.unregisterStudent(5L, 12L);

        verify(eventRegistrationService).unregisterRegistration(5L, 12L);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Registration cancelled.");
    }
}
