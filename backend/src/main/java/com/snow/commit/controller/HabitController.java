package com.snow.commit.controller;

import com.snow.commit.dto.CompletionRequest;
import com.snow.commit.dto.CreateHabitRequest;
import com.snow.commit.dto.HabitCompletionResponse;
import com.snow.commit.dto.HabitResponse;
import com.snow.commit.dto.StreakResponse;
import com.snow.commit.dto.TreeResponse;
import com.snow.commit.dto.UpdateHabitRequest;
import com.snow.commit.service.HabitService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HabitController {

    private final HabitService habitService;

    public HabitController(HabitService habitService) {
        this.habitService = habitService;
    }

    @GetMapping("/habits")
    public List<HabitResponse> getHabits() {
        return habitService.getHabits();
    }

    @PostMapping("/habits")
    public ResponseEntity<HabitResponse> createHabit(@Valid @RequestBody CreateHabitRequest request) {
        HabitResponse habit = habitService.createHabit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(habit);
    }

    @GetMapping("/habits/{id}")
    public HabitResponse getHabit(@PathVariable Long id) {
        return habitService.getHabit(id);
    }

    @PutMapping("/habits/{id}")
    public HabitResponse updateHabit(@PathVariable Long id, @Valid @RequestBody UpdateHabitRequest request) {
        return habitService.updateHabit(id, request);
    }

    @DeleteMapping("/habits/{id}")
    public ResponseEntity<Void> deleteHabit(@PathVariable Long id) {
        habitService.deleteHabit(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/habits/{id}/completions")
    public HabitCompletionResponse completeHabit(@PathVariable Long id, @Valid @RequestBody CompletionRequest request) {
        return habitService.completeHabit(id, request);
    }

    @GetMapping("/habits/{id}/history")
    public List<HabitCompletionResponse> getHistory(@PathVariable Long id) {
        return habitService.getHistory(id);
    }

    @GetMapping("/habits/{id}/streak")
    public StreakResponse getStreak(@PathVariable Long id) {
        return habitService.getStreak(id);
    }

    @GetMapping("/habits/{id}/tree")
    public TreeResponse getTree(@PathVariable Long id) {
        return habitService.getTree(id);
    }
}
