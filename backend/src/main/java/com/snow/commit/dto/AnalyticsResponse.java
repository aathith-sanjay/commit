package com.snow.commit.dto;

import java.util.List;

public record AnalyticsResponse(
    long totalCompletions,
    double completionRate,
    int currentStreak,
    int longestStreak,
    double consistencyScore,
    List<WeekStat> weeklyStats,
    List<MonthStat> monthlyStats
) {
    public record WeekStat(String weekLabel, int scheduled, int completed) {
    }

    public record MonthStat(String monthLabel, int scheduled, int completed) {
    }
}
