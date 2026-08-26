package com.snow.commit.dto;

import com.snow.commit.entity.ScheduleType;
import com.snow.commit.entity.TreeStage;
import com.snow.commit.entity.TreeState;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record HabitResponse(
    Long id,
    String name,
    ScheduleType scheduleType,
    boolean active,
    LocalDate startDate,
    int currentStreak,
    int longestStreak,
    TreeState treeState,
    TreeStage treeStage,
    boolean todayCompleted,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String description,
    String category,
    LocalDate endDate,
    String timezone,
    String scheduleDays
) {
}
