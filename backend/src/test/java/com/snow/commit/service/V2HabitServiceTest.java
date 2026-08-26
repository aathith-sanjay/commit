package com.snow.commit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.snow.commit.dto.CompletionRequest;
import com.snow.commit.dto.CreateHabitRequest;
import com.snow.commit.entity.AppUser;
import com.snow.commit.entity.ScheduleType;
import com.snow.commit.entity.TreeState;
import com.snow.commit.repository.AppUserRepository;
import com.snow.commit.repository.HabitCompletionRepository;
import com.snow.commit.repository.HabitRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Autowired
    private AppUserRepository appUserRepository;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        habitCompletionRepository.deleteAll();
        habitRepository.deleteAll();

        AppUser user = new AppUser();
        user.setEmail("test-" + System.nanoTime() + "@test.com");
        user.setPasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        user.setDisplayName("Test User");
        user.setCreatedAt(LocalDateTime.now());
        testUserId = appUserRepository.save(user).getId();
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
        ), testUserId);

        habitService.completeHabit(created.id(), new CompletionRequest(lastWeekMonday), testUserId);
        habitService.completeHabit(created.id(), new CompletionRequest(lastWeekMonday.plusDays(2)), testUserId);
        habitService.completeHabit(created.id(), new CompletionRequest(lastWeekMonday.plusDays(4)), testUserId);
        habitService.completeHabit(created.id(), new CompletionRequest(thisWeekMonday), testUserId);
        habitService.completeHabit(created.id(), new CompletionRequest(thisWeekMonday.plusDays(2)), testUserId);

        var streak = habitService.getStreak(created.id(), testUserId);

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
        ), testUserId);

        habitService.completeHabit(created.id(), new CompletionRequest(lastWeekMonday), testUserId);

        var streak = habitService.getStreak(created.id(), testUserId);
        var tree = habitService.getTree(created.id(), testUserId);

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
        ), testUserId);

        habitService.completeHabit(created.id(), new CompletionRequest(today.minusDays(2)), testUserId);
        habitService.completeHabit(created.id(), new CompletionRequest(today.minusDays(1)), testUserId);
        habitService.completeHabit(created.id(), new CompletionRequest(today), testUserId);

        var analytics = habitService.getAnalytics(created.id(), testUserId);

        assertThat(analytics.completionRate()).isCloseTo(60.0, within(0.001));
        assertThat(analytics.totalCompletions()).isEqualTo(3);
    }

    @Test
    void shouldArchiveAndRestoreHabit() {
        var created = habitService.createHabit(new CreateHabitRequest("Walk", LocalDate.now().minusDays(3)), testUserId);

        habitService.archiveHabit(created.id(), testUserId);

        assertThat(habitService.getHabits(false, testUserId)).extracting("id").doesNotContain(created.id());
        assertThat(habitService.getHabits(true, testUserId)).extracting("id").contains(created.id());

        habitService.restoreHabit(created.id(), testUserId);

        assertThat(habitService.getHabits(false, testUserId)).extracting("id").contains(created.id());
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
        ), testUserId);

        habitService.completeHabit(created.id(), new CompletionRequest(today.minusDays(1)), testUserId);

        var analytics = habitService.getAnalytics(created.id(), testUserId);

        assertThat(analytics.weeklyStats()).hasSize(8);
    }
}
