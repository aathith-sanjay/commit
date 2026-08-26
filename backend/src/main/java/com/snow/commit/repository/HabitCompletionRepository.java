package com.snow.commit.repository;

import com.snow.commit.entity.HabitCompletion;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitCompletionRepository extends JpaRepository<HabitCompletion, Long> {

    boolean existsByHabitIdAndCompletionDate(Long habitId, LocalDate completionDate);

    List<HabitCompletion> findByHabitIdOrderByCompletionDateDesc(Long habitId);

    Optional<HabitCompletion> findTopByHabitIdOrderByCompletionDateDesc(Long habitId);
}
