package com.snow.commit.service;

import com.snow.commit.dto.CompletionRequest;
import com.snow.commit.dto.CreateHabitRequest;
import com.snow.commit.dto.HabitCompletionResponse;
import com.snow.commit.dto.HabitResponse;
import com.snow.commit.dto.StreakResponse;
import com.snow.commit.dto.TreeResponse;
import com.snow.commit.dto.UpdateHabitRequest;
import com.snow.commit.entity.Habit;
import com.snow.commit.entity.HabitCompletion;
import com.snow.commit.entity.TreeStage;
import com.snow.commit.entity.TreeState;
import com.snow.commit.exception.DuplicateCompletionException;
import com.snow.commit.exception.ResourceNotFoundException;
import com.snow.commit.repository.HabitCompletionRepository;
import com.snow.commit.repository.HabitRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class HabitService {

    private static final TreeStage[] STAGE_ORDER = {
        TreeStage.SEED,
        TreeStage.HERB,
        TreeStage.SHRUB,
        TreeStage.SAPLING,
        TreeStage.YOUNG_TREE,
        TreeStage.TREE,
        TreeStage.FLOWERING_TREE,
        TreeStage.FRUIT_TREE,
        TreeStage.MATURE_TREE
    };

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
        habit.setScheduleType("DAILY");
        habit.setActive(true);
        habit.setStartDate(request.startDate() == null ? LocalDate.now() : request.startDate());
        habit.setCurrentStreak(0);
        habit.setLongestStreak(0);
        habit.setTreeState(TreeState.DEAD);
        habit.setUpdatedAt(LocalDateTime.now());

        Habit savedHabit = habitRepository.save(habit);
        return toResponse(savedHabit);
    }

    public List<HabitResponse> getHabits() {
        return habitRepository.findAll().stream().map(this::toResponse).toList();
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
            habit.getUpdatedAt()
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
        List<HabitCompletion> completions = completionRepository.findByHabitIdOrderByCompletionDateDesc(habit.getId());
        List<LocalDate> completionDates = completions.stream().map(HabitCompletion::getCompletionDate).sorted(Comparator.reverseOrder()).toList();

        int currentStreak = calculateCurrentStreak(completionDates);
        int longestStreak = calculateLongestStreak(completionDates);
        boolean todayCompleted = completionDates.contains(LocalDate.now());

        // Tree is ALIVE only if the most recent completion is today or yesterday.
        // Any gap of 2+ days means a scheduled day was missed and the tree is DEAD.
        LocalDate mostRecent = completionDates.isEmpty() ? null : completionDates.get(0);
        TreeState treeState;
        if (mostRecent == null) {
            treeState = TreeState.DEAD;
        } else if (!mostRecent.isBefore(LocalDate.now().minusDays(1))) {
            treeState = TreeState.ALIVE;
        } else {
            treeState = TreeState.DEAD;
        }

        TreeStage treeStage = determineTreeStage(currentStreak);
        return new HabitMetrics(currentStreak, longestStreak, todayCompleted, treeState, treeStage);
    }

    private int calculateCurrentStreak(List<LocalDate> completionDates) {
        if (completionDates.isEmpty()) {
            return 0;
        }

        LocalDate cursor = completionDates.get(0);
        int streak = 0;
        Set<LocalDate> completionSet = completionDates.stream().collect(Collectors.toSet());

        while (completionSet.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private int calculateLongestStreak(List<LocalDate> completionDates) {
        if (completionDates.isEmpty()) {
            return 0;
        }

        List<LocalDate> sorted = completionDates.stream().sorted().toList();
        int longest = 0;
        int current = 0;
        LocalDate previous = null;

        for (LocalDate date : sorted) {
            if (previous == null || date.equals(previous.plusDays(1))) {
                current++;
            } else {
                current = 1;
            }
            longest = Math.max(longest, current);
            previous = date;
        }

        return longest;
    }

    private TreeStage determineTreeStage(int streak) {
        if (streak <= 0) {
            return TreeStage.SEED;
        }
        int index = Math.min(streak, STAGE_ORDER.length - 1);
        return STAGE_ORDER[index];
    }

    private record HabitMetrics(int currentStreak, int longestStreak, boolean todayCompleted, TreeState treeState, TreeStage treeStage) {
    }
}
