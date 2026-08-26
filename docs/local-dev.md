# Local Development Guide

This document describes how to run **commit.** locally without conflicting with the AppSense project that lives at `~/development/repository/appsense`.

---

## Port map — AppSense vs commit.

| Service | AppSense | commit. |
|---|---|---|
| Frontend dev server | **9094** (normal) / **9093** (lib mode) | **5173** (Vite default) |
| Inflo dev server | **9093** | — |
| Backend / Tomcat | **8443** (HTTPS) | **8080** (HTTP) |
| PostgreSQL | not used locally (remote DB) | **5434** ← mapped on host |
| ClickHouse TCP | 9000 / 9001 / 9002 | — |
| ClickHouse keeper | 9281 / 9234 | — |
| Proxy (squid) | 3128 (localhost) | — |
| AppSense agent WS | 63397 | — |
| Playwright test server | 63315 | — |

> **commit. PostgreSQL uses host port 5434** (not the default 5432) to avoid any future clash if a local Postgres instance is started for AppSense work.

---

## Setup

### 1. Clone and enter the repo

```bash
git clone <repo-url>
cd commit
```

### 2. Copy environment files

```bash
cp backend/.env.example backend/.env
cp frontend/.env.example frontend/.env
```

No changes to `backend/.env` are needed for local Docker-based development.
`frontend/.env` points to `http://localhost:8080` by default which is correct.

### 3. Start the database

Docker must be running. The database uses **host port 5434** (not 5432) to avoid conflicts.

```bash
docker compose up -d
```

Verify it is healthy:

```bash
docker compose ps
```

### 4. Run the backend

Requires **Java 17**. If your default `java` is not Java 17, set `JAVA_HOME` first:

```bash
export JAVA_HOME=/usr/lib/jvm/jdk-17.0.12-oracle-x64
```

From the `backend/` directory:

```bash
cd backend
../mvnw spring-boot:run
```

The API will be available at `http://localhost:8080/api/v1/habits`.

### 5. Run the frontend

Requires **Node 20+** (installed via nvm in this environment):

```bash
source ~/.nvm/nvm.sh   # if node is not on PATH
cd frontend
npm install            # first time only
npm run dev
```

The UI will open at `http://localhost:5173`.

The Vite dev server proxies all `/api` requests to `http://localhost:8080`, so CORS is not an issue during local development.

---

## docker-compose port note

The `docker-compose.yml` at the repo root maps PostgreSQL as follows:

```yaml
ports:
  - "5434:5432"
```

The container listens on 5432 internally; it is exposed on **5434 on the host**.
The `backend/.env.example` already uses `DB_URL=jdbc:postgresql://localhost:5434/commit` to match.

---

## Running backend tests (no database required)

Tests use an in-memory H2 database and do not need PostgreSQL or Docker:

```bash
cd backend
JAVA_HOME=/usr/lib/jvm/jdk-17.0.12-oracle-x64 ../mvnw test
```

---

## Summary of all commit. local ports

| Service | Host port |
|---|---|
| Vite dev server | 5173 |
| Spring Boot API | 8080 |
| PostgreSQL (Docker) | **5434** |

None of these collide with AppSense's local services (9094, 9093, 8443, 9000–9002, 9281, 3128, 63397, 63315).


---

## V3 Authentication

V3 adds JWT-based authentication. New environment variables required:

| Variable | Description | Required in production |
|---|---|---|
| `JWT_SECRET` | HMAC-SHA256 signing key (min 32 chars) | **Yes** |
| `JWT_EXPIRATION_DAYS` | Token validity in days (default: 30) | No |

### Generate a secure JWT secret
```bash
openssl rand -base64 48
```

### Local dev default
The backend will use a built-in development secret if `JWT_SECRET` is not set.
**Do not use the default in production.**

### Migration user
The V3 Flyway migration creates a migration user `admin@commit.local` with password `changeme`
and assigns all existing habits to it. After deploying V3, register a proper account and
re-create your habits, or update the migration user's email/password via the database.

### Auth endpoints
| Method | URL | Description |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Register new account |
| `POST` | `/api/v1/auth/login` | Login, receive JWT |
| `GET` | `/api/v1/auth/me` | Get current user info |
