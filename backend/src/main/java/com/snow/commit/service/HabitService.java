package com.snow.commit.service;

import com.snow.commit.dto.AnalyticsResponse;
import com.snow.commit.dto.CompletionRequest;
import com.snow.commit.dto.CreateHabitRequest;
import com.snow.commit.dto.HabitCompletionResponse;
import com.snow.commit.dto.HabitResponse;
import com.snow.commit.dto.StreakResponse;
import com.snow.commit.dto.TreeResponse;
import com.snow.commit.dto.UpdateHabitRequest;
import com.snow.commit.entity.Habit;
import com.snow.commit.entity.HabitCompletion;
import com.snow.commit.entity.ScheduleType;
import com.snow.commit.entity.TreeStage;
import com.snow.commit.entity.TreeState;
import com.snow.commit.exception.DuplicateCompletionException;
import com.snow.commit.exception.ResourceNotFoundException;
import com.snow.commit.repository.HabitCompletionRepository;
import com.snow.commit.repository.HabitRepository;
import jakarta.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class HabitService {

    private static final String DEFAULT_TIMEZONE = "Asia/Kolkata";

    private final HabitRepository habitRepository;
    private final HabitCompletionRepository completionRepository;

    public HabitService(HabitRepository habitRepository, HabitCompletionRepository completionRepository) {
        this.habitRepository = habitRepository;
        this.completionRepository = completionRepository;
    }

    @Transactional
    public HabitResponse createHabit(CreateHabitRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Habit name is required");
        }

        Habit habit = new Habit();
        habit.setName(request.name().trim());
        habit.setDescription(normalizeText(request.description()));
        habit.setCategory(normalizeText(request.category()));
        habit.setActive(true);
        habit.setStartDate(request.startDate() == null ? LocalDate.now() : request.startDate());
        habit.setEndDate(request.endDate());
        habit.setTimezone(normalizeTimezone(request.timezone()));
        habit.setScheduleType(resolveScheduleType(request.scheduleType()));
        habit.setScheduleDays(normalizeScheduleDays(habit.getScheduleType(), request.scheduleDays()));
        validateDateRange(habit.getStartDate(), habit.getEndDate());
        habit.setCurrentStreak(0);
        habit.setLongestStreak(0);
        habit.setTreeState(TreeState.DEAD);
        habit.setUpdatedAt(LocalDateTime.now());

        Habit savedHabit = habitRepository.save(habit);
        refreshHabitMetrics(savedHabit);
        return toResponse(savedHabit);
    }

    public List<HabitResponse> getHabits() {
        return getHabits(false);
    }

    public List<HabitResponse> getHabits(boolean includeArchived) {
        List<Habit> habits = includeArchived ? habitRepository.findAll() : habitRepository.findByActive(true);
        return habits.stream().map(this::toResponse).toList();
    }

    public HabitResponse getHabit(Long id) {
        Habit habit = getHabitEntity(id);
        return toResponse(habit);
    }

    @Transactional
    public HabitResponse updateHabit(Long id, UpdateHabitRequest request) {
        Habit habit = getHabitEntity(id);

        if (request.name() != null && !request.name().isBlank()) {
            habit.setName(request.name().trim());
        }
        if (request.active() != null) {
            habit.setActive(request.active());
        }
        if (request.description() != null) {
            habit.setDescription(normalizeText(request.description()));
        }
        if (request.category() != null) {
            habit.setCategory(normalizeText(request.category()));
        }
        if (request.startDate() != null) {
            habit.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            habit.setEndDate(request.endDate());
        }
        if (request.timezone() != null) {
            habit.setTimezone(normalizeTimezone(request.timezone()));
        }

        ScheduleType effectiveScheduleType = request.scheduleType() != null ? request.scheduleType() : habit.getScheduleType();
        String effectiveScheduleDays = request.scheduleDays() != null ? request.scheduleDays() : habit.getScheduleDays();

        habit.setScheduleType(effectiveScheduleType);
        habit.setScheduleDays(normalizeScheduleDays(effectiveScheduleType, effectiveScheduleDays));
        validateDateRange(habit.getStartDate(), habit.getEndDate());
        habit.setUpdatedAt(LocalDateTime.now());

        Habit saved = habitRepository.save(habit);
        refreshHabitMetrics(saved);
        return toResponse(saved);
    }

    @Transactional
    public HabitResponse archiveHabit(Long id) {
        Habit habit = getHabitEntity(id);
        habit.setActive(false);
        habit.setUpdatedAt(LocalDateTime.now());
        Habit saved = habitRepository.save(habit);
        return toResponse(saved);
    }

    @Transactional
    public HabitResponse restoreHabit(Long id) {
        Habit habit = getHabitEntity(id);
        habit.setActive(true);
        habit.setUpdatedAt(LocalDateTime.now());
        Habit saved = habitRepository.save(habit);
        return toResponse(saved);
    }

    @Transactional
    public void deleteHabit(Long id) {
        Habit habit = getHabitEntity(id);
        habitRepository.delete(habit);
    }

    @Transactional
    public HabitCompletionResponse completeHabit(Long id, CompletionRequest request) {
        Habit habit = getHabitEntity(id);
        LocalDate completionDate = request.completionDate();

        if (completionRepository.existsByHabitIdAndCompletionDate(id, completionDate)) {
            throw new DuplicateCompletionException("Habit was already completed on " + completionDate);
        }

        HabitCompletion completion = new HabitCompletion();
        completion.setHabit(habit);
        completion.setCompletionDate(completionDate);
        HabitCompletion savedCompletion = completionRepository.save(completion);

        refreshHabitMetrics(habit);
        return new HabitCompletionResponse(savedCompletion.getId(), habit.getId(), savedCompletion.getCompletionDate(), savedCompletion.getCreatedAt());
    }

    public List<HabitCompletionResponse> getHistory(Long id) {
        getHabitEntity(id);
        return completionRepository.findByHabitIdOrderByCompletionDateDesc(id).stream()
            .map(c -> new HabitCompletionResponse(c.getId(), c.getHabit().getId(), c.getCompletionDate(), c.getCreatedAt()))
            .toList();
    }

    public StreakResponse getStreak(Long id) {
        Habit habit = getHabitEntity(id);
        HabitMetrics metrics = calculateMetrics(habit);
        return new StreakResponse(metrics.currentStreak(), metrics.longestStreak(), metrics.todayCompleted());
    }

    public TreeResponse getTree(Long id) {
        Habit habit = getHabitEntity(id);
        HabitMetrics metrics = calculateMetrics(habit);
        return new TreeResponse(metrics.treeState(), metrics.treeStage(), metrics.currentStreak(), metrics.longestStreak());
    }

    public AnalyticsResponse getAnalytics(Long id) {
        Habit habit = getHabitEntity(id);
        HabitMetrics metrics = calculateMetrics(habit);
        LocalDate today = LocalDate.now();
        LocalDate trackingEnd = getTrackingEndDate(habit, today);
        List<HabitCompletion> completions = getRelevantCompletions(habit, trackingEnd);
        List<LocalDate> scheduledUnits = getScheduledUnitKeys(habit, trackingEnd);
        Set<LocalDate> completedUnits = getCompletedUnitKeys(habit, completions, trackingEnd);

        double completionRate = calculatePercentage(completedUnits.size(), scheduledUnits.size());
        double consistencyScore = calculateConsistencyScore(scheduledUnits, completedUnits);

        return new AnalyticsResponse(
            completionRepository.countByHabitId(id),
            completionRate,
            metrics.currentStreak(),
            metrics.longestStreak(),
            consistencyScore,
            buildWeeklyStats(trackingEnd, scheduledUnits, completedUnits),
            buildMonthlyStats(trackingEnd, scheduledUnits, completedUnits)
        );
    }

    private Habit getHabitEntity(Long id) {
        return habitRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id " + id));
    }

    private HabitResponse toResponse(Habit habit) {
        HabitMetrics metrics = calculateMetrics(habit);
        return new HabitResponse(
            habit.getId(),
            habit.getName(),
            habit.getScheduleType(),
            habit.isActive(),
            habit.getStartDate(),
            metrics.currentStreak(),
            metrics.longestStreak(),
            metrics.treeState(),
            metrics.treeStage(),
            metrics.todayCompleted(),
            habit.getCreatedAt(),
            habit.getUpdatedAt(),
            habit.getDescription(),
            habit.getCategory(),
            habit.getEndDate(),
            habit.getTimezone(),
            habit.getScheduleDays()
        );
    }

    private void refreshHabitMetrics(Habit habit) {
        HabitMetrics metrics = calculateMetrics(habit);
        habit.setCurrentStreak(metrics.currentStreak());
        habit.setLongestStreak(metrics.longestStreak());
        habit.setTreeState(metrics.treeState());
        habit.setUpdatedAt(LocalDateTime.now());
        habitRepository.save(habit);
    }

    private HabitMetrics calculateMetrics(Habit habit) {
        LocalDate today = LocalDate.now();
        LocalDate trackingEnd = getTrackingEndDate(habit, today);
        List<HabitCompletion> completions = getRelevantCompletions(habit, trackingEnd);
        List<LocalDate> scheduledUnits = getScheduledUnitKeys(habit, trackingEnd);
        Set<LocalDate> completedUnits = getCompletedUnitKeys(habit, completions, trackingEnd);
        List<LocalDate> completionDates = completions.stream().map(HabitCompletion::getCompletionDate).distinct().sorted(Comparator.reverseOrder()).toList();

        int currentStreak = calculateCurrentStreak(scheduledUnits, completedUnits);
        int longestStreak = calculateLongestStreak(scheduledUnits, completedUnits);
        boolean todayCompleted = completionDates.contains(today);
        TreeState treeState = determineTreeState(habit, scheduledUnits, completedUnits, today, trackingEnd);
        TreeStage treeStage = determineTreeStage(currentStreak);

        return new HabitMetrics(currentStreak, longestStreak, todayCompleted, treeState, treeStage);
    }

    private List<HabitCompletion> getRelevantCompletions(Habit habit, LocalDate trackingEnd) {
        if (trackingEnd.isBefore(habit.getStartDate())) {
            return List.of();
        }
        return completionRepository.findByHabitIdAndCompletionDateBetweenOrderByCompletionDateAsc(
            habit.getId(),
            habit.getStartDate(),
            trackingEnd
        );
    }

    private LocalDate getTrackingEndDate(Habit habit, LocalDate today) {
        if (habit.getEndDate() != null && habit.getEndDate().isBefore(today)) {
            return habit.getEndDate();
        }
        return today;
    }

    private List<LocalDate> getScheduledUnitKeys(Habit habit, LocalDate trackingEnd) {
        if (trackingEnd.isBefore(habit.getStartDate())) {
            return List.of();
        }

        return switch (habit.getScheduleType()) {
            case DAILY -> getDailyUnits(habit.getStartDate(), trackingEnd);
            case SPECIFIC_DAYS -> getSpecificDayUnits(habit.getStartDate(), trackingEnd, parseScheduleDays(habit.getScheduleDays()));
            case WEEKLY -> getWeeklyUnits(habit.getStartDate(), trackingEnd);
        };
    }

    private Set<LocalDate> getCompletedUnitKeys(Habit habit, List<HabitCompletion> completions, LocalDate trackingEnd) {
        if (completions.isEmpty()) {
            return Set.of();
        }

        return switch (habit.getScheduleType()) {
            case DAILY -> completions.stream()
                .map(HabitCompletion::getCompletionDate)
                .filter(date -> !date.isAfter(trackingEnd))
                .collect(Collectors.toCollection(LinkedHashSet::new));
            case SPECIFIC_DAYS -> {
                Set<DayOfWeek> days = parseScheduleDays(habit.getScheduleDays());
                yield completions.stream()
                    .map(HabitCompletion::getCompletionDate)
                    .filter(date -> !date.isAfter(trackingEnd))
                    .filter(date -> days.contains(date.getDayOfWeek()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            }
            case WEEKLY -> completions.stream()
                .map(HabitCompletion::getCompletionDate)
                .filter(date -> !date.isAfter(trackingEnd))
                .map(this::startOfWeek)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        };
    }

    private List<LocalDate> getDailyUnits(LocalDate start, LocalDate end) {
        List<LocalDate> units = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            units.add(date);
        }
        return units;
    }

    private List<LocalDate> getSpecificDayUnits(LocalDate start, LocalDate end, Set<DayOfWeek> scheduledDays) {
        List<LocalDate> units = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            if (scheduledDays.contains(date.getDayOfWeek())) {
                units.add(date);
            }
        }
        return units;
    }

    private List<LocalDate> getWeeklyUnits(LocalDate start, LocalDate end) {
        List<LocalDate> units = new ArrayList<>();
        for (LocalDate date = startOfWeek(start); !date.isAfter(end); date = date.plusWeeks(1)) {
            units.add(date);
        }
        return units;
    }

    private int calculateCurrentStreak(List<LocalDate> scheduledUnits, Set<LocalDate> completedUnits) {
        for (int i = scheduledUnits.size() - 1; i >= 0; i--) {
            if (!completedUnits.contains(scheduledUnits.get(i))) {
                continue;
            }

            int streak = 0;
            for (int j = i; j >= 0 && completedUnits.contains(scheduledUnits.get(j)); j--) {
                streak++;
            }
            return streak;
        }
        return 0;
    }

    private int calculateLongestStreak(List<LocalDate> scheduledUnits, Set<LocalDate> completedUnits) {
        int longest = 0;
        int current = 0;

        for (LocalDate scheduledUnit : scheduledUnits) {
            if (completedUnits.contains(scheduledUnit)) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 0;
            }
        }

        return longest;
    }

    private TreeState determineTreeState(
        Habit habit,
        List<LocalDate> scheduledUnits,
        Set<LocalDate> completedUnits,
        LocalDate today,
        LocalDate trackingEnd
    ) {
        if (scheduledUnits.isEmpty()) {
            return TreeState.DEAD;
        }

        LocalDate lastDueUnit = findLastDueUnit(habit, scheduledUnits, today, trackingEnd);
        LocalDate lastCompletedUnit = scheduledUnits.stream()
            .filter(completedUnits::contains)
            .max(LocalDate::compareTo)
            .orElse(null);

        LocalDate referenceUnit = maxDate(lastDueUnit, lastCompletedUnit);
        if (referenceUnit == null) {
            return TreeState.DEAD;
        }

        return completedUnits.contains(referenceUnit) ? TreeState.ALIVE : TreeState.DEAD;
    }

    private LocalDate findLastDueUnit(Habit habit, List<LocalDate> scheduledUnits, LocalDate today, LocalDate trackingEnd) {
        if (scheduledUnits.isEmpty()) {
            return null;
        }

        return switch (habit.getScheduleType()) {
            case DAILY, SPECIFIC_DAYS -> {
                LocalDate dueCutoff = trackingEnd.isBefore(today) ? trackingEnd : today.minusDays(1);
                yield scheduledUnits.stream().filter(date -> !date.isAfter(dueCutoff)).max(LocalDate::compareTo).orElse(null);
            }
            case WEEKLY -> {
                LocalDate dueContext = trackingEnd.isBefore(today) ? trackingEnd : today.minusDays(1);
                LocalDate weekStart = startOfWeek(dueContext);
                if (dueContext.getDayOfWeek() != DayOfWeek.SUNDAY) {
                    weekStart = weekStart.minusWeeks(1);
                }
                LocalDate finalWeekStart = weekStart;
                yield scheduledUnits.stream().filter(date -> !date.isAfter(finalWeekStart)).max(LocalDate::compareTo).orElse(null);
            }
        };
    }

    private TreeStage determineTreeStage(int streak) {
        if (streak >= 90) {
            return TreeStage.MATURE_TREE;
        }
        if (streak >= 60) {
            return TreeStage.FRUIT_TREE;
        }
        if (streak >= 30) {
            return TreeStage.FLOWERING_TREE;
        }
        if (streak >= 21) {
            return TreeStage.TREE;
        }
        if (streak >= 14) {
            return TreeStage.YOUNG_TREE;
        }
        if (streak >= 7) {
            return TreeStage.SAPLING;
        }
        if (streak >= 3) {
            return TreeStage.SHRUB;
        }
        if (streak >= 1) {
            return TreeStage.HERB;
        }
        return TreeStage.SEED;
    }

    private double calculateConsistencyScore(List<LocalDate> scheduledUnits, Set<LocalDate> completedUnits) {
        if (scheduledUnits.isEmpty()) {
            return 0.0;
        }

        int fromIndex = Math.max(0, scheduledUnits.size() - 30);
        List<LocalDate> recentUnits = scheduledUnits.subList(fromIndex, scheduledUnits.size());
        long completedRecent = recentUnits.stream().filter(completedUnits::contains).count();
        return calculatePercentage(completedRecent, recentUnits.size());
    }

    private double calculatePercentage(long numerator, int denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return Math.min(100.0, (numerator * 100.0) / denominator);
    }

    private List<AnalyticsResponse.WeekStat> buildWeeklyStats(LocalDate trackingEnd, List<LocalDate> scheduledUnits, Set<LocalDate> completedUnits) {
        LocalDate currentWeekStart = startOfWeek(trackingEnd);
        List<AnalyticsResponse.WeekStat> stats = new ArrayList<>();

        for (int i = 7; i >= 0; i--) {
            LocalDate weekStart = currentWeekStart.minusWeeks(i);
            LocalDate weekEnd = weekStart.plusDays(6);
            stats.add(new AnalyticsResponse.WeekStat(
                weekLabel(weekStart),
                countUnitsInRange(scheduledUnits, weekStart, weekEnd),
                countUnitsInRange(completedUnits, weekStart, weekEnd)
            ));
        }

        return stats;
    }

    private List<AnalyticsResponse.MonthStat> buildMonthlyStats(
        LocalDate trackingEnd,
        List<LocalDate> scheduledUnits,
        Set<LocalDate> completedUnits
    ) {
        LocalDate currentMonthStart = trackingEnd.withDayOfMonth(1);
        List<AnalyticsResponse.MonthStat> stats = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            LocalDate monthStart = currentMonthStart.minusMonths(i);
            LocalDate monthEnd = monthStart.with(TemporalAdjusters.lastDayOfMonth());
            stats.add(new AnalyticsResponse.MonthStat(
                String.format("%d-%02d", monthStart.getYear(), monthStart.getMonthValue()),
                countUnitsInRange(scheduledUnits, monthStart, monthEnd),
                countUnitsInRange(completedUnits, monthStart, monthEnd)
            ));
        }

        return stats;
    }

    private int countUnitsInRange(List<LocalDate> units, LocalDate from, LocalDate to) {
        return (int) units.stream().filter(date -> !date.isBefore(from) && !date.isAfter(to)).count();
    }

    private int countUnitsInRange(Set<LocalDate> units, LocalDate from, LocalDate to) {
        return (int) units.stream().filter(date -> !date.isBefore(from) && !date.isAfter(to)).count();
    }

    private String weekLabel(LocalDate weekStart) {
        int weekBasedYear = weekStart.get(IsoFields.WEEK_BASED_YEAR);
        int week = weekStart.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return String.format("%d-W%02d", weekBasedYear, week);
    }

    private LocalDate startOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private ScheduleType resolveScheduleType(ScheduleType scheduleType) {
        return scheduleType == null ? ScheduleType.DAILY : scheduleType;
    }

    private String normalizeTimezone(String timezone) {
        String normalized = normalizeText(timezone);
        return normalized == null ? DEFAULT_TIMEZONE : normalized;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
    }

    private String normalizeScheduleDays(ScheduleType scheduleType, String scheduleDays) {
        if (scheduleType != ScheduleType.SPECIFIC_DAYS) {
            return null;
        }
        return formatScheduleDays(parseScheduleDays(scheduleDays));
    }

    private Set<DayOfWeek> parseScheduleDays(String scheduleDays) {
        if (scheduleDays == null || scheduleDays.isBlank()) {
            throw new IllegalArgumentException("Schedule days are required for SPECIFIC_DAYS habits");
        }

        Set<DayOfWeek> days = Arrays.stream(scheduleDays.split(","))
            .map(String::trim)
            .filter(token -> !token.isEmpty())
            .map(this::parseDayOfWeek)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (days.isEmpty()) {
            throw new IllegalArgumentException("Schedule days are required for SPECIFIC_DAYS habits");
        }

        return days;
    }

    private String formatScheduleDays(Set<DayOfWeek> scheduleDays) {
        return Arrays.stream(DayOfWeek.values())
            .filter(scheduleDays::contains)
            .map(this::abbreviateDay)
            .collect(Collectors.joining(","));
    }

    private DayOfWeek parseDayOfWeek(String token) {
        return switch (token.toUpperCase(Locale.ROOT)) {
            case "MON", "MONDAY" -> DayOfWeek.MONDAY;
            case "TUE", "TUESDAY" -> DayOfWeek.TUESDAY;
            case "WED", "WEDNESDAY" -> DayOfWeek.WEDNESDAY;
            case "THU", "THURSDAY" -> DayOfWeek.THURSDAY;
            case "FRI", "FRIDAY" -> DayOfWeek.FRIDAY;
            case "SAT", "SATURDAY" -> DayOfWeek.SATURDAY;
            case "SUN", "SUNDAY" -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("Invalid schedule day: " + token);
        };
    }

    private String abbreviateDay(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "MON";
            case TUESDAY -> "TUE";
            case WEDNESDAY -> "WED";
            case THURSDAY -> "THU";
            case FRIDAY -> "FRI";
            case SATURDAY -> "SAT";
            case SUNDAY -> "SUN";
        };
    }

    private LocalDate maxDate(LocalDate first, LocalDate second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isAfter(second) ? first : second;
    }

    private record HabitMetrics(int currentStreak, int longestStreak, boolean todayCompleted, TreeState treeState, TreeStage treeStage) {
    }
}
