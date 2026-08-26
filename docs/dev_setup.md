# commit. — Developer Setup Guide

> This guide walks a new developer through setting up **commit.** for local development from scratch. It covers every prerequisite, explains what each tool is for, and gives exact commands for each step.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Prerequisites](#2-prerequisites)
   - 2.1 [Git](#21-git)
   - 2.2 [Java 17](#22-java-17)
   - 2.3 [Maven](#23-maven)
   - 2.4 [Node.js via nvm](#24-nodejs-via-nvm)
   - 2.5 [Docker & Docker Compose](#25-docker--docker-compose)
   - 2.6 [Recommended IDE](#26-recommended-ide)
3. [Clone the Repository](#3-clone-the-repository)
4. [Project Structure at a Glance](#4-project-structure-at-a-glance)
5. [Backend Setup](#5-backend-setup)
   - 5.1 [Configure Environment Variables](#51-configure-environment-variables)
   - 5.2 [Start the Database](#52-start-the-database)
   - 5.3 [Run the Backend](#53-run-the-backend)
   - 5.4 [Verify Backend is Running](#54-verify-backend-is-running)
6. [Frontend Setup](#6-frontend-setup)
   - 6.1 [Install Dependencies](#61-install-dependencies)
   - 6.2 [Configure Environment Variables](#62-configure-environment-variables)
   - 6.3 [Run the Frontend](#63-run-the-frontend)
   - 6.4 [Verify Frontend is Running](#64-verify-frontend-is-running)
7. [Running Tests](#7-running-tests)
8. [Port Reference](#8-port-reference)
9. [Common Issues & Fixes](#9-common-issues--fixes)
10. [Stopping and Resetting](#10-stopping-and-resetting)
11. [Making Code Changes](#11-making-code-changes)

---

## 1. Project Overview

**commit.** is a full-stack habit tracker with:

- A **Spring Boot REST API** (Java 17) as the backend
- A **React + TypeScript** single-page application as the frontend
- **PostgreSQL** as the database (run locally via Docker)
- **JWT** for authentication

When running locally, the architecture looks like this:

```
Browser (localhost:5173)
        │
        │ HTTP proxy /api → localhost:8080
        ▼
  Spring Boot API (localhost:8080)
        │
        │ JDBC
        ▼
  PostgreSQL (localhost:5434)
```

The frontend dev server (Vite, port 5173) proxies all `/api` requests to the backend (port 8080). You never need to configure CORS in local development.

---

## 2. Prerequisites

Install each tool below before starting. Each section tells you what the tool is for and how to verify the installation.

---

### 2.1 Git

**What it is:** Version control system. Used to clone the repository and commit changes.

**Install:**

```bash
# Ubuntu/Debian
sudo apt update && sudo apt install git -y

# macOS (if not already installed via Xcode Command Line Tools)
xcode-select --install
# or via Homebrew
brew install git
```

**Verify:**
```bash
git --version
# Expected: git version 2.x.x
```

---

### 2.2 Java 17

**What it is:** The Java Development Kit. The Spring Boot backend requires **exactly Java 17** (not 11, not 21). Some Linux distros default to Java 21 — you will need Java 17 installed alongside.

**Why Java 17 specifically?** The `pom.xml` specifies `<java.version>17</java.version>`. Spring Boot 4.1.x runs on Java 17+, but the project was built and tested with 17.

**Install on Ubuntu/Debian:**
```bash
# Add the Adoptium repository for Temurin (Eclipse) builds
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo tee /etc/apt/trusted.gpg.d/adoptium.asc > /dev/null
echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt update
sudo apt install temurin-17-jdk -y
```

**Install on macOS:**
```bash
brew tap homebrew/cask-versions
brew install --cask temurin17
```

**Set JAVA_HOME to Java 17:**

If your system has multiple Java versions, you must point `JAVA_HOME` to Java 17 before running Maven commands.

```bash
# Find where Java 17 is installed
update-java-alternatives --list    # Linux
/usr/libexec/java_home -V          # macOS

# Set JAVA_HOME for the current terminal session
export JAVA_HOME=/path/to/jdk-17   # replace with actual path
# Example on Ubuntu with oracle JDK:
export JAVA_HOME=/usr/lib/jvm/jdk-17.0.12-oracle-x64

# Verify
java -version
# Must show: openjdk version "17.x.x" ...
```

> **Tip:** Add `export JAVA_HOME=...` to your `~/.bashrc` or `~/.zshrc` so it persists across terminal sessions.

**Verify:**
```bash
$JAVA_HOME/bin/java -version
# Expected: openjdk version "17.x.x"
```

---

### 2.3 Maven

**What it is:** Java build tool. Used to compile the backend, download dependencies, and run tests.

**Install on Ubuntu/Debian:**
```bash
sudo apt install maven -y
```

**Install on macOS:**
```bash
brew install maven
```

**Verify:**
```bash
mvn -version
# Expected: Apache Maven 3.x.x
```

> **Important:** Always prefix Maven commands with `JAVA_HOME=/path/to/java-17` (or export it) to ensure Maven uses Java 17 and not whatever `java` resolves to on your system.

---

### 2.4 Node.js via nvm

**What it is:** Node.js is the JavaScript runtime for building and running the frontend. `nvm` (Node Version Manager) lets you install multiple Node.js versions without conflicts.

**Why nvm instead of system Node?** System-installed Node.js is often outdated. nvm gives you control over the exact version.

**Install nvm:**
```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.1/install.sh | bash

# Reload your shell (or open a new terminal)
source ~/.nvm/nvm.sh
# or: source ~/.bashrc  (Linux)
# or: source ~/.zshrc   (macOS with zsh)
```

**Install Node.js LTS (v20 or v22):**
```bash
nvm install 20
nvm use 20

# Make it the default for new terminals
nvm alias default 20
```

**Verify:**
```bash
node --version   # Expected: v20.x.x
npm --version    # Expected: 10.x.x
```

> **Every time you open a new terminal,** nvm may not auto-activate. If `node` is not found, run:
> ```bash
> source ~/.nvm/nvm.sh && nvm use default
> ```
> Alternatively, add `source ~/.nvm/nvm.sh` to your `~/.bashrc` or `~/.zshrc`.

---

### 2.5 Docker & Docker Compose

**What it is:** Docker runs a PostgreSQL database in an isolated container. Docker Compose manages the container configuration from `docker-compose.yml`.

**You only need Docker to run the local database.** If you already have PostgreSQL installed locally, you can skip Docker and configure the database manually (see §9 for manual DB setup).

**Install Docker Engine on Ubuntu:**
```bash
# Remove old versions
sudo apt remove docker docker-engine docker.io containerd runc 2>/dev/null

# Install prerequisites
sudo apt update
sudo apt install ca-certificates curl gnupg lsb-release -y

# Add Docker's official GPG key
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Add repository
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install Docker Engine + Compose plugin
sudo apt update
sudo apt install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin -y
```

**Install Docker Desktop on macOS:**
```bash
brew install --cask docker
# Then open Docker Desktop from Applications to start the daemon.
```

**Add yourself to the docker group (Linux only — avoids needing `sudo` for docker commands):**
```bash
sudo groupadd docker 2>/dev/null; true
sudo usermod -aG docker $USER
sudo chown root:docker /var/run/docker.sock
# Apply the group change (either log out/in, or run:)
newgrp docker
```

**Verify:**
```bash
docker --version
# Expected: Docker version 24.x.x or newer

docker compose version
# Expected: Docker Compose version v2.x.x
```

---

### 2.6 Recommended IDE

**IntelliJ IDEA** (Community or Ultimate) is recommended for the backend.

- Install the **Java** and **Spring Boot** plugins.
- Open the `backend/` folder as a Maven project (File → Open → select `backend/pom.xml`).
- Install the **Lombok** plugin if needed.

**VS Code** is recommended for the frontend (or the entire repo).

- Install the **ESLint**, **Prettier**, and **TypeScript** extensions.
- Open the `frontend/` folder as the project root for best TypeScript support.

---

## 3. Clone the Repository

```bash
git clone https://github.com/aathith-sanjay/commit.git
cd commit
```

You should now have this structure:
```
commit/
├── backend/
├── frontend/
├── docs/
├── .github/
├── Dockerfile
├── docker-compose.yml
├── render.yaml
└── README.md
```

---

## 4. Project Structure at a Glance

| Directory/File | Purpose |
|---|---|
| `backend/` | Spring Boot REST API (Java 17, Maven) |
| `backend/pom.xml` | Maven dependencies and build config |
| `backend/src/main/resources/application.properties` | Spring Boot config (reads from env vars) |
| `backend/src/main/resources/db/migration/` | Flyway SQL migration files (V1, V2, V3) |
| `backend/.env.example` | Template for your local `.env` file |
| `frontend/` | React + TypeScript frontend (Vite) |
| `frontend/package.json` | NPM dependencies |
| `frontend/vite.config.ts` | Vite config (dev proxy, base path) |
| `docker-compose.yml` | Local PostgreSQL container config |
| `docs/` | Documentation (this file, design.md, local-dev.md) |

---

## 5. Backend Setup

### 5.1 Configure Environment Variables

The backend reads all configuration from environment variables (never hardcoded). Copy the example file and fill in the values:

```bash
cp backend/.env.example backend/.env
```

Open `backend/.env` in a text editor. The contents should look like:

```env
# Local database connection (docker-compose starts PostgreSQL on port 5434)
DB_URL=jdbc:postgresql://localhost:5434/commit
DB_USERNAME=commit
DB_PASSWORD=commit

# JWT signing secret — keep this secret in production!
# For local dev the default value is fine.
JWT_SECRET=commit-default-dev-secret-key-please-change-in-production-1234567890
JWT_EXPIRATION_DAYS=30

# CORS — allow all origins in local dev
CORS_ALLOWED_ORIGINS=*
```

> **For local development**, these defaults work out of the box with the `docker-compose.yml` database.

**What each variable does:**

| Variable | Description |
|---|---|
| `DB_URL` | Full JDBC connection string to PostgreSQL |
| `DB_USERNAME` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |
| `JWT_SECRET` | Secret key for signing JWT tokens. Any string ≥ 32 characters works for local dev. **Use a long random string in production** (`openssl rand -base64 48`) |
| `JWT_EXPIRATION_DAYS` | How many days a login token stays valid before expiring |
| `CORS_ALLOWED_ORIGINS` | Which domains are allowed to call the API. `*` = allow all (fine for local dev) |

---

### 5.2 Start the Database

The database runs as a Docker container. Port `5434` on your machine maps to PostgreSQL's standard port `5432` inside the container.

```bash
# From the repo root (where docker-compose.yml lives)
docker compose up -d
```

**What this does:**
- Pulls the `postgres:17-alpine` image (first time only, ~100 MB)
- Creates a container named `commit-db`
- Starts PostgreSQL with database `commit`, user `commit`, password `commit`
- Stores data in a Docker volume (`commit_db_data`) so data persists between restarts

**Verify the database is running:**
```bash
docker compose ps
# Should show: commit-db ... running

# Optional: connect to the database
docker exec -it commit-db psql -U commit -d commit
# Inside psql: \dt   to list tables (empty until Spring Boot runs Flyway)
# Type \q to exit
```

---

### 5.3 Run the Backend

**Load the environment variables from your `.env` file and start Spring Boot:**

```bash
# Set JAVA_HOME to Java 17 (adjust path to match your installation)
export JAVA_HOME=/usr/lib/jvm/jdk-17.0.12-oracle-x64

# Load .env and run
export $(grep -v '^#' backend/.env | xargs) && \
  mvn -f backend/pom.xml spring-boot:run
```

**What happens on first startup:**
1. Spring Boot connects to PostgreSQL.
2. Flyway checks the `flyway_schema_history` table (auto-created).
3. Since the database is empty, Flyway runs all 3 migration scripts in order:
   - `V1__initial_schema.sql` — creates `habit`, `habit_completion` tables
   - `V2__habit_model_enhancements.sql` — adds columns to `habit`
   - `V3__user_auth.sql` — creates `app_user`, adds `user_id` to `habit`
4. The API starts listening on port 8080.

**Expected output (last few lines):**
```
...
Started CommitApplication in 4.2 seconds (process running for 5.1)
```

> **Tip:** Spring Boot's `spring.jpa.hibernate.ddl-auto=validate` means Hibernate will **validate** (not create/alter) the schema. All schema changes must be done via Flyway migrations. If you add a new column to a JPA entity, you must also add a Flyway migration script.

---

### 5.4 Verify Backend is Running

Open a new terminal and run:

```bash
# Check the health of the API (this endpoint requires a valid JWT)
curl http://localhost:8080/api/v1/habits
# Expected: 401 Unauthorized (correct — you need to register/login first)

# Check OpenAPI docs (no auth needed)
curl http://localhost:8080/v3/api-docs
# Expected: JSON OpenAPI spec

# Or open Swagger UI in a browser:
# http://localhost:8080/swagger-ui.html
```

**Register your first account:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"password123","displayName":"Your Name"}'
# Expected: {"token":"eyJ...","user":{"id":1,"email":"...","displayName":"..."}}
```

Save the `token` value — you'll use it to test authenticated endpoints:

```bash
TOKEN="eyJ..."  # paste your token

curl http://localhost:8080/api/v1/habits \
  -H "Authorization: Bearer $TOKEN"
# Expected: [] (empty array — no habits yet)
```

---

## 6. Frontend Setup

### 6.1 Install Dependencies

```bash
# Load nvm if not already loaded
source ~/.nvm/nvm.sh

# Move into the frontend directory
cd frontend

# Install all npm packages (reads from package.json, creates node_modules/)
npm install
```

This installs React, Vite, TypeScript, Axios, React Router, Recharts, and all other dependencies listed in `package.json`.

---

### 6.2 Configure Environment Variables

Vite reads `.env` files in the `frontend/` directory. Variables must be prefixed with `VITE_` to be accessible in the browser.

For **local development**, you do **not** need a `.env` file because:
- The Vite dev server proxies `/api` → `http://localhost:8080` automatically (see `vite.config.ts`).
- The base path defaults to `/`.

However, if you want to be explicit:

```bash
# frontend/.env.local (gitignored)
# No VITE_API_BASE_URL needed for local — Vite proxy handles it
# VITE_BASE_PATH defaults to '/' which is correct for local dev
```

> **For production builds only:** `VITE_API_BASE_URL` must be set to the backend URL (e.g. `https://commit-backend.onrender.com`) and `VITE_BASE_PATH` to `/commit/` for GitHub Pages. These are set as GitHub Actions environment variables — you don't need them locally.

---

### 6.3 Run the Frontend

```bash
# Make sure you are in the frontend/ directory
cd frontend  # (skip if already there)

# Load nvm
source ~/.nvm/nvm.sh

# Start the Vite development server
npm run dev
```

**Expected output:**
```
  VITE v8.x.x  ready in 300 ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
```

Open your browser at **http://localhost:5173**.

---

### 6.4 Verify Frontend is Running

1. You should see the **commit.** login page.
2. Register an account or log in with your previously created account.
3. After login, you'll be taken to the Dashboard.
4. Try creating a habit via the **+ New** button.
5. Check the browser's Network tab — API calls should go to `localhost:8080` via the Vite proxy.

---

## 7. Running Tests

### Backend Tests

Tests use an **in-memory H2 database** — no PostgreSQL or Docker needed.

```bash
# From the repo root or any directory
export JAVA_HOME=/usr/lib/jvm/jdk-17.0.12-oracle-x64

mvn -f backend/pom.xml test
```

**Expected output:**
```
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- HabitServiceTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- V2HabitServiceTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**What the tests cover:**
- `HabitServiceTest` (4 tests): Creating a habit, tracking a streak, rejecting duplicate completions, tree reset after missed days.
- `V2HabitServiceTest` (5 tests): Schedule-aware streak for SPECIFIC_DAYS and WEEKLY habits, archive/restore, multi-user data isolation.

### Frontend Lint/Build Check

There are no Jest/Vitest tests currently. To verify the TypeScript compiles with no errors:

```bash
cd frontend
source ~/.nvm/nvm.sh
npm run build
```

**Expected:** `✓ built in Xms` with no TypeScript errors in the output.

---

## 8. Port Reference

| Service | Host Port | Description |
|---|---|---|
| Vite dev server | **5173** | Frontend (React app) |
| Spring Boot API | **8080** | Backend REST API |
| PostgreSQL (Docker) | **5434** | Database (mapped from container's 5432) |

> **Why port 5434 instead of 5432?** The project uses 5434 to avoid conflicts with any locally-installed PostgreSQL instance or other projects that may use the default PostgreSQL port. See `docker-compose.yml`.

**Swagger UI:** http://localhost:8080/swagger-ui.html

**OpenAPI JSON spec:** http://localhost:8080/v3/api-docs

---

## 9. Common Issues & Fixes

### ❌ `docker: permission denied`
```
permission denied while trying to connect to the Docker daemon socket
```
**Fix:**
```bash
sudo groupadd docker 2>/dev/null; true
sudo usermod -aG docker $USER
sudo chown root:docker /var/run/docker.sock
newgrp docker
```
Then retry the `docker compose up -d` command.

---

### ❌ `command not found: node`
Node.js is installed via nvm but not loaded in the current terminal.

**Fix:**
```bash
source ~/.nvm/nvm.sh
nvm use default
```
To make this permanent, add `source ~/.nvm/nvm.sh` to your `~/.bashrc` (Linux) or `~/.zshrc` (macOS).

---

### ❌ Maven uses wrong Java version
```
error: release version 17 not supported
```
**Fix:**
```bash
export JAVA_HOME=/path/to/java-17
# Verify
$JAVA_HOME/bin/java -version  # must show 17
mvn -f backend/pom.xml spring-boot:run
```

---

### ❌ Flyway migration error on startup
```
FlywayException: Found non-empty schema(s) ... but no schema history table
```
This means the database has tables from a previous incomplete setup.

**Fix (wipe and restart):**
```bash
docker compose down -v   # removes containers AND the data volume
docker compose up -d     # fresh PostgreSQL with empty database
```
Then restart Spring Boot — Flyway will run all migrations cleanly.

---

### ❌ `Connection refused: localhost:5434`
PostgreSQL container is not running.

**Fix:**
```bash
docker compose ps         # check if container is listed
docker compose up -d      # start if stopped
docker compose logs db    # check logs if it started but crashed
```

---

### ❌ `401 Unauthorized` on all API calls from the frontend
Your JWT token has expired or the `Authorization` header isn't being sent.

**Fix:**
1. Open browser DevTools → Application tab → Local Storage → `localhost:5173`
2. Check if `commit_token` exists.
3. If missing, log in again via the UI.
4. If present, try logging out and logging back in (the token may have expired).

---

### ❌ Frontend shows blank page or wrong routes
This is a Vite base path issue — only relevant when deploying to GitHub Pages. For local development, the base path should always be `/`.

**Fix:**
Ensure no `VITE_BASE_PATH` is set in `frontend/.env.local`. The default `/` is correct for local development.

---

### I don't have Docker — can I use a local PostgreSQL instead?

Yes. Install PostgreSQL locally:
```bash
# Ubuntu
sudo apt install postgresql postgresql-contrib -y
sudo -u postgres psql -c "CREATE USER commit WITH PASSWORD 'commit';"
sudo -u postgres psql -c "CREATE DATABASE commit OWNER commit;"
```

Then update `backend/.env`:
```env
DB_URL=jdbc:postgresql://localhost:5432/commit
DB_USERNAME=commit
DB_PASSWORD=commit
```
(Use port 5432 instead of 5434 — 5434 is only used in `docker-compose.yml` to avoid conflicts.)

---

## 10. Stopping and Resetting

### Stop everything
```bash
# In the terminal running `npm run dev` — press Ctrl+C
# In the terminal running Spring Boot — press Ctrl+C

# Stop the Docker database (keeps data)
docker compose stop

# Or stop AND remove the container (data survives in the volume)
docker compose down
```

### Wipe the database (start fresh)
```bash
# Removes containers AND deletes the PostgreSQL data volume
docker compose down -v
```

After this, the next `docker compose up -d` + Spring Boot start will recreate everything from scratch via Flyway migrations.

---

## 11. Making Code Changes

### Adding a new database column

1. Create a new Flyway migration: `backend/src/main/resources/db/migration/V4__your_description.sql`
2. Write the SQL (`ALTER TABLE ...` or new `CREATE TABLE ...`)
3. Update the corresponding JPA entity class to add the field + getter/setter
4. Update the DTO (add the field to the record if it needs to be exposed via API)
5. Update `HabitService` if the field affects business logic

> **Never** modify existing migration scripts (V1, V2, V3). Flyway tracks which scripts have been applied using checksums. Modifying an applied script will cause a startup error.

### Adding a new API endpoint

1. Add a new method to `HabitController` or `AuthController` with the appropriate `@GetMapping`/`@PostMapping` etc.
2. Create the corresponding method in `HabitService` or `AuthService`
3. Create input/output DTO records in the `dto/` package if needed
4. Add custom exception handling in `ApiExceptionHandler` if the new endpoint can throw new error types
5. Write a test in `HabitServiceTest` or `V2HabitServiceTest`

### Adding a new frontend page

1. Create the page component in `frontend/src/pages/MyPage.tsx`
2. Create a CSS file `frontend/src/pages/MyPage.css` for styles
3. Add the route in `frontend/src/App.tsx`:
   ```tsx
   <Route path="/my-path" element={<ProtectedRoute><MyPage /></ProtectedRoute>} />
   ```
4. Add any new API call functions to `frontend/src/api/habits.ts`
5. Add TypeScript types for new response shapes to `frontend/src/types/index.ts`

### Running the full development loop

**Terminal 1:** Database
```bash
docker compose up -d
```

**Terminal 2:** Backend
```bash
export JAVA_HOME=/usr/lib/jvm/jdk-17.0.12-oracle-x64
export $(grep -v '^#' backend/.env | xargs)
mvn -f backend/pom.xml spring-boot:run
```

**Terminal 3:** Frontend
```bash
source ~/.nvm/nvm.sh
cd frontend && npm run dev
```

**Terminal 4:** Tests (on demand)
```bash
export JAVA_HOME=/usr/lib/jvm/jdk-17.0.12-oracle-x64
mvn -f backend/pom.xml test
```

Open http://localhost:5173 — changes to frontend files hot-reload instantly. Changes to Java files require restarting the Spring Boot process (or use Spring Boot DevTools for auto-restart).
