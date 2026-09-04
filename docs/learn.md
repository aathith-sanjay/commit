# commit. — Learn the Project from Scratch

This guide explains the complete `commit.` project for someone new to full-stack development. Read it in order once; then use the later sections as a reference while you work.

---

## 1. What is this project?

`commit.` is a multi-user habit-tracking web application. A user can:

- create an account and log in;
- create habits, such as reading, exercise, or coding;
- choose when a habit is scheduled;
- mark a habit as complete for a date;
- see completion history, streaks, analytics, and a tree representing consistency;
- keep their habits private from every other user.

The central product rule is simple: completing scheduled habits builds a streak and grows a tree; missing a scheduled habit damages or resets its tree state without deleting the historical record.

The repository currently contains work through **V3**:

| Version | Main outcome |
|---|---|
| V1 | A personal habit tracker: habits, completions, streaks, trees, persistence |
| V2 | Better schedules, editing, archiving, analytics, calendars, responsive UI |
| V3 | Multi-user accounts, password login, JWT authentication, and data isolation |

V4 and later are future work: mobile clients, reminders, social features, and further production hardening.

---

## 2. The big picture

A browser should not connect directly to a database. Instead, this project uses three separate parts:

```text
Your browser
    |
    | HTTPS requests containing JSON
    v
React frontend (GitHub Pages)
    |
    | HTTPS REST API calls
    v
Spring Boot backend (Render)
    |
    | SQL through JDBC / Hibernate
    v
PostgreSQL database (Render)
```

### Why split the application this way?

- **Frontend:** controls what the user sees and how they interact with the app.
- **Backend:** applies business rules, validates requests, authenticates users, and protects data.
- **Database:** stores accounts, habits, and completions permanently.

This separation means a future mobile app can reuse the same backend and database without rewriting the habit logic.

---

## 3. Repository map

```text
commit/
├── backend/                 Java and Spring Boot API
│   ├── pom.xml              Java dependencies and Maven build configuration
│   ├── .env.example         Example local backend environment values
│   └── src/
│       ├── main/java/       Application source code
│       ├── main/resources/  Configuration and database migrations
│       └── test/            Automated backend tests
├── frontend/                React and TypeScript web application
│   ├── package.json         JavaScript dependencies and npm commands
│   ├── .env.example         Example frontend environment values
│   └── src/                 UI, pages, API client, types, and auth state
├── docs/                    Project documentation
├── .github/workflows/       GitHub Actions workflows
├── Dockerfile               Instructions to build a backend container image
├── docker-compose.yml       Local PostgreSQL container configuration
├── render.yaml              Render infrastructure blueprint
├── mvnw, .mvn/              Maven Wrapper files
└── README.md                Quick project introduction and setup guide
```

---

## 4. Frontend fundamentals: React, TypeScript, and Vite

### React

React is a JavaScript library for building user interfaces. Instead of manually changing the browser's HTML each time something changes, you create **components** that describe what the UI should look like for a given state.

For example, a habit card component receives habit data and renders its name, streak, and completion button. When the habit data changes, React updates the relevant part of the screen.

### TypeScript

TypeScript is JavaScript with type checking. It lets the project describe data explicitly:

```ts
type Habit = {
  id: number
  name: string
  currentStreak: number
}
```

This catches errors before code reaches a browser, such as trying to display a field that the backend does not provide.

### Vite

Vite is the frontend build tool and local development server.

- `npm run dev` starts a fast local server, normally at `http://localhost:5173`.
- `npm run build` type-checks the app and creates optimized static files in `frontend/dist/`.

Those static files are HTML, CSS, JavaScript, and assets. GitHub Pages can host them because no frontend server is needed in production.

### Frontend layout

Important frontend directories:

| Location | Purpose |
|---|---|
| `frontend/src/pages/` | Full screens such as Login, Register, Dashboard, and Habit Detail |
| `frontend/src/components/` | Reusable pieces of UI |
| `frontend/src/api/` | Axios client and functions that call the backend API |
| `frontend/src/context/AuthContext.tsx` | Global login state for the current browser session |
| `frontend/src/types/` | TypeScript descriptions of API data |

### The frontend API client

The Axios client sends requests to `${VITE_API_BASE_URL}/api/v1`. Locally, `VITE_API_BASE_URL` is normally `http://localhost:8080`; in production it becomes your Render backend URL.

For protected calls, it reads the JWT from browser `localStorage` and adds this header:

