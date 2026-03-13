package com.tus.campusconnect.service;

import com.tus.campusconnect.exception.BadRequestException;
import com.tus.campusconnect.exception.ConflictException;
import com.tus.campusconnect.exception.NotFoundException;
import com.tus.campusconnect.exception.UnauthorizedException;
import com.tus.campusconnect.model.Club;
import com.tus.campusconnect.model.Event;
import com.tus.campusconnect.model.EventRegistration;
import com.tus.campusconnect.model.RegistrationStatus;
import com.tus.campusconnect.model.User;
import com.tus.campusconnect.repository.ClubRepository;
import com.tus.campusconnect.repository.EventRegistrationRepository;
import com.tus.campusconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final Clock clock;

    public List<Club> getActiveClubs() {
        return clubRepository.findAllByIsActiveTrueOrderByNameAsc();
    }

    public List<Club> getAllClubs() {
        return clubRepository.findAllByOrderByNameAsc();
    }

    public Club createClub(Authentication authentication, String name, String description, String category) {
        User admin = resolveAdmin(authentication);
        if (admin == null) {
            throw new UnauthorizedException("User not found.");
        }

        String normalizedName = normalize(name);
        if (normalizedName.isEmpty()) {
            throw new BadRequestException("Club name is required.");
        }

        if (clubRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ConflictException("Club already exists");
        }

        Club club = new Club();
        club.setName(normalizedName);
        club.setDescription(toNullable(description));
        club.setCategory(toNullable(category));
        club.setActive(true);
        club.setCreatedAt(LocalDateTime.now(clock));
        club.setAdmin(admin);

        return clubRepository.save(club);
    }

    public Club updateClub(Long id, String name, String description, String category) {
        Club club = requireClub(id);

        String normalizedName = normalize(name);
        if (normalizedName.isEmpty()) {
            throw new BadRequestException("Invalid club details.");
        }

        if (!normalizedName.equalsIgnoreCase(club.getName()) && clubRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ConflictException("Club already exists");
        }

        club.setName(normalizedName);
        club.setDescription(toNullable(description));
        club.setCategory(toNullable(category));

        return clubRepository.save(club);
    }

    @Transactional
    public Club deactivateClub(Long id) {
        Club club = requireClub(id);

        club.setActive(false);
        cancelUpcomingRegistrations(club.getId());
        return clubRepository.save(club);
    }

    public Club activateClub(Long id) {
        Club club = requireClub(id);

        club.setActive(true);
        return clubRepository.save(club);
    }

    @Transactional
    public Club deleteClub(Long id) {
        Club club = requireClub(id);
        clubRepository.delete(club);
        return club;
    }

    private User resolveAdmin(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        String identifier = authentication.getName();
        return userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier).orElse(null);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String toNullable(String value) {
        String trimmed = normalize(value);
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void cancelUpcomingRegistrations(Long clubId) {
        List<EventRegistration> registrations = eventRegistrationRepository
                .findAllByEventClubIdAndStatus(clubId, RegistrationStatus.REGISTERED);
        if (registrations.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
        List<EventRegistration> toCancel = registrations.stream()
                .filter(registration -> isUpcomingOrOngoing(registration.getEvent(), now))
                .map(registration -> {
                    registration.setStatus(RegistrationStatus.CANCELLED);
                    registration.setCancelledAt(now);
                    return registration;
                })
                .toList();

        if (!toCancel.isEmpty()) {
            eventRegistrationRepository.saveAll(toCancel);
        }
    }

    private boolean isUpcomingOrOngoing(Event event, LocalDateTime now) {
        if (event == null) {
            return false;
        }
        LocalDateTime endTime = event.getEndTime();
        if (endTime != null) {
            return !endTime.isBefore(now);
        }
        LocalDateTime startTime = event.getStartTime();
        if (startTime != null) {
            return !startTime.isBefore(now);
        }
        return false;
    }

    private Club requireClub(Long id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Club not found."));
    }
}
