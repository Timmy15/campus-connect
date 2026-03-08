package com.tus.campusconnect.dto.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EventActionResponseDTO {
    private String message;
    private EventResponseDTO event;
}
