# commit.

commit. is a personal habit tracker where each habit grows into a tree, rewards consistency, and withers when a streak is broken. The project is split into a Java/Spring Boot backend and a React + TypeScript frontend in a single monorepo.

## What this app does

- Track daily and scheduled habits
- Log completions and view historical activity
- Calculate streaks and tree growth state
- Support user accounts with JWT authentication
- Keep habit data isolated per user
- Run locally with PostgreSQL via Docker

## Tech stack

- Backend: Java 17, Spring Boot, Spring Security, JPA/Hibernate
- Database: PostgreSQL, with Flyway migrations
- Frontend: React 19, TypeScript, Vite
- Auth: JWT
- Deployment: Render for backend, GitHub Pages/Vite for frontend

## Repository layout

```text
commit/
├── backend/            Spring Boot REST API
├── frontend/           React frontend
├── docs/               Design and local development docs
├── .github/workflows/  CI/CD
├── docker-compose.yml  Local PostgreSQL setup
├── Dockerfile          Container build
├── render.yaml         Render deployment blueprint
├── LICENSE
├── README.md
└── ...
```

## Prerequisites

- Java 17+
- Node.js 20+
- Docker Desktop or Docker Engine
- Git

## Local development

### 1) Start PostgreSQL

```bash
docker compose up -d
```

The app is configured to use PostgreSQL on host port `5434` to avoid conflicts with other local services.

### 2) Configure environment files

Copy the example environment files:

```bash
cp backend/.env.example backend/.env
cp frontend/.env.example frontend/.env
```

The default values are already set for local development:

```env
# backend/.env
DB_URL=jdbc:postgresql://localhost:5434/commit
DB_USERNAME=commit
DB_PASSWORD=commit
JWT_SECRET=commit-default-dev-secret-key-please-change-in-production-1234567890
JWT_EXPIRATION_DAYS=30
```

```env
# frontend/.env
VITE_API_BASE_URL=http://localhost:8080
```

### 3) Run the backend

```bash
cd backend
mvn test
mvn spring-boot:run
```

The API runs on:

- `http://localhost:8080`

### 4) Run the frontend

```bash
cd frontend
npm install
npm run dev
```

The app is available at:

- `http://localhost:5173`

## Authentication

The backend includes JWT-based auth for registration and login. Common endpoints include:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`

Protected habit endpoints require a valid bearer token.

## Project docs

For more details on the architecture, database design, and local workflow, see:

- `docs/design.md`
- `docs/local-dev.md`
- `frontend/README.md`

## Contributing

1. Create a feature branch.
2. Make your changes in the relevant `backend/` or `frontend/` area.
3. Run the relevant tests locally.
4. Open a pull request with a clear summary of the change.

## License

This project is licensed under the MIT License. See `LICENSE` for details.