```http
Authorization: Bearer <token>
```

If the backend returns `401 Unauthorized`, the client clears the saved login and redirects to the login page. This prevents a stale or expired token from leaving the UI in a confusing state.

---

## 5. Backend fundamentals: Java, Spring Boot, and Maven

### Java

Java is the backend programming language. The project targets **Java 17**. Java code is compiled into bytecode and runs on a Java Virtual Machine (JVM).

### Spring Boot

Spring Boot is a framework that starts and configures the web application. It provides:

- an embedded HTTP server (Tomcat);
- REST endpoint routing;
- dependency injection;
- database access through JPA/Hibernate;
- validation and consistent error handling;
- security integration.

When you run the backend, Spring Boot starts a web server on port `8080` by default.

### Maven

Maven is the Java build and dependency tool. The backend's `pom.xml` lists libraries such as Spring Boot, PostgreSQL, Flyway, and JWT support.

Common commands from the repository root:

```bash
# Run backend tests with the system Maven installation
mvn -f backend/pom.xml test

# Run backend through the Maven Wrapper
./mvnw -f backend/pom.xml test
./mvnw -f backend/pom.xml spring-boot:run
```

### Maven Wrapper (`mvnw`)

The Maven Wrapper is a small script committed to the repository. It downloads and uses the exact Maven version configured by the project when needed. This makes builds reproducible on a developer machine, GitHub Actions, or Render without relying on a preinstalled Maven version.

The files are:

- `mvnw`: Unix/macOS/Linux wrapper script;
- `mvnw.cmd`: Windows wrapper script;
- `.mvn/wrapper/maven-wrapper.properties`: wrapper configuration.

The Dockerfile uses `./mvnw`, so these files must stay in Git.

---

## 6. How a backend request works

Suppose a logged-in user marks a habit complete.

```text
1. User clicks Complete in React.
2. React calls the API client.
3. Axios sends POST /api/v1/habits/{id}/completions with the JWT header.
4. Spring Security validates the JWT.
5. The controller receives the request.
6. The service checks validation, ownership, scheduling, and duplicates.
7. Hibernate writes a completion row to PostgreSQL.
8. The service recalculates streak and tree state.
9. Spring serializes a response as JSON.
10. React receives JSON and updates the screen.
```

The browser never decides whether a user is allowed to complete a habit that belongs to another user. That authorization decision belongs on the backend.

---

## 7. Backend code structure

The Java code uses a common layered design.

| Package | Responsibility |
|---|---|
| `controller/` | HTTP endpoints: reads requests and returns responses |
| `service/` | Business rules: streaks, tree state, habit ownership, authentication |
| `repository/` | Queries and persistence through Spring Data JPA |
| `entity/` | Java objects mapped to database tables |
| `dto/` | Request and response data shapes used by the API |
| `security/` | JWT creation/validation and authenticated-user handling |
| `config/` | Security, CORS, and request logging configuration |
| `exception/` | API error responses and domain exceptions |

### Controllers

Controllers define routes, for example:

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/auth/me

GET  /api/v1/habits
POST /api/v1/habits
GET  /api/v1/habits/{id}
PUT  /api/v1/habits/{id}
DELETE /api/v1/habits/{id}
```

Controllers should stay thin. They pass work to services instead of embedding streak calculations or database queries.

### Services

Services contain the rules that define product behavior. `HabitService`, for example, handles creation, completion history, scheduled dates, streaks, tree progression, analytics, and ownership checks.

Keeping this logic in one layer makes it testable and prevents different endpoints from applying inconsistent rules.

### Repositories and entities

An **entity** represents stored data. A **repository** provides data access methods for that entity.

Key entities are:

| Entity | Database purpose |
|---|---|
| `AppUser` | A registered account: email, password hash, display name |
| `Habit` | One habit owned by one user, including schedule and current state |
| `HabitCompletion` | A historical record that the habit was completed on a particular date |

The most important V3 relationship is:

```text
AppUser (one)  ----  (many) Habit  ----  (many) HabitCompletion
```

A habit has a `user_id` foreign key. The service uses the authenticated user ID when looking up or changing a habit, so users cannot access another user's habit just by guessing an ID.

---

## 8. The habit domain model

### Scheduling

A habit can use these schedules:

- `DAILY`: scheduled every day;
- `WEEKLY`: scheduled weekly;
- `SPECIFIC_DAYS`: scheduled on selected weekdays, such as Monday, Wednesday, and Friday.

The schedule determines which dates count when calculating streaks and missed completions. A missed unscheduled date must not break a streak.

### Completion history

A completion is stored separately from the current streak. This is important because a streak can reset while the historical achievement remains intact.

```text
Running completion history: Jan 1, Jan 2, Jan 3
Miss Jan 4
Current streak: 0
Historical completions: still Jan 1, Jan 2, Jan 3
```

### Tree state

The tree is a visual expression of habit consistency. Backend state such as `TreeState` and `TreeStage` gives the frontend a stable meaning; the frontend decides how it looks visually.

---

## 9. Authentication and authorization

Authentication answers **who are you?** Authorization answers **are you allowed to do this?**

### Registration

1. User submits email, display name, and password.
2. `AuthService` checks that the email is not already taken.
3. The password is hashed using BCrypt.
4. The backend stores only the password hash, never the plain password.
5. The backend issues a JWT and returns basic user information.

### Login

1. User submits email and password.
2. The backend loads the user by email.
3. BCrypt compares the submitted password with the stored hash.
4. If valid, the backend issues a JWT.

### JWT explained

A JWT (JSON Web Token) is a cryptographically signed string. It identifies the user and contains an expiration time. The backend signs it with `JWT_SECRET`.

```text
Client stores token
       |
       v
