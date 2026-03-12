package com.tus.campusconnect.dto.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class EventRegistrationDetailDTO {
    private Long registrationId;
    private Long userId;
    private String fullName;
    private String email;
    private String username;
    private String status;
    private LocalDateTime registeredAt;
}
