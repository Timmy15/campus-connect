package com.tus.campusconnect.service;

import com.tus.campusconnect.dto.user.UserProfileDTO;
import com.tus.campusconnect.exception.UnauthorizedException;
import com.tus.campusconnect.model.User;
import com.tus.campusconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserProfileDTO getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("User not found.");
        }

        String identifier = authentication.getName();
        User user = userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier).orElse(null);
        if (user == null) {
            throw new UnauthorizedException("User not found.");
        }

        String role = user.getRole().name();
        return new UserProfileDTO(
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                role
        );
    }
}
