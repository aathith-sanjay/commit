package com.snow.commit.dto;

import com.snow.commit.entity.TreeState;
import com.snow.commit.entity.TreeStage;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record HabitResponse(
    Long id,
    String name,
    String scheduleType,
    boolean active,
    LocalDate startDate,
    int currentStreak,
    int longestStreak,
    TreeState treeState,
    TreeStage treeStage,
    boolean todayCompleted,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
