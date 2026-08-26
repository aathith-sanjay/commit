package com.snow.commit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateHabitRequest(
    @NotBlank(message = "Habit name is required")
    @Size(max = 150, message = "Habit name cannot exceed 150 characters")
    String name,

    @NotNull(message = "Start date is required")
    LocalDate startDate
) {
}
