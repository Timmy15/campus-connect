package com.tus.campusconnect.repository;

import com.tus.campusconnect.model.EventRegistration;
import com.tus.campusconnect.model.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {
    boolean existsByEventIdAndUserIdAndStatus(Long eventId, Long userId, RegistrationStatus status);
    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);
    List<EventRegistration> findAllByUserIdAndStatusOrderByRegisteredAtDesc(Long userId, RegistrationStatus status);
    List<EventRegistration> findAllByEventIdAndStatusOrderByRegisteredAtDesc(Long eventId, RegistrationStatus status);
    List<EventRegistration> findAllByEventClubIdAndStatus(Long clubId, RegistrationStatus status);
    void deleteByEventId(Long eventId);
    Optional<EventRegistration> findByEventIdAndUserId(Long eventId, Long userId);
    Optional<EventRegistration> findByIdAndUserId(Long id, Long userId);
    Optional<EventRegistration> findByIdAndEventId(Long id, Long eventId);
}
