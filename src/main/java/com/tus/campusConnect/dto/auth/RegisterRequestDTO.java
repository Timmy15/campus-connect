package com.tus.campusConnect.dto.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequestDTO {
    private String email;
    private String username;
    private String password;
}
