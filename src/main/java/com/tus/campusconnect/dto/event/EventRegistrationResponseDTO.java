package com.tus.campusconnect.dto.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EventRegistrationResponseDTO {
    private String message;
    private EventResponseDTO event;
}
