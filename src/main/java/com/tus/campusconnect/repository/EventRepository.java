package com.tus.campusconnect.repository;

import com.tus.campusconnect.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findAllByIsActiveTrueOrderByStartTimeAsc();
    List<Event> findAllByClubIdOrderByStartTimeAsc(Long clubId);
}
