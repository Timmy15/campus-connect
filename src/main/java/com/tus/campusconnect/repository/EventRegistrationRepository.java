package com.tus.campusconnect.repository;

import com.tus.campusconnect.model.EventRegistration;
import com.tus.campusconnect.model.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {
    boolean existsByEventIdAndUserIdAndStatus(Long eventId, Long userId, RegistrationStatus status);
    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);
    List<EventRegistration> findAllByUserIdAndStatusOrderByRegisteredAtDesc(Long userId, RegistrationStatus status);
}
