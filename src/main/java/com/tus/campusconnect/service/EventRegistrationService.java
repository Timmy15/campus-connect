package com.tus.campusconnect.service;

import com.tus.campusconnect.exception.BadRequestException;
import com.tus.campusconnect.exception.ConflictException;
import com.tus.campusconnect.exception.NotFoundException;
import com.tus.campusconnect.exception.UnauthorizedException;
import com.tus.campusconnect.model.Event;
import com.tus.campusconnect.model.EventRegistration;
import com.tus.campusconnect.model.RegistrationStatus;
import com.tus.campusconnect.model.User;
import com.tus.campusconnect.repository.EventRegistrationRepository;
import com.tus.campusconnect.repository.EventRepository;
import com.tus.campusconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EventRegistrationService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    public Event registerForEvent(Long eventId, Authentication authentication) {
        User user = requireUser(authentication);
        Event event = requireEvent(eventId);

        if (!event.isActive() || event.getClub() == null || !event.getClub().isActive()) {
            throw new BadRequestException("Event is inactive.");
        }

        boolean alreadyRegistered = eventRegistrationRepository.existsByEventIdAndUserIdAndStatus(
                eventId,
                user.getId(),
                RegistrationStatus.REGISTERED
        );
        if (alreadyRegistered) {
            throw new ConflictException("You're already registered for this event page");
        }

        long registeredCount = eventRegistrationRepository.countByEventIdAndStatus(
                eventId,
                RegistrationStatus.REGISTERED
        );
        Integer capacity = event.getCapacity();
        if (capacity != null && registeredCount >= capacity) {
            throw new ConflictException("Capacity for this event is reached");
        }

        EventRegistration registration = new EventRegistration();
        registration.setEvent(event);
        registration.setUser(user);
        registration.setStatus(RegistrationStatus.REGISTERED);
        registration.setRegisteredAt(LocalDateTime.now(clock));

        eventRegistrationRepository.save(registration);
        return event;
    }

    private User requireUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("User not found.");
        }

        String identifier = authentication.getName();
        User user = userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier).orElse(null);
        if (user == null) {
            throw new UnauthorizedException("User not found.");
        }
        return user;
    }

    private Event requireEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found."));
    }
}
