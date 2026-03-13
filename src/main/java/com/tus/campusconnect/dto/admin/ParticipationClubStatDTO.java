package com.tus.campusconnect.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ParticipationClubStatDTO {
    private Long clubId;
    private String clubName;
    private long registrationCount;
}