Client sends Authorization: Bearer <token>
       |
       v
Backend verifies the token signature and expiration
       |
       v
Backend knows which user made the request
```

The token is not a replacement for backend authorization checks. It identifies the caller; services must still query only records owned by that caller.

### Important JWT safety rules

- Keep `JWT_SECRET` only in environment variables; never commit it.
- Use a long, random secret in production.
- Always use HTTPS in production; GitHub Pages and Render provide this.
- Do not log JWTs or passwords.
- Treat tokens in browser `localStorage` as a pragmatic V3 approach. Future production hardening can evaluate short-lived access tokens and secure HTTP-only refresh cookies.

### Security configuration

`SecurityConfig` permits unauthenticated access to registration, login, OpenAPI docs, and CORS preflight `OPTIONS` calls. All other endpoints require authentication.

---

## 10. PostgreSQL and relational databases

A database stores application data after the backend process stops or redeploys.

PostgreSQL is a relational database. Information is held in tables with rows and columns:

```text
app_user
  id | email | password_hash | display_name

habit
  id | user_id | name | schedule_type | current_streak | tree_state

habit_completion
  id | habit_id | completion_date
```

### Keys and constraints

- A **primary key** uniquely identifies a row, such as `app_user.id`.
- A **foreign key** links tables, such as `habit.user_id -> app_user.id`.
- A **unique constraint** prevents duplicates, such as duplicate account emails or duplicate completions for the same habit/date.
- An **index** makes commonly queried fields faster, such as a user's habit list.

These constraints are important even when the application validates data, because the database is the final protection for persistent data integrity.

---

## 11. Flyway: versioning database changes

### The problem Flyway solves

Code can be versioned with Git. Database structure must also be versioned. Without a tool, every developer and production database can accidentally end up with a different schema.

**Flyway** applies ordered SQL migrations and records which ones have already run in a table named `flyway_schema_history`.

This project contains:

```text
V1__initial_schema.sql
V2__habit_model_enhancements.sql
V3__user_auth.sql
```

On a brand-new production database, Flyway applies V1, then V2, then V3. On the next application startup, it sees those versions in its history table and does nothing.

### Why migrations must not be edited after deployment

Once a migration has run in production, its checksum is stored. Editing it later makes databases disagree and can cause Flyway validation to fail.

The safe approach is:

```text
Need a new schema change?
    |
    v
Create a new migration, e.g. V4__add_reminder_preferences.sql
    |
    v
Commit it and deploy
    |
    v
