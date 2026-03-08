package com.tus.campusconnect.dto.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class EventResponseDTO {
    private Long id;
    private Long clubId;
    private String clubName;
    private String clubCategory;
    private String title;
    private String description;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer capacity;
    private boolean active;
}
