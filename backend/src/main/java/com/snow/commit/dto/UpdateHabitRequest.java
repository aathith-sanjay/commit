package com.snow.commit.dto;

import jakarta.validation.constraints.Size;

public record UpdateHabitRequest(
    @Size(max = 150, message = "Habit name cannot exceed 150 characters")
    String name,
    Boolean active
) {
}