Flyway applies it exactly once
```

### PostgreSQL support module

Modern Flyway versions split database-specific support into separate dependencies. For PostgreSQL, this project needs both:

```xml
<artifactId>flyway-core</artifactId>
<artifactId>flyway-database-postgresql</artifactId>
```

Without `flyway-database-postgresql`, Flyway cannot handle PostgreSQL migrations correctly; Hibernate then starts against an empty schema and reports errors such as `missing table [app_user]`.

### Flyway versus Hibernate

- **Flyway** creates and evolves the schema deliberately through reviewed SQL migrations.
- **Hibernate validation** checks that entities match the existing schema.

The configuration uses `spring.jpa.hibernate.ddl-auto=validate`, which is a production-safe choice: Hibernate does not silently create or mutate tables. If the schema is missing or wrong, startup fails rather than risking data loss.

---

## 12. Local development environment

You run all three pieces locally: database, backend, and frontend.

### Prerequisites

- Java 17
- Node.js 20 or newer
- npm
- Docker and Docker Compose
- Git

### Environment files

Create untracked local files from examples:

```bash
cp backend/.env.example backend/.env
cp frontend/.env.example frontend/.env
```

Local backend database configuration:

```env
DB_URL=jdbc:postgresql://localhost:5434/commit
DB_USERNAME=commit
DB_PASSWORD=commit
```

Local frontend configuration:

```env
VITE_API_BASE_URL=http://localhost:8080
```

Do not commit `.env` files. They can contain local or production secrets.

### Start local PostgreSQL with Docker Compose

```bash
docker compose up -d
```

The project maps database port `5432` inside the container to host port `5434`:

```text
Your machine: localhost:5434  -->  PostgreSQL container: 5432
```

Check it is running:

```bash
docker compose ps
```

Stop it without deleting data:

```bash
docker compose down
```

### Start the backend

From the repository root:

```bash
JAVA_HOME=/path/to/java-17 ./mvnw -f backend/pom.xml spring-boot:run
```

It listens at `http://localhost:8080`.

### Start the frontend

In another terminal:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

During development, Vite proxies `/api` requests to `http://localhost:8080`. This lets the frontend call the backend without production CORS concerns.

---

## 13. Docker explained

### What is Docker?

Docker packages an application with the operating system libraries and runtime it needs into a **container image**. A container runs that image consistently on your computer, Render, or another platform.

Docker does not replace your application. It gives the application a repeatable runtime environment.

### The project Dockerfile

The root `Dockerfile` is a **multi-stage build**:

```text
Stage 1: build
- starts from a Java 17 JDK image;
- copies Maven Wrapper, backend pom.xml, and source;
- downloads dependencies;
- packages the Spring Boot JAR.

Stage 2: runtime
- starts from a smaller Java 17 JRE image;
- copies only the packaged JAR from Stage 1;
- runs java -jar app.jar.
```

A JDK can compile Java; a JRE is sufficient to run compiled Java. Using a smaller runtime image reduces production image size and attack surface.

### Why the Maven Wrapper mattered

The Dockerfile runs:

```dockerfile
RUN ./mvnw -f backend/pom.xml package -DskipTests -q
```

If `mvnw` and `.mvn/` are missing from Git, Render cannot build the image. That is why they were generated and committed.

---

## 14. Render explained

Render is the hosting platform for the backend and PostgreSQL database.

### The Render Blueprint

`render.yaml` is an infrastructure-as-code blueprint. Instead of clicking every setting manually, it describes what Render should create:

- a managed PostgreSQL database named `commit-db`;
- a Docker web service named `commit-backend`;
- database connection variables injected into the service;
- production CORS origin;
- a generated JWT secret;
- a public health-check endpoint.

### Production environment variables

The backend reads configuration from environment variables:

| Variable | Meaning |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | Where the Render database is |
| `DB_USERNAME`, `DB_PASSWORD` | Database credentials |
| `JWT_SECRET` | Secret used to sign JWTs |
| `JWT_EXPIRATION_DAYS` | Token lifetime |
| `CORS_ALLOWED_ORIGINS` | Browser origin that may call the API |

Render injects the database values through `fromDatabase` in `render.yaml`, and generates the JWT secret. Do not replace the generated secret after users have logged in unless you accept that all existing JWTs will become invalid.

### Deploy flow on Render

```text
Push to GitHub main
    |
    v
Render detects the change
    |
    v
Render builds Dockerfile
    |
    v
Container starts Spring Boot
    |
    v
Spring connects to PostgreSQL
    |
    v
Flyway applies any pending migrations
    |
    v
Hibernate validates the resulting schema
    |
    v
Service listens on port 8080
    |
    v
Render health check calls /v3/api-docs
```

### Why the health check is `/v3/api-docs`

The original health check used `/api/v1/habits`, but that endpoint requires a JWT. Render health checks are anonymous, so it received `401 Unauthorized` and would mark the application unhealthy.

`/v3/api-docs` is public in the security configuration and returns only after the application is operational. It is suitable for the current deployment.

### Render free tier sleep

