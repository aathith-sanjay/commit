package com.snow.commit.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HabitCompletionResponse(Long id, Long habitId, LocalDate completionDate, LocalDateTime createdAt) {
}
