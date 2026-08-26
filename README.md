# commit.

A monorepo for the commit. habit tracker project.

## Structure

- `backend/` — Spring Boot REST API for V1 habit tracking
- `frontend/` — React frontend for the personal web app
- `.github/workflows/` — CI/CD workflows

## Backend quick start

```bash
cd backend
mvn test
```

If you prefer a local PostgreSQL setup, configure these environment variables before running the app:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/commit
export DB_USERNAME=commit
export DB_PASSWORD=commit
```

Then:

```bash
mvn spring-boot:run
```

## V1 focus

The first backend version is a single-user habit tracker that supports:

- creating and updating habits
- recording completions
- tracking streaks and history
- calculating tree growth/reset state
- persisting historical completion data
