package com.tus.campusconnect.service;

import com.tus.campusconnect.exception.BadRequestException;
import com.tus.campusconnect.exception.NotFoundException;
import com.tus.campusconnect.model.Club;
import com.tus.campusconnect.model.Event;
import com.tus.campusconnect.repository.ClubRepository;
import com.tus.campusconnect.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final ClubRepository clubRepository;

    public List<Event> getActiveEvents() {
        return eventRepository.findAllByIsActiveTrueOrderByStartTimeAsc()
                .stream()
                .filter(event -> event.getClub() != null && event.getClub().isActive())
                .toList();
    }

    public List<Event> getEventsForClub(Long clubId) {
        requireClub(clubId);
        return eventRepository.findAllByClubIdOrderByStartTimeAsc(clubId);
    }

    public Event createEvent(Long clubId,
                             String title,
                             String description,
                             String location,
                             LocalDateTime startTime,
                             LocalDateTime endTime,
                             Integer capacity) {
        Club club = requireClub(clubId);
        if (!club.isActive()) {
            throw new BadRequestException("Club is inactive.");
        }

        validateEventDetails(title, location, capacity, startTime, endTime);

        Event event = new Event();
        event.setTitle(normalize(title));
        event.setDescription(toNullable(description));
        event.setLocation(normalize(location));
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setCapacity(capacity);
        event.setActive(true);
        event.setCreatedAt(LocalDateTime.now());
        event.setClub(club);

        return eventRepository.save(event);
    }

    public Event updateEvent(Long eventId,
                             String title,
                             String description,
                             String location,
                             LocalDateTime startTime,
                             LocalDateTime endTime,
                             Integer capacity) {
        Event event = requireEvent(eventId);

        validateEventDetails(title, location, capacity, startTime, endTime);

        event.setTitle(normalize(title));
        event.setDescription(toNullable(description));
        event.setLocation(normalize(location));
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setCapacity(capacity);

        return eventRepository.save(event);
    }

    private void validateEventDetails(String title,
                                      String location,
                                      Integer capacity,
                                      LocalDateTime startTime,
                                      LocalDateTime endTime) {
        String normalizedTitle = normalize(title);
        if (normalizedTitle.isEmpty()) {
            throw new BadRequestException("Event title is required.");
        }

        String normalizedLocation = normalize(location);
        if (normalizedLocation.isEmpty()) {
            throw new BadRequestException("Event location is required.");
        }

        if (capacity == null || capacity <= 0) {
            throw new BadRequestException("Capacity must be greater than 0.");
        }

        if (startTime == null) {
            throw new BadRequestException("Start time is required.");
        }

        if (startTime.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Start time must be in the future.");
        }

        if (endTime != null && endTime.isBefore(startTime)) {
            throw new BadRequestException("End time must be after the start time.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String toNullable(String value) {
        String trimmed = normalize(value);
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Club requireClub(Long id) {
        return clubRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Club not found."));
    }

    private Event requireEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found."));
    }
}
