package com.snow.commit.exception;

public class HabitNotFoundException extends ResourceNotFoundException {

    public HabitNotFoundException(Long id) {
        super("Habit not found with id " + id);
    }
}
