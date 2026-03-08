package com.tus.campusConnect.dto.club;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClubActionResponseDTO {
    private String message;
    private ClubResponseDTO club;
}
