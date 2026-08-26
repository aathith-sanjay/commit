package com.snow.commit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.snow.commit.dto.CompletionRequest;
import com.snow.commit.dto.CreateHabitRequest;
import com.snow.commit.entity.ScheduleType;
import com.snow.commit.entity.TreeState;
import com.snow.commit.repository.HabitCompletionRepository;
import com.snow.commit.repository.HabitRepository;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class V2HabitServiceTest {

    @Autowired
    private HabitService habitService;

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private HabitCompletionRepository habitCompletionRepository;

    @BeforeEach
    void setUp() {
        habitCompletionRepository.deleteAll();
        habitRepository.deleteAll();
    }

    @Test
    void shouldCalculateCorrectStreakForSpecificDaysHabit() {
        LocalDate today = LocalDate.now();
        LocalDate thisWeekMonday = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate lastWeekMonday = thisWeekMonday.minusWeeks(1);

        var created = habitService.createHabit(new CreateHabitRequest(
            "Workout",
            null,
            "Health",
            lastWeekMonday,
            null,
            "Asia/Kolkata",
            ScheduleType.SPECIFIC_DAYS,
            "MON,WED,FRI"
        ));

        habitService.completeHabit(created.id(), new CompletionRequest(lastWeekMonday));
        habitService.completeHabit(created.id(), new CompletionRequest(lastWeekMonday.plusDays(2)));
        habitService.completeHabit(created.id(), new CompletionRequest(lastWeekMonday.plusDays(4)));
        habitService.completeHabit(created.id(), new CompletionRequest(thisWeekMonday));
        habitService.completeHabit(created.id(), new CompletionRequest(thisWeekMonday.plusDays(2)));

        var streak = habitService.getStreak(created.id());

        assertThat(streak.currentStreak()).isEqualTo(5);
        assertThat(streak.longestStreak()).isEqualTo(5);
    }

    @Test
    void shouldMarkTreeDeadWhenScheduledDayMissedForSpecificDaysHabit() {
        LocalDate today = LocalDate.now();
        LocalDate thisWeekMonday = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate lastWeekMonday = thisWeekMonday.minusWeeks(1);

        var created = habitService.createHabit(new CreateHabitRequest(
            "Meditate",
            null,
            null,
            lastWeekMonday,
            null,
            "Asia/Kolkata",
            ScheduleType.SPECIFIC_DAYS,
            "MON,WED,FRI"
        ));

        habitService.completeHabit(created.id(), new CompletionRequest(lastWeekMonday));

        var streak = habitService.getStreak(created.id());
        var tree = habitService.getTree(created.id());

        assertThat(streak.currentStreak()).isLessThanOrEqualTo(1);
        assertThat(tree.treeState()).isEqualTo(TreeState.DEAD);
    }

    @Test
    void shouldReturnAnalyticsWithCorrectCompletionRate() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(4);
        var created = habitService.createHabit(new CreateHabitRequest(
            "Read",
            "Read daily",
            "Learning",
            startDate,
            null,
            "Asia/Kolkata",
            ScheduleType.DAILY,
            null
        ));

        habitService.completeHabit(created.id(), new CompletionRequest(today.minusDays(2)));
        habitService.completeHabit(created.id(), new CompletionRequest(today.minusDays(1)));
        habitService.completeHabit(created.id(), new CompletionRequest(today));

        var analytics = habitService.getAnalytics(created.id());

        assertThat(analytics.completionRate()).isCloseTo(60.0, within(0.001));
        assertThat(analytics.totalCompletions()).isEqualTo(3);
    }

    @Test
    void shouldArchiveAndRestoreHabit() {
        var created = habitService.createHabit(new CreateHabitRequest("Walk", LocalDate.now().minusDays(3)));

        habitService.archiveHabit(created.id());

        assertThat(habitService.getHabits()).extracting("id").doesNotContain(created.id());
        assertThat(habitService.getHabits(true)).extracting("id").contains(created.id());

        habitService.restoreHabit(created.id());

        assertThat(habitService.getHabits()).extracting("id").contains(created.id());
    }

    @Test
    void shouldReturnWeeklyStatsInAnalytics() {
        LocalDate today = LocalDate.now();
        var created = habitService.createHabit(new CreateHabitRequest(
            "Stretch",
            null,
            null,
            today.minusWeeks(10),
            null,
            "Asia/Kolkata",
            ScheduleType.DAILY,
            null
        ));

        habitService.completeHabit(created.id(), new CompletionRequest(today.minusDays(1)));

        var analytics = habitService.getAnalytics(created.id());

        assertThat(analytics.weeklyStats()).hasSize(8);
    }
}
