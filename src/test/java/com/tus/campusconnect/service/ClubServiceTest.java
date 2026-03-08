package com.tus.campusconnect.service;

import com.tus.campusconnect.model.Club;
import com.tus.campusconnect.repository.ClubRepository;
import com.tus.campusconnect.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubServiceTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private UserRepository userRepository;

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
    }

    @Test
    void getAllClubsUsesRepository() {
        when(clubRepository.findAllByOrderByNameAsc()).thenReturn(List.of());

        List<Club> result = clubService.getAllClubs();

        assertThat(result).isEmpty();
    }
}
