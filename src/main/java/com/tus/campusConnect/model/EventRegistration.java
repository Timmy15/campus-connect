package com.tus.campusConnect.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "event_registrations",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"event_id", "user_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class EventRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus status = RegistrationStatus.REGISTERED;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // Many registrations belong to one user
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Many registrations belong to one event
    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
}