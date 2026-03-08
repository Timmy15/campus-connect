package com.tus.campusConnect.dto.club;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClubResponseDTO {
    private Long id;
    private String name;
    private String description;
    private String category;
    private boolean active;
}
