package com.tus.campusConnect.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UsernameAvailabilityDTO {
    private boolean available;
    private String message;
}
