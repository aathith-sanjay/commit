package com.snow.commit.dto;

public record StreakResponse(int currentStreak, int longestStreak, boolean todayCompleted) {
}
