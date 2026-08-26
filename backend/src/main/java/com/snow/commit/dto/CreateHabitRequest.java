package com.snow.commit.dto;

import com.snow.commit.entity.ScheduleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateHabitRequest(
    @NotBlank(message = "Habit name is required")
    @Size(max = 150, message = "Habit name cannot exceed 150 characters")
    String name,

    String description,

    @Size(max = 80, message = "Category cannot exceed 80 characters")
    String category,

    @NotNull(message = "Start date is required")
    LocalDate startDate,

    LocalDate endDate,

    @Size(max = 60, message = "Timezone cannot exceed 60 characters")
    String timezone,

    ScheduleType scheduleType,

    @Size(max = 30, message = "Schedule days cannot exceed 30 characters")
    String scheduleDays
) {
    public CreateHabitRequest(String name, LocalDate startDate) {
        this(name, null, null, startDate, null, null, null, null);
    }
}
