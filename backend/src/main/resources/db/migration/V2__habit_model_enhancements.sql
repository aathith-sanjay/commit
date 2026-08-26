ALTER TABLE habit ADD COLUMN description TEXT;
ALTER TABLE habit ADD COLUMN category VARCHAR(80);
ALTER TABLE habit ADD COLUMN end_date DATE;
ALTER TABLE habit ADD COLUMN timezone VARCHAR(60) NOT NULL DEFAULT 'Asia/Kolkata';
ALTER TABLE habit ADD COLUMN schedule_days VARCHAR(30);

CREATE INDEX idx_habit_active ON habit (active);
CREATE INDEX idx_habit_category ON habit (category);
