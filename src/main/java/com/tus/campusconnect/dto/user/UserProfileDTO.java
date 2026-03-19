package com.tus.campusconnect.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileDTO {
    private String username;
    private String fullName;
    private String email;
    private String role;
}
