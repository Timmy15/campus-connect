package com.tus.campusconnect.repository;

import com.tus.campusconnect.model.EventRegistration;
import com.tus.campusconnect.model.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {
    boolean existsByEventIdAndUserIdAndStatus(Long eventId, Long userId, RegistrationStatus status);
    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);
}
