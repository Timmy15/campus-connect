package com.tus.campusconnect.repository;

import com.tus.campusconnect.dto.admin.ParticipationClubStatDTO;
import com.tus.campusconnect.dto.admin.ParticipationEventStatDTO;
import com.tus.campusconnect.model.EventRegistration;
import com.tus.campusconnect.model.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            select new com.tus.campusconnect.dto.admin.ParticipationEventStatDTO(
                e.id,
                e.title,
                c.name,
                count(r)
            )
            from EventRegistration r
            join r.event e
            join e.club c
            where r.status = :status
            group by e.id, e.title, c.name
            order by count(r) desc
            """)
    List<ParticipationEventStatDTO> findEventRegistrationStats(@Param("status") RegistrationStatus status);

    @Query("""
            select new com.tus.campusconnect.dto.admin.ParticipationClubStatDTO(
                c.id,
                c.name,
                count(r)
            )
            from EventRegistration r
            join r.event e
            join e.club c
            where r.status = :status
            group by c.id, c.name
            order by count(r) desc
            """)
    List<ParticipationClubStatDTO> findClubRegistrationStats(@Param("status") RegistrationStatus status);
}
