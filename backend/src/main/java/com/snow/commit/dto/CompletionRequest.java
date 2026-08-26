package com.snow.commit.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CompletionRequest(@NotNull(message = "Completion date is required") LocalDate completionDate) {
}
