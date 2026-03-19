package com.tus.campusconnect.dto.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class EventRegistrationItemDTO {
    private Long registrationId;
    private Long eventId;
    private String eventTitle;
    private String clubName;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private LocalDateTime registeredAt;
}
