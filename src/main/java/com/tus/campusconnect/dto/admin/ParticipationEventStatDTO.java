package com.tus.campusconnect.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ParticipationEventStatDTO {
    private Long eventId;
    private String eventTitle;
    private String clubName;
    private long registrationCount;
}
