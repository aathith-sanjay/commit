package com.snow.commit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.snow.commit.dto.CompletionRequest;
import com.snow.commit.dto.CreateHabitRequest;
import com.snow.commit.entity.TreeState;
import com.snow.commit.exception.DuplicateCompletionException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class HabitServiceTest {

    @Autowired
    private HabitService habitService;

    @Test
    void shouldCreateHabitAndTrackStreak() {
        CreateHabitRequest request = new CreateHabitRequest("Read", LocalDate.now());

        var created = habitService.createHabit(request);
        habitService.completeHabit(created.id(), new CompletionRequest(LocalDate.now()));

        var streak = habitService.getStreak(created.id());
        var tree = habitService.getTree(created.id());

        assertThat(streak.currentStreak()).isEqualTo(1);
        assertThat(streak.longestStreak()).isEqualTo(1);
        assertThat(tree.treeState()).isEqualTo(TreeState.ALIVE);
        assertThat(tree.currentStreak()).isEqualTo(1);
    }

    @Test
    void shouldRejectDuplicateCompletionForSameDate() {
        var created = habitService.createHabit(new CreateHabitRequest("Walk", LocalDate.now()));
        habitService.completeHabit(created.id(), new CompletionRequest(LocalDate.now()));

        assertThatThrownBy(() -> habitService.completeHabit(created.id(), new CompletionRequest(LocalDate.now())))
            .isInstanceOf(DuplicateCompletionException.class);
    }

    @Test
    void shouldMarkTreeDeadWhenHabitIsMissed() {
        var created = habitService.createHabit(new CreateHabitRequest("Journal", LocalDate.now().minusDays(10)));
        habitService.completeHabit(created.id(), new CompletionRequest(LocalDate.now().minusDays(2)));

        var tree = habitService.getTree(created.id());

        assertThat(tree.treeState()).isEqualTo(TreeState.DEAD);
    }
}
