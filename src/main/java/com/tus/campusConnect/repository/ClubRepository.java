package com.tus.campusConnect.repository;

import com.tus.campusConnect.model.Club;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClubRepository extends JpaRepository<Club, Long> {
    boolean existsByNameIgnoreCase(String name);
    List<Club> findAllByIsActiveTrueOrderByNameAsc();
    List<Club> findAllByOrderByNameAsc();
}