On the free tier, a service can spin down after a period of inactivity. The first request after that is a **cold start**: Render starts the container, Spring Boot initializes, connects to PostgreSQL, and validates the schema. That can take tens of seconds.

For a personal V3 app, this is acceptable. Later options include a paid always-on plan or an intentional external health ping, subject to Render's current terms and pricing.

---

## 15. GitHub Actions and GitHub Pages

### GitHub Actions

GitHub Actions runs automated workflows in response to repository events.

This repository includes:

| Workflow | Purpose |
|---|---|
| `backend-ci.yml` | Runs `mvn -f backend/pom.xml test` on pushes and pull requests |
| `frontend-ci.yml` | Runs `npm ci` and `npm run build` on pushes and pull requests |
| `frontend-deploy.yml` | Builds and deploys the frontend to GitHub Pages on pushes to `main` |

CI is valuable because it catches broken builds before or immediately after pushing code.

### GitHub Pages

GitHub Pages hosts static files. It serves the Vite production build from the deployment workflow. It does **not** run Java or host PostgreSQL; those live on Render.

The frontend deploy workflow uses:

```yaml
VITE_BASE_PATH: /commit/
VITE_API_BASE_URL: ${{ vars.VITE_API_BASE_URL }}
```

- `VITE_BASE_PATH=/commit/` is required when the repository is hosted at `https://<user>.github.io/commit/` rather than the root domain.
- `VITE_API_BASE_URL` must be set as a GitHub Actions repository variable to your actual Render URL, for example `https://commit-backend.onrender.com`.

### First-time Pages setup

1. Open the GitHub repository.
2. Go to **Settings → Pages**.
3. Select **GitHub Actions** as the source.
4. Go to **Settings → Secrets and variables → Actions → Variables**.
5. Create `VITE_API_BASE_URL` with the exact Render backend URL, no trailing `/api/v1`.
6. Push to `main` or manually run the `frontend-deploy` workflow.

The frontend URL will normally be:

```text
https://<github-user>.github.io/commit/
```

---

## 16. CORS explained

Browsers prevent a website at one origin from freely calling another origin. This browser rule is called the **same-origin policy**.

In production, the frontend and backend are on different origins:

```text
Frontend: https://<github-user>.github.io
Backend:  https://commit-backend.onrender.com
```

**CORS** (Cross-Origin Resource Sharing) is how the backend explicitly says which browser origins are allowed to call it.

`CORS_ALLOWED_ORIGINS` must contain the exact GitHub Pages origin:

```text
https://<github-user>.github.io
```

Do not include the `/commit/` path, because CORS uses origins only: scheme, hostname, and port.

Using `*` in local development is convenient. In production, restricting CORS to the real frontend origin reduces exposure.

---

## 17. Testing and build checks

### Backend tests

Run:

```bash
JAVA_HOME=/path/to/java-17 ./mvnw -f backend/pom.xml test
```

The tests use an in-memory H2 database. They validate core service logic without needing Docker or PostgreSQL.

A passing test suite confirms known tested behavior. It does not prove every possible production scenario is correct, so always perform a deployed end-to-end smoke test too.

### Frontend build

Run:

```bash
cd frontend
npm run build
```

This runs TypeScript checking and produces the production Vite build. A build can succeed even if a feature has a logic bug, but it ensures type errors and bundling failures are caught.

### End-to-end smoke test after deployment

1. Open the GitHub Pages URL.
2. Register a new account.
3. Log in.
4. Create a habit.
5. Complete it.
6. Refresh the page and confirm it persists.
7. Log out and log in again.
8. Confirm a second account cannot see the first account's habits.

---

## 18. Reading production logs and diagnosing startup failures

Render logs are the first place to look when a deploy fails.

### Normal successful startup clues

Look for messages indicating:

- database connection established;
- Flyway migrations successfully applied or validated;
- Hibernate initialized;
- Tomcat started on port `8080`;
- Render health check passed.

### `missing table [app_user]`

This error means Hibernate loaded the `AppUser` entity, but PostgreSQL did not contain the `app_user` table.

The usual investigation path is:

1. Check that Flyway logs appear during startup.
2. Check that `flyway-database-postgresql` is in `backend/pom.xml` alongside `flyway-core`.
3. Check `flyway_schema_history` in the database for migration status.
4. On a brand-new disposable database, recreate it only if needed; Flyway should then apply V1, V2, and V3.
5. Do not delete a production database with real user data to solve a migration problem. Back up first and investigate the migration history.

### `release version 17 not supported`

