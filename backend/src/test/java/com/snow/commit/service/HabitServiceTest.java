package com.snow.commit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.snow.commit.dto.CompletionRequest;
import com.snow.commit.dto.CreateHabitRequest;
import com.snow.commit.entity.AppUser;
import com.snow.commit.entity.TreeState;
import com.snow.commit.exception.DuplicateCompletionException;
import com.snow.commit.exception.HabitNotFoundException;
import com.snow.commit.repository.AppUserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class HabitServiceTest {

    @Autowired
    private HabitService habitService;

    @Autowired
    private AppUserRepository appUserRepository;

    private Long testUserId;

    @BeforeEach
    void setUpUser() {
        AppUser user = new AppUser();
        user.setEmail("test-" + System.nanoTime() + "@test.com");
        user.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        user.setDisplayName("Test User");
        user.setCreatedAt(LocalDateTime.now());
        testUserId = appUserRepository.save(user).getId();
    }

    @Test
    void shouldCreateHabitAndTrackStreak() {
        CreateHabitRequest request = new CreateHabitRequest("Read", LocalDate.now());

        var created = habitService.createHabit(request, testUserId);
        habitService.completeHabit(created.id(), new CompletionRequest(LocalDate.now()), testUserId);

        var streak = habitService.getStreak(created.id(), testUserId);
        var tree = habitService.getTree(created.id(), testUserId);

        assertThat(streak.currentStreak()).isEqualTo(1);
        assertThat(streak.longestStreak()).isEqualTo(1);
        assertThat(tree.treeState()).isEqualTo(TreeState.ALIVE);
        assertThat(tree.currentStreak()).isEqualTo(1);
    }

    @Test
    void shouldRejectDuplicateCompletionForSameDate() {
        var created = habitService.createHabit(new CreateHabitRequest("Walk", LocalDate.now()), testUserId);
        habitService.completeHabit(created.id(), new CompletionRequest(LocalDate.now()), testUserId);

        assertThatThrownBy(() -> habitService.completeHabit(created.id(), new CompletionRequest(LocalDate.now()), testUserId))
            .isInstanceOf(DuplicateCompletionException.class);
    }

    @Test
    void shouldHideHabitsOwnedByOtherUsers() {
        var created = habitService.createHabit(new CreateHabitRequest("Private", LocalDate.now()), testUserId);

        AppUser otherUser = new AppUser();
        otherUser.setEmail("other-" + System.nanoTime() + "@test.com");
        otherUser.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        otherUser.setDisplayName("Other User");
        otherUser.setCreatedAt(LocalDateTime.now());
        Long otherUserId = appUserRepository.save(otherUser).getId();

        assertThatThrownBy(() -> habitService.getHabit(created.id(), otherUserId))
            .isInstanceOf(HabitNotFoundException.class);
    }

    @Test
    void shouldMarkTreeDeadWhenHabitIsMissed() {
        var created = habitService.createHabit(new CreateHabitRequest("Journal", LocalDate.now().minusDays(10)), testUserId);
        habitService.completeHabit(created.id(), new CompletionRequest(LocalDate.now().minusDays(2)), testUserId);

        var tree = habitService.getTree(created.id(), testUserId);

        assertThat(tree.treeState()).isEqualTo(TreeState.DEAD);
    }
}
