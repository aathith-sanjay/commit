package com.snow.commit.dto;

import com.snow.commit.entity.ScheduleType;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateHabitRequest(
    @Size(max = 150, message = "Habit name cannot exceed 150 characters")
    String name,
    Boolean active,
    String description,
    @Size(max = 80, message = "Category cannot exceed 80 characters")
    String category,
    LocalDate startDate,
    LocalDate endDate,
    @Size(max = 60, message = "Timezone cannot exceed 60 characters")
    String timezone,
    ScheduleType scheduleType,
    @Size(max = 30, message = "Schedule days cannot exceed 30 characters")
    String scheduleDays
) {
    public UpdateHabitRequest(String name, Boolean active) {
        this(name, active, null, null, null, null, null, null, null);
    }
}