This means Maven is using a Java runtime older than Java 17, regardless of what your project requires. Verify:

```bash
java -version
./mvnw -version
```

Set `JAVA_HOME` to a Java 17 installation before running Maven. Render's Dockerfile already uses Java 17, and GitHub Actions configures Java 17.

---

## 19. Production-readiness checklist for the current V3 app

Before sharing the app broadly, ensure all items below are true.

### Deployment

- [ ] Render service is live and its health check is passing.
- [ ] Flyway migration history contains successful V1, V2, and V3 entries.
- [ ] GitHub Pages is enabled with GitHub Actions as the source.
- [ ] `VITE_API_BASE_URL` points exactly to the Render HTTPS URL.
- [ ] Production CORS allows only the actual GitHub Pages origin.
- [ ] A real account can register, log in, create a habit, and complete it.

### Security

- [ ] `JWT_SECRET` is generated/stored only in Render environment settings.
- [ ] No committed `.env` file or real secret exists in Git history.
- [ ] Render database credentials are not copied into frontend code.
- [ ] Passwords are stored as BCrypt hashes only.
- [ ] Protected habit routes require JWT authentication.
- [ ] Services validate that a habit belongs to the authenticated user.

### Reliability

- [ ] Backend tests pass.
- [ ] Frontend production build passes.
- [ ] Database backup/export and restore procedure are understood.
- [ ] You understand Render free-tier cold starts.
- [ ] You test a new account and a second account for data isolation.

### Product

- [ ] Browser layout works on mobile and desktop.
- [ ] Invalid forms show understandable errors.
- [ ] Empty, loading, and request-error states are understandable.
- [ ] You use the deployed app personally before adding V4 features.

---

## 20. Safe day-to-day development workflow

Use branches and small, focused commits.

```text
1. Pull the latest main branch.
2. Create a feature branch.
3. Make one focused change.
4. Run backend tests and/or frontend build.
5. Commit with a clear message.
6. Push the branch and open a pull request if applicable.
7. Merge into main only after CI is green.
8. Main triggers Render and GitHub Pages deployments.
9. Smoke-test production after deployment.
```

Before introducing a schema change:

```text
1. Create a new Flyway migration file with the next version.
2. Test it against a clean local database.
3. Review the SQL carefully.
4. Commit it with the code that requires it.
5. Deploy once; never edit that migration after it has reached production.
```

---

## 21. Glossary

| Term | Meaning |
|---|---|
| API | A defined way for programs to communicate, here through HTTP requests and JSON responses |
| Backend | Server-side application that owns business rules and database access |
| Build | Process of compiling/packaging source code into deployable output |
| CI | Continuous Integration: automated checks run after code changes |
| Container | Isolated running instance of a Docker image |
| Docker image | Packaged filesystem and instructions used to run a container |
| DTO | Data Transfer Object: request/response shape separate from database entities |
| Environment variable | Configuration value supplied outside source code, often for secrets or deployment-specific settings |
| Flyway migration | Ordered SQL file that changes a database schema exactly once |
| Frontend | Browser-side interface users see and interact with |
| Hibernate | Java ORM that maps entities to relational database tables |
| HTTP | Protocol used for browser/API communication |
| JPA | Java persistence standard used by Hibernate and Spring Data |
| JWT | Signed token used to identify an authenticated user |
| Maven | Java dependency and build tool |
| ORM | Object-Relational Mapping: bridge between program objects and database tables |
| PostgreSQL | Relational database used by the app |
| Render | Cloud platform hosting this backend and database |
| REST | HTTP API style built around resources and standard methods such as GET and POST |
| Spring Boot | Java framework that runs and configures this backend |
| TypeScript | JavaScript with static type checking |
| Vite | Development server and production builder for the React frontend |

---

## 22. What to learn next

The best order for deepening your understanding is:

1. Use the deployed app daily and note real friction.
2. Read one controller, its service, repository, entity, and DTO as one request path.
3. Read the V1, V2, and V3 Flyway migrations in order.
4. Trace login from `LoginPage` through the API client, `AuthController`, `AuthService`, and `JwtUtil`.
5. Make a small non-destructive feature, such as improving an error message, and deploy it.
6. Learn database backup and restore before storing data you cannot afford to lose.
7. Only then move toward V4 mobile work, reminders, and production hardening.

The key concept is this: the frontend presents data, the backend owns rules and security, the database preserves state, and deployment makes all three available as a working product.
