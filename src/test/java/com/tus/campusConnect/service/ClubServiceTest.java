package com.tus.campusConnect.service;

import com.tus.campusConnect.model.Club;
import com.tus.campusConnect.repository.ClubRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubServiceTest {

    @Mock
    private ClubRepository clubRepository;

    @InjectMocks
    private ClubService clubService;

    @Test
    void getActiveClubsUsesRepository() {
        Club club = new Club();
        club.setId(1L);
        club.setName("Chess Club");
        club.setActive(true);

        when(clubRepository.findAllByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(club));

        List<Club> result = clubService.getActiveClubs();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Chess Club");
        verify(clubRepository).findAllByIsActiveTrueOrderByNameAsc();
    }

    @Test
    void getAllClubsUsesRepository() {
        when(clubRepository.findAllByOrderByNameAsc()).thenReturn(List.of());

        List<Club> result = clubService.getAllClubs();

        assertThat(result).isEmpty();
        verify(clubRepository).findAllByOrderByNameAsc();
    }

    @Test
    void nameExistsDelegatesToRepository() {
        when(clubRepository.existsByNameIgnoreCase("Robotics")).thenReturn(true);

        boolean exists = clubService.nameExists("Robotics");

        assertThat(exists).isTrue();
        verify(clubRepository).existsByNameIgnoreCase("Robotics");
    }

    @Test
    void findByIdDelegatesToRepository() {
        Club club = new Club();
        club.setId(4L);
        when(clubRepository.findById(4L)).thenReturn(Optional.of(club));

        Optional<Club> result = clubService.findById(4L);

        assertThat(result).isPresent();
        verify(clubRepository).findById(4L);
    }

    @Test
    void saveDelegatesToRepository() {
        Club club = new Club();
        club.setName("Drama");
        when(clubRepository.save(club)).thenReturn(club);

        Club saved = clubService.save(club);

        assertThat(saved.getName()).isEqualTo("Drama");
        verify(clubRepository).save(club);
    }
}
