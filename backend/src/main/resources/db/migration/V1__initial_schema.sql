CREATE TABLE habit (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    schedule_type VARCHAR(50) NOT NULL DEFAULT 'DAILY',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    start_date DATE NOT NULL,
    current_streak INTEGER NOT NULL DEFAULT 0,
    longest_streak INTEGER NOT NULL DEFAULT 0,
    tree_state VARCHAR(30) NOT NULL DEFAULT 'DEAD',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE habit_completion (
    id BIGSERIAL PRIMARY KEY,
    habit_id BIGINT NOT NULL,
    completion_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_habit_completion_habit FOREIGN KEY (habit_id) REFERENCES habit(id) ON DELETE CASCADE,
    CONSTRAINT uk_habit_completion UNIQUE (habit_id, completion_date)
);

CREATE INDEX idx_habit_completion_habit_date ON habit_completion (habit_id, completion_date);
