package com.snow.commit.repository;

import com.snow.commit.entity.Habit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitRepository extends JpaRepository<Habit, Long> {

    List<Habit> findByUserId(Long userId);

    List<Habit> findByActiveAndUserId(boolean active, Long userId);
}
