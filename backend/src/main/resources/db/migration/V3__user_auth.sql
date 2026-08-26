CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO app_user (email, password_hash, display_name, created_at)
VALUES ('admin@commit.local', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Admin', now());

ALTER TABLE habit ADD COLUMN user_id BIGINT;
UPDATE habit SET user_id = (SELECT id FROM app_user WHERE email = 'admin@commit.local');
ALTER TABLE habit ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE habit ADD CONSTRAINT fk_habit_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE;
CREATE INDEX idx_habit_user_id ON habit(user_id);
