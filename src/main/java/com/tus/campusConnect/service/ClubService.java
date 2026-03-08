package com.tus.campusConnect.service;

import com.tus.campusConnect.model.Club;
import com.tus.campusConnect.repository.ClubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository clubRepository;

    public List<Club> getActiveClubs() {
        return clubRepository.findAllByIsActiveTrueOrderByNameAsc();
    }

    public List<Club> getAllClubs() {
        return clubRepository.findAllByOrderByNameAsc();
    }

    public Optional<Club> findById(Long id) {
        return clubRepository.findById(id);
    }

    public boolean nameExists(String name) {
        return clubRepository.existsByNameIgnoreCase(name);
    }

    public Club save(Club club) {
        return clubRepository.save(club);
    }
}
