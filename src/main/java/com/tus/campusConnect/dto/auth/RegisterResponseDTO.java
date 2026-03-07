package com.tus.campusConnect.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterResponseDTO {
    private String message;
    private String role;
}
