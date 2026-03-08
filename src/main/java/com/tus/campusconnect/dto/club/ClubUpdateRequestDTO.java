package com.tus.campusconnect.dto.club;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClubUpdateRequestDTO {
    private String name;
    private String description;
    private String category;
}
