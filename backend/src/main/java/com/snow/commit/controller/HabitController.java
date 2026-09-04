package com.snow.commit.controller;

import com.snow.commit.dto.AnalyticsResponse;
import com.snow.commit.dto.CompletionRequest;
import com.snow.commit.dto.CreateHabitRequest;
import com.snow.commit.dto.HabitCompletionResponse;
import com.snow.commit.dto.HabitResponse;
import com.snow.commit.dto.StreakResponse;
import com.snow.commit.dto.TreeResponse;
import com.snow.commit.dto.UpdateHabitRequest;
import com.snow.commit.security.UserPrincipal;
import com.snow.commit.service.HabitService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HabitController {

    private final HabitService habitService;

    public HabitController(HabitService habitService) {
        this.habitService = habitService;
    }

    @GetMapping("/habits")
    public List<HabitResponse> getHabits(
        @RequestParam(defaultValue = "false") boolean includeArchived,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return habitService.getHabits(includeArchived, currentUser.getUserId());
    }

    @PostMapping("/habits")
    public ResponseEntity<HabitResponse> createHabit(
        @Valid @RequestBody CreateHabitRequest request,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        HabitResponse habit = habitService.createHabit(request, currentUser.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(habit);
    }

    @GetMapping("/habits/{id}")
    public HabitResponse getHabit(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal currentUser) {
        return habitService.getHabit(id, currentUser.getUserId());
    }

    @PutMapping("/habits/{id}")
    public HabitResponse updateHabit(
        @PathVariable Long id,
        @Valid @RequestBody UpdateHabitRequest request,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return habitService.updateHabit(id, request, currentUser.getUserId());
    }

    @PatchMapping("/habits/{id}/archive")
    public HabitResponse archiveHabit(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal currentUser) {
        return habitService.archiveHabit(id, currentUser.getUserId());
    }

    @PatchMapping("/habits/{id}/restore")
    public HabitResponse restoreHabit(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal currentUser) {
        return habitService.restoreHabit(id, currentUser.getUserId());
    }

    @DeleteMapping("/habits/{id}")
    public ResponseEntity<Void> deleteHabit(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal currentUser) {
        habitService.deleteHabit(id, currentUser.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/habits/{id}/completions")
    public HabitCompletionResponse completeHabit(
        @PathVariable Long id,
        @Valid @RequestBody CompletionRequest request,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return habitService.completeHabit(id, request, currentUser.getUserId());
    }

    @GetMapping("/habits/{id}/history")
    public List<HabitCompletionResponse> getHistory(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal currentUser) {
        return habitService.getHistory(id, currentUser.getUserId());
    }

    @GetMapping("/habits/{id}/streak")
    public StreakResponse getStreak(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal currentUser) {
        return habitService.getStreak(id, currentUser.getUserId());
    }

    @GetMapping("/habits/{id}/tree")
    public TreeResponse getTree(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal currentUser) {
        return habitService.getTree(id, currentUser.getUserId());
    }

    @GetMapping("/habits/{id}/analytics")
    public AnalyticsResponse getAnalytics(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal currentUser) {
        return habitService.getAnalytics(id, currentUser.getUserId());
    }
}
