# commit. — Architecture & Design Document

> This document describes the complete architecture, component design, database schema, and business logic of **commit.** — a personal habit tracker where completed habits grow a visual tree and missed days damage it.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [System Architecture](#2-system-architecture)
3. [Repository Structure](#3-repository-structure)
4. [Backend Design](#4-backend-design)
   - 4.1 [Package Structure](#41-package-structure)
   - 4.2 [Entities](#42-entities)
   - 4.3 [Enums](#43-enums)
   - 4.4 [Repositories](#44-repositories)
   - 4.5 [Services](#45-services)
   - 4.6 [Controllers](#46-controllers)
   - 4.7 [Security Layer](#47-security-layer)
   - 4.8 [Configuration Classes](#48-configuration-classes)
   - 4.9 [DTOs (Data Transfer Objects)](#49-dtos-data-transfer-objects)
   - 4.10 [Exception Handling](#410-exception-handling)
5. [Database Design](#5-database-design)
   - 5.1 [Tables](#51-tables)
   - 5.2 [Entity-Relationship Diagram](#52-entity-relationship-diagram)
   - 5.3 [Flyway Migrations](#53-flyway-migrations)
6. [Core Business Logic](#6-core-business-logic)
   - 6.1 [Schedule Types](#61-schedule-types)
   - 6.2 [Streak Calculation](#62-streak-calculation)
   - 6.3 [Tree State Logic](#63-tree-state-logic)
   - 6.4 [Tree Stage Progression](#64-tree-stage-progression)
   - 6.5 [Analytics Calculation](#65-analytics-calculation)
7. [Frontend Design](#7-frontend-design)
   - 7.1 [Directory Structure](#71-directory-structure)
   - 7.2 [Type System](#72-type-system)
   - 7.3 [API Layer](#73-api-layer)
   - 7.4 [Auth Context](#74-auth-context)
   - 7.5 [Pages](#75-pages)
   - 7.6 [Components](#76-components)
8. [Authentication Flow](#8-authentication-flow)
9. [API Reference](#9-api-reference)
10. [Deployment Architecture](#10-deployment-architecture)
11. [CI/CD Pipelines](#11-cicd-pipelines)
12. [Environment Variables](#12-environment-variables)

---

## 1. Project Overview

**commit.** is a full-stack multi-user habit tracker. The core concept is that each habit has a living tree that grows visually as the user maintains a streak and dies if they miss their scheduled completions.

| Aspect | Technology |
|---|---|
| Backend | Java 17, Spring Boot 4.1.1, Spring Security 6 |
| Database | PostgreSQL 17 (production), H2 in-memory (tests) |
| Migrations | Flyway |
| Auth | JWT (JJWT 0.12.6, HMAC-SHA256) |
| Frontend | React 19, TypeScript, Vite 8 |
| Routing | React Router v7 |
| HTTP Client | Axios |
| Charts | Recharts |
| Deployment | Render (backend), GitHub Pages (frontend) |
| CI | GitHub Actions |

---

## 2. System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                        Browser                          │
│  React + TypeScript (GitHub Pages / Vite dev server)    │
│  • Pages: Login, Register, Dashboard, Detail, Edit      │
│  • AuthContext: JWT stored in localStorage              │
│  • Axios: attaches Bearer token to every request        │
└────────────────────────┬────────────────────────────────┘
                         │ HTTPS / HTTP  (REST + JSON)
                         │ Authorization: Bearer <JWT>
┌────────────────────────▼────────────────────────────────┐
│               Spring Boot REST API (Render)             │
│  • Spring Security 6: validates JWT on every request    │
│  • JwtAuthFilter → SecurityContext → @AuthPrincipal     │
│  • HabitController + AuthController                     │
│  • HabitService + AuthService                           │
│  • Flyway auto-runs migrations on startup               │
└────────────────────────┬────────────────────────────────┘
                         │ JDBC (HikariCP)
┌────────────────────────▼────────────────────────────────┐
│              PostgreSQL (Render Managed DB)             │
│  Tables: app_user, habit, habit_completion              │
└─────────────────────────────────────────────────────────┘
```

**Request lifecycle for a protected endpoint:**
1. Browser sends `GET /api/v1/habits` with `Authorization: Bearer <token>`.
2. `RequestLoggingFilter` logs method + path for observability.
3. `JwtAuthFilter` extracts the token, validates it, loads the `UserDetails`, and sets the `SecurityContext`.
4. Spring Security checks the URL rule — all `/api/v1/habits/**` require authentication.
5. `HabitController.getHabits()` is called; it receives the authenticated `UserPrincipal` via `@AuthenticationPrincipal`.
6. `HabitService.getHabits(includeArchived, userId)` queries only habits belonging to that user.
7. JSON response is returned.

---

## 3. Repository Structure

```
commit/
├── backend/                          Spring Boot application
│   ├── pom.xml                       Maven build descriptor
│   ├── .env.example                  Required environment variables
│   └── src/
│       ├── main/
│       │   ├── java/com/snow/commit/
│       │   │   ├── CommitApplication.java
│       │   │   ├── config/           Spring configuration beans
│       │   │   ├── controller/       REST controllers (HTTP layer)
│       │   │   ├── dto/              Request/response record types
│       │   │   ├── entity/           JPA entities (database models)
│       │   │   ├── exception/        Custom exceptions + global handler
│       │   │   ├── repository/       Spring Data JPA repositories
│       │   │   ├── security/         JWT utilities + Spring Security
│       │   │   └── service/          Business logic
│       │   └── resources/
│       │       ├── application.properties
│       │       └── db/migration/     Flyway SQL scripts
│       └── test/                     JUnit/Spring tests (H2 in-memory)
│
├── frontend/                         Vite React TypeScript app
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── App.tsx                   Router + AuthProvider root
│       ├── main.tsx                  React DOM entry point
│       ├── index.css                 Global styles
│       ├── api/                      Axios API functions
│       ├── components/               Reusable UI components
│       ├── context/                  React context (AuthContext)
│       ├── pages/                    Route-level page components
│       └── types/                    TypeScript type definitions
│
├── .github/workflows/                GitHub Actions CI/CD
├── Dockerfile                        Multi-stage Docker build
├── docker-compose.yml                Local PostgreSQL container
├── render.yaml                       Render Blueprint (production)
└── docs/                             Documentation
```

---

## 4. Backend Design

### 4.1 Package Structure

```
com.snow.commit
├── CommitApplication          Spring Boot entry point
├── config/
│   ├── SecurityConfig         Spring Security filter chain configuration
│   ├── WebConfig              CORS configuration
│   └── RequestLoggingFilter   HTTP request/response logging
├── controller/
│   ├── AuthController         POST /auth/register, POST /auth/login, GET /auth/me
│   └── HabitController        All /habits/** endpoints
├── dto/                       Input/output record types (no JPA)
├── entity/                    JPA-mapped database models
├── exception/                 Custom exceptions + global @RestControllerAdvice
├── repository/                Spring Data JPA interfaces
├── security/
│   ├── JwtUtil                Token generation and validation
│   ├── JwtAuthFilter          Per-request token extraction
│   ├── UserDetailsServiceImpl Loads AppUser from DB for Spring Security
│   └── UserPrincipal          Wraps AppUser as Spring Security UserDetails
└── service/
    ├── AuthService            Register, login, user lookup
    └── HabitService           All habit CRUD + streak + analytics logic
```

---

### 4.2 Entities

#### `AppUser` — `com.snow.commit.entity.AppUser`
Represents a registered user account. Maps to the `app_user` table.

| Field | Type | Description |
|---|---|---|
| `id` | `Long` | Primary key, auto-incremented |
| `email` | `String` | Unique login identifier, max 255 chars |
| `passwordHash` | `String` | BCrypt-hashed password (never stored in plain text) |
| `displayName` | `String` | User's display name shown in the UI, max 150 chars |
| `createdAt` | `LocalDateTime` | Account creation timestamp |

Relationships:
- One `AppUser` has many `Habit` objects (not mapped as a Java collection — accessed via `HabitRepository.findByUserId`).

---

#### `Habit` — `com.snow.commit.entity.Habit`
Represents a single habit belonging to a user. Maps to the `habit` table.

| Field | Type | Description |
|---|---|---|
| `id` | `Long` | Primary key |
| `name` | `String` | Habit name, max 150 chars, required |
| `description` | `String` | Optional free-text description |
| `category` | `String` | Optional category label (Health, Fitness, etc.), max 80 chars |
| `scheduleType` | `ScheduleType` | Enum: DAILY, WEEKLY, SPECIFIC_DAYS |
| `scheduleDays` | `String` | Comma-separated day abbreviations, e.g. `"MON,WED,FRI"`. Only used when `scheduleType = SPECIFIC_DAYS` |
| `active` | `boolean` | `true` = active, `false` = archived |
| `startDate` | `LocalDate` | Date tracking begins. Completions before this date are ignored |
| `endDate` | `LocalDate` | Optional end date. Tracking stops here if set |
| `timezone` | `String` | IANA timezone (default: `Asia/Kolkata`). Used for future reminder logic |
| `currentStreak` | `int` | Cached value — consecutive scheduled units completed up to today |
| `longestStreak` | `int` | Cached value — all-time best streak |
| `treeState` | `TreeState` | Enum: ALIVE or DEAD |
| `user` | `AppUser` | Many-to-one FK to `app_user`. Enforces data isolation |
| `createdAt` | `LocalDateTime` | Creation timestamp, never updated |
| `updatedAt` | `LocalDateTime` | Last modification timestamp |

> **Why cache streak on the entity?** Recalculating streak on every list fetch would require joining `habit_completion` for every row. By caching `currentStreak` and `longestStreak` on the `Habit` entity and refreshing them after any completion change, list endpoints stay fast.

---

#### `HabitCompletion` — `com.snow.commit.entity.HabitCompletion`
Records a single completion event for a habit on a specific date. Maps to `habit_completion`.

| Field | Type | Description |
|---|---|---|
| `id` | `Long` | Primary key |
| `habit` | `Habit` | Many-to-one FK to `habit` |
| `completionDate` | `LocalDate` | The calendar date of completion |
| `createdAt` | `LocalDateTime` | Exact timestamp when the record was created |

A unique constraint on `(habit_id, completion_date)` prevents recording more than one completion per habit per calendar day.

---

### 4.3 Enums

#### `ScheduleType`
Controls which days a habit is "due" — used by the streak engine to know which days count.

| Value | Meaning |
|---|---|
| `DAILY` | Habit is due every calendar day |
| `WEEKLY` | Habit needs one completion per ISO week (Mon–Sun) |
| `SPECIFIC_DAYS` | Habit is due only on specific days stored in `scheduleDays` (e.g. `MON,WED,FRI`) |

#### `TreeStage`
The visual stage of the habit's tree, determined purely by `currentStreak`.

| Stage | Minimum streak | Emoji used in UI |
|---|---|---|
| `SEED` | 0 | 🌱 |
| `HERB` | 1 | 🌿 |
| `SHRUB` | 3 | 🪴 |
| `SAPLING` | 7 | 🌳 |
| `YOUNG_TREE` | 14 | 🌲 |
| `TREE` | 21 | 🌳 |
| `FLOWERING_TREE` | 30 | 🌸 |
| `FRUIT_TREE` | 60 | 🍎 |
| `MATURE_TREE` | 90 | 🏔️ |

#### `TreeState`
Whether the tree is alive or dead, based on whether the most recent due unit was completed.

| Value | Meaning |
|---|---|
| `ALIVE` | The last due scheduled unit was completed |
| `DEAD` | The last due scheduled unit was missed |
| `RECOVERING` | Reserved for future use |

---

### 4.4 Repositories

All repositories extend `JpaRepository<Entity, Long>` which provides standard `save`, `findById`, `findAll`, `delete` methods. Custom queries are declared as method name-based queries (Spring Data JPA derives the SQL automatically from the method name).

#### `AppUserRepository`

| Method | Purpose |
|---|---|
| `findByEmail(String email)` | Look up a user by email for login or token validation |
| `existsByEmail(String email)` | Fast check before registering to prevent duplicate emails |

#### `HabitRepository`

| Method | Purpose |
|---|---|
| `findByUserId(Long userId)` | Return ALL habits (active + archived) for a user |
| `findByActiveAndUserId(boolean active, Long userId)` | Return only active or only archived habits for a user |

#### `HabitCompletionRepository`

| Method | Purpose |
|---|---|
| `existsByHabitIdAndCompletionDate(...)` | Guard against duplicate completions before saving |
| `findByHabitIdOrderByCompletionDateDesc(...)` | Return full history sorted newest-first |
| `findByHabitIdAndCompletionDateBetween...(...)` | Return completions in a date range (used by streak engine) |
| `countByHabitId(Long habitId)` | Total completions count for analytics |
| `findByHabitIdAndCompletionDate(...)` | Find a specific completion record (used by undo) |
| `findTopByHabitIdOrderByCompletionDateDesc(...)` | Latest completion (used by streak engine) |

---

### 4.5 Services

#### `AuthService` — `com.snow.commit.service.AuthService`

Handles user registration, login, and user data conversion.

**`register(RegisterRequest request) → AuthResponse`**
1. Checks if `email` is already taken via `AppUserRepository.existsByEmail`. Throws `EmailAlreadyExistsException` (HTTP 409) if so.
2. Creates a new `AppUser`, encodes password with BCrypt, saves to DB.
3. Generates a JWT via `JwtUtil.generateToken`.
4. Returns `AuthResponse { token, user }`.

**`login(LoginRequest request) → AuthResponse`**
1. Finds user by email. Throws `InvalidCredentialsException` (HTTP 401) if not found.
2. Verifies password with `BCryptPasswordEncoder.matches`. Throws `InvalidCredentialsException` if wrong.
3. Generates a JWT, returns `AuthResponse`.

**`toUserDto(AppUser user) → UserDto`**
Converts an `AppUser` entity to `UserDto { id, email, displayName }` for safe public exposure.

---

#### `HabitService` — `com.snow.commit.service.HabitService`

The core of the application. All public methods take `Long userId` as the last parameter to enforce data isolation — a user can only read or modify their own habits.

**Public methods:**

| Method | HTTP Trigger | Description |
|---|---|---|
| `createHabit(request, userId)` | `POST /habits` | Creates a habit, sets defaults, saves, recalculates metrics |
| `getHabits(includeArchived, userId)` | `GET /habits` | Returns all habits for the user (optionally including archived) |
| `getHabit(id, userId)` | `GET /habits/{id}` | Returns a single habit; throws 404 if not found or not owned |
| `updateHabit(id, request, userId)` | `PUT /habits/{id}` | Patches provided fields, recalculates metrics |
| `archiveHabit(id, userId)` | `PATCH /habits/{id}/archive` | Sets `active = false` |
| `restoreHabit(id, userId)` | `PATCH /habits/{id}/restore` | Sets `active = true` |
| `deleteHabit(id, userId)` | `DELETE /habits/{id}` | Permanently deletes habit + all completions (cascade) |
| `completeHabit(id, request, userId)` | `POST /habits/{id}/completions` | Saves a completion, refreshes metrics |
| `undoCompletion(id, date, userId)` | `DELETE /habits/{id}/completions/{date}` | Deletes a completion record, refreshes metrics |
| `getHistory(id, userId)` | `GET /habits/{id}/history` | Returns all completions newest-first |
| `getStreak(id, userId)` | `GET /habits/{id}/streak` | Returns cached streak + todayCompleted |
| `getTree(id, userId)` | `GET /habits/{id}/tree` | Returns tree state + stage |
| `getAnalytics(id, userId)` | `GET /habits/{id}/analytics` | Computes full analytics stats |

**Private helper methods (internal to HabitService):**

| Method | Description |
|---|---|
| `getHabitEntity(id, userId)` | Fetches by ID, throws 404 if missing or if `habit.user.id ≠ userId` (ownership check) |
| `refreshHabitMetrics(habit)` | Calls `calculateMetrics`, updates and saves `currentStreak`, `longestStreak`, `treeState` |
| `calculateMetrics(habit)` | Computes all live metrics from completion history (see §6) |
| `getScheduledUnitKeys(habit, trackingEnd)` | Returns list of dates/weeks that were scheduled between `startDate` and `trackingEnd` |
| `getCompletedUnitKeys(habit, completions, end)` | Maps raw completion dates to their "unit keys" (date for DAILY/SPECIFIC_DAYS, week-start for WEEKLY) |
| `calculateCurrentStreak(scheduled, completed)` | Counts consecutive most-recent completed units |
| `calculateLongestStreak(scheduled, completed)` | Scans all units for the longest run |
| `determineTreeState(...)` | Compares most-recently-due unit with most-recently-completed unit |
| `determineTreeStage(streak)` | Maps streak count to `TreeStage` enum via threshold table |
| `calculateConsistencyScore(scheduled, completed)` | `completed.size / scheduled.size` as a 0–1 ratio |

---

### 4.6 Controllers

All controllers are annotated `@RestController` and live under `/api/v1`.

#### `AuthController` — `/api/v1/auth`

| Method | Path | Auth Required | Description |
|---|---|---|---|
| `POST` | `/auth/register` | ❌ No | Register a new user; returns JWT + user info |
| `POST` | `/auth/login` | ❌ No | Login with email/password; returns JWT + user info |
| `GET` | `/auth/me` | ✅ Yes | Returns current user's profile data |

#### `HabitController` — `/api/v1`

| Method | Path | Description |
|---|---|---|
| `GET` | `/habits` | List active habits (`?includeArchived=true` for all) |
| `POST` | `/habits` | Create a new habit |
| `GET` | `/habits/{id}` | Get a single habit by ID |
| `PUT` | `/habits/{id}` | Update habit fields |
| `DELETE` | `/habits/{id}` | Permanently delete a habit |
| `PATCH` | `/habits/{id}/archive` | Soft-delete (hide from main list) |
| `PATCH` | `/habits/{id}/restore` | Un-archive a habit |
| `POST` | `/habits/{id}/completions` | Mark today (or a specific date) as complete |
| `DELETE` | `/habits/{id}/completions/{date}` | Undo a completion for a given date |
| `GET` | `/habits/{id}/history` | Full completion history |
| `GET` | `/habits/{id}/streak` | Current and longest streak |
| `GET` | `/habits/{id}/tree` | Tree state and stage |
| `GET` | `/habits/{id}/analytics` | Full analytics including weekly/monthly stats |

Every `HabitController` method injects `@AuthenticationPrincipal UserPrincipal currentUser` and passes `currentUser.getUserId()` to the service, ensuring strict per-user data isolation.

---

### 4.7 Security Layer

#### `JwtUtil` — `com.snow.commit.security.JwtUtil`

Manages JWT token lifecycle using JJWT 0.12.6 with HMAC-SHA256 signing.

- **`generateToken(AppUser user)`**: Creates a signed JWT with `sub = email`, `userId` claim, `iat` (issued-at), and `exp` (expiry). The expiry is `jwt.expiration-days × 24h` from now (default 30 days).
- **`parseToken(String token)`**: Validates signature and parses claims. Throws if expired or tampered.
- **`isValid(String token)`**: Wraps `parseToken` in a try/catch — returns `false` for any invalid/expired token.
- **Signing key**: The `jwt.secret` property is read as UTF-8 bytes. If shorter than 32 bytes, it is zero-padded to meet HMAC-SHA256's 256-bit minimum.

#### `JwtAuthFilter` — `com.snow.commit.security.JwtAuthFilter`

Extends `OncePerRequestFilter` — runs exactly once per HTTP request before controllers.

1. Reads `Authorization` header.
2. Strips `Bearer ` prefix to get the raw token.
3. Calls `JwtUtil.isValid(token)`.
4. If valid: extracts `email` from claims, loads `UserDetails` via `UserDetailsServiceImpl`, creates a `UsernamePasswordAuthenticationToken`, and sets it in `SecurityContextHolder`.
5. Calls `chain.doFilter` regardless (allows Spring Security to enforce URL rules).

#### `UserPrincipal` — `com.snow.commit.security.UserPrincipal`

Implements Spring Security's `UserDetails`. Wraps an `AppUser` entity.

- `getUsername()` → returns the user's email
- `getPassword()` → returns the BCrypt hash
- `getUserId()` → returns `appUser.getId()` — used by controllers to scope queries
- `getUser()` → returns the raw `AppUser` — used by `AuthController.me()`
- All account status methods (`isEnabled`, `isAccountNonExpired`, etc.) return `true` unconditionally

#### `UserDetailsServiceImpl` — `com.snow.commit.security.UserDetailsServiceImpl`

Implements `UserDetailsService`. Spring Security calls this when validating credentials.

- `loadUserByUsername(String email)` → looks up `AppUser` by email, wraps in `UserPrincipal`. Throws `UsernameNotFoundException` if not found.

#### `SecurityConfig` — `com.snow.commit.config.SecurityConfig`

Configures the Spring Security filter chain:

- **CSRF**: disabled (REST API, no browser form sessions)
- **Sessions**: stateless (no HTTP session created; JWT is the only auth mechanism)
- **CORS**: applied via `WebConfig` bean (the `Customizer.withDefaults()` call delegates to `WebMvcConfigurer`)
- **Public URLs**: `OPTIONS /**` (preflight), `/api/v1/auth/register`, `/api/v1/auth/login`, `/v3/api-docs/**`, `/swagger-ui/**`
- **All other URLs**: require authentication
- **Filter order**: `JwtAuthFilter` runs before `UsernamePasswordAuthenticationFilter`
- **`PasswordEncoder`**: `BCryptPasswordEncoder` with default strength 10
- **`AuthenticationManager`**: exposed as a bean (required for programmatic auth if needed in tests)

---

### 4.8 Configuration Classes

#### `WebConfig` — `com.snow.commit.config.WebConfig`

Configures CORS. Reads allowed origins from `cors.allowed-origins` property (defaults to `*` for local dev).

- Maps `/api/**`
- Allowed methods: `GET, POST, PUT, DELETE, OPTIONS`
- Allowed headers: `*` (permits `Authorization`, `Content-Type`, etc.)
- Exposed headers: `Authorization` (so browsers can read it in JS)
- Max age: 3600 seconds (1 hour preflight cache)

In production, `CORS_ALLOWED_ORIGINS=https://aathith-sanjay.github.io` restricts cross-origin access to only the GitHub Pages domain.

#### `RequestLoggingFilter` — `com.snow.commit.config.RequestLoggingFilter`

A simple `OncePerRequestFilter` that logs every HTTP request with method, path, status code, and duration in milliseconds. Uses SLF4J (`INFO` level).

Example log line: `GET /api/v1/habits -> 200 (34 ms)`

---

### 4.9 DTOs (Data Transfer Objects)

All DTOs are Java `record` types — immutable, compact, and serialized/deserialized by Jackson automatically.

#### Request DTOs (incoming from client)

| Record | Fields | Purpose |
|---|---|---|
| `RegisterRequest` | `email`, `password` (min 8), `displayName` | Create a new account |
| `LoginRequest` | `email`, `password` | Authenticate |
| `CreateHabitRequest` | `name`, `startDate`, `scheduleType`, `scheduleDays`, `description`, `category`, `endDate`, `timezone` | Create a habit |
| `UpdateHabitRequest` | All optional versions of the same fields + `active` | Partially update a habit |
| `CompletionRequest` | `completionDate` (LocalDate) | Record a completion |

All request DTOs use Bean Validation annotations (`@NotBlank`, `@Email`, `@Size`) — violations return HTTP 400 with a meaningful error message.

#### Response DTOs (outgoing to client)

| Record | Fields | Purpose |
|---|---|---|
| `AuthResponse` | `token`, `user` (UserDto) | Returned after register/login |
| `UserDto` | `id`, `email`, `displayName` | Safe user representation (no password) |
| `HabitResponse` | All habit fields including `treeState`, `treeStage`, `todayCompleted` | Standard habit representation |
| `HabitCompletionResponse` | `id`, `habitId`, `completionDate`, `createdAt` | Single completion record |
| `StreakResponse` | `currentStreak`, `longestStreak`, `todayCompleted` | Streak-only response |
| `TreeResponse` | `treeState`, `treeStage`, `currentStreak`, `longestStreak` | Tree-only response |
| `AnalyticsResponse` | `totalCompletions`, `completionRate`, `currentStreak`, `longestStreak`, `consistencyScore`, `weeklyStats[]`, `monthlyStats[]` | Full analytics |
| `ApiError` | `timestamp`, `status`, `error`, `message` | Standardized error response |

---

### 4.10 Exception Handling

`ApiExceptionHandler` (`@RestControllerAdvice`) catches all exceptions thrown anywhere in the controller or service layer and maps them to `ApiError` JSON responses.

| Exception | HTTP Status | Trigger |
|---|---|---|
| `HabitNotFoundException` / `ResourceNotFoundException` | 404 Not Found | Habit ID does not exist or belongs to another user |
| `DuplicateCompletionException` | 409 Conflict | Completing a habit that was already completed on that date |
| `EmailAlreadyExistsException` | 409 Conflict | Registering with an email that already has an account |
| `InvalidCredentialsException` | 401 Unauthorized | Wrong email or password at login |
| `IllegalArgumentException` | 400 Bad Request | Invalid input (empty name, bad date range, etc.) |
| `MethodArgumentNotValidException` | 400 Bad Request | Bean Validation failure on a request DTO |

---

## 5. Database Design

### 5.1 Tables

#### `app_user`

| Column | Type | Constraints |
|---|---|---|
| `id` | `BIGSERIAL` | PRIMARY KEY |
| `email` | `VARCHAR(255)` | NOT NULL, UNIQUE |
| `password_hash` | `VARCHAR(255)` | NOT NULL |
| `display_name` | `VARCHAR(150)` | NOT NULL |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT now() |

---

#### `habit`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `BIGSERIAL` | PRIMARY KEY | |
| `name` | `VARCHAR(150)` | NOT NULL | |
| `schedule_type` | `VARCHAR(50)` | NOT NULL, DEFAULT 'DAILY' | One of: DAILY, WEEKLY, SPECIFIC_DAYS |
| `active` | `BOOLEAN` | NOT NULL, DEFAULT TRUE | FALSE = archived |
| `start_date` | `DATE` | NOT NULL | Tracking begins here |
| `current_streak` | `INTEGER` | NOT NULL, DEFAULT 0 | Cached, recomputed on every completion change |
| `longest_streak` | `INTEGER` | NOT NULL, DEFAULT 0 | Cached all-time best |
| `tree_state` | `VARCHAR(30)` | NOT NULL, DEFAULT 'DEAD' | ALIVE or DEAD |
| `created_at` | `TIMESTAMP` | NOT NULL | |
| `updated_at` | `TIMESTAMP` | NOT NULL | |
| `description` | `TEXT` | nullable | Added in V2 migration |
| `category` | `VARCHAR(80)` | nullable | Added in V2 migration |
| `end_date` | `DATE` | nullable | Added in V2 migration |
| `timezone` | `VARCHAR(60)` | NOT NULL, DEFAULT 'Asia/Kolkata' | Added in V2 migration |
| `schedule_days` | `VARCHAR(30)` | nullable | e.g. `MON,WED,FRI`. Added in V2 migration |
| `user_id` | `BIGINT` | NOT NULL, FK → app_user(id) ON DELETE CASCADE | Added in V3 migration |

**Indexes:**
- `idx_habit_active` on `(active)` — for filtering active habits
- `idx_habit_category` on `(category)` — for category filtering
- `idx_habit_user_id` on `(user_id)` — for fast per-user lookups

---

#### `habit_completion`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `BIGSERIAL` | PRIMARY KEY | |
| `habit_id` | `BIGINT` | NOT NULL, FK → habit(id) ON DELETE CASCADE | Deletions cascade — completing a habit owns completions |
| `completion_date` | `DATE` | NOT NULL | Calendar date (not datetime) |
| `created_at` | `TIMESTAMP` | NOT NULL | Exact time of the API call |

**Unique constraint:** `(habit_id, completion_date)` — only one completion per habit per calendar day.

**Index:** `idx_habit_completion_habit_date` on `(habit_id, completion_date)`.

---

### 5.2 Entity-Relationship Diagram

```
┌──────────────┐       1        ┌──────────────────────┐
│   app_user   │ ──────────────< │        habit         │
├──────────────┤                 ├──────────────────────┤
│ id (PK)      │                 │ id (PK)              │
│ email        │                 │ name                 │
│ password_hash│                 │ schedule_type        │
│ display_name │                 │ schedule_days        │
│ created_at   │                 │ active               │
└──────────────┘                 │ start_date           │
                                 │ end_date             │
                                 │ timezone             │
                                 │ category             │
                                 │ description          │
                                 │ current_streak       │
                                 │ longest_streak       │
                                 │ tree_state           │
                                 │ user_id (FK)         │
                                 │ created_at           │
                                 │ updated_at           │
                                 └──────────┬───────────┘
                                            │ 1
                                            │
                                            │ *
                                 ┌──────────▼───────────┐
                                 │   habit_completion   │
                                 ├──────────────────────┤
                                 │ id (PK)              │
                                 │ habit_id (FK)        │
                                 │ completion_date      │
                                 │ created_at           │
                                 └──────────────────────┘
```

---

### 5.3 Flyway Migrations

Flyway runs automatically on startup and applies pending migrations in order.

| File | Version | What it does |
|---|---|---|
| `V1__initial_schema.sql` | V1 | Creates `habit` and `habit_completion` tables with basic columns |
| `V2__habit_model_enhancements.sql` | V2 | Adds `description`, `category`, `end_date`, `timezone`, `schedule_days` to `habit`; adds indexes |
| `V3__user_auth.sql` | V3 | Creates `app_user` table; inserts migration user `admin@commit.local`; adds `user_id` FK to `habit`; assigns all pre-existing habits to the migration user |

**Test environment**: Flyway is disabled in the `test` Spring profile (`spring.flyway.enabled=false`). H2 creates the schema from JPA entities automatically (`ddl-auto=create-drop`).

---

## 6. Core Business Logic

### 6.1 Schedule Types

The schedule type controls which calendar dates are "due" for a habit.

**DAILY**: Every day from `startDate` to `trackingEnd`. A date is a scheduled unit.

**SPECIFIC_DAYS**: Only dates whose day-of-week matches `scheduleDays`. For example, `MON,WED,FRI` means the habit is due every Monday, Wednesday, and Friday. Stored as a comma-separated string of abbreviations (MON, TUE, WED, THU, FRI, SAT, SUN).

**WEEKLY**: Each ISO week (Monday–Sunday) from `startDate` is treated as one unit. The "key" for a weekly unit is the Monday of that week (`startOfWeek(date)`). One completion anywhere within the week satisfies the unit.

---

### 6.2 Streak Calculation

The streak engine works with two abstract lists:

- **`scheduledUnits`**: All dates (or week-start dates for WEEKLY) between `startDate` and `trackingEnd` that the habit was supposed to be completed.
- **`completedUnits`**: The subset of scheduled units that were actually completed (mapped from raw `HabitCompletion` records to their unit keys).

**`calculateCurrentStreak`**:
Scans from the most recent scheduled unit backwards. Finds the most recent completed unit, then counts backwards consecutively until a gap (uncompleted unit) is found. Returns that count.

Example for DAILY habit completed Mon–Fri, missed Saturday, completed Sunday:
- scheduledUnits: [Mon, Tue, Wed, Thu, Fri, Sat, Sun]
- completedUnits: {Mon, Tue, Wed, Thu, Fri, Sun}
- Most recent completed = Sun. Count backwards: Sun ✓ (1), Sat ✗ — stop. Streak = 1.

**`calculateLongestStreak`**:
Scans all scheduled units left-to-right, maintaining a running counter. Increments on completed units, resets to 0 on missed units. Tracks the maximum seen.

---

### 6.3 Tree State Logic

The tree is **ALIVE** if the most recently due scheduled unit was completed. It is **DEAD** if the most recently due unit was missed.

"Most recently due" means: the last scheduled unit that falls on or before yesterday (not today, because today is still pending). For WEEKLY habits, the last full week that ended before today.

This design means:
- If you complete a daily habit today → ALIVE.
- If you miss today but completed yesterday → still ALIVE (yesterday's due unit was completed).
- If you miss yesterday → DEAD.
- New habits with no completions yet → DEAD (no completed units).

---

### 6.4 Tree Stage Progression

The visual stage advances based purely on `currentStreak`:

```
streak 0      → SEED 🌱
streak 1–2    → HERB 🌿
streak 3–6    → SHRUB 🪴
streak 7–13   → SAPLING 🌳
streak 14–20  → YOUNG_TREE 🌲
streak 21–29  → TREE 🌳
streak 30–59  → FLOWERING_TREE 🌸
streak 60–89  → FRUIT_TREE 🍎
streak ≥ 90   → MATURE_TREE 🏔️
```

When a streak breaks and resets to 0, the tree stage goes back to SEED and the tree is marked DEAD — but all historical `habit_completion` records are preserved. The `longestStreak` is never decremented.

---

### 6.5 Analytics Calculation

`getAnalytics` computes from the full history since `startDate`:

- **`totalCompletions`**: Count of all `HabitCompletion` records.
- **`completionRate`**: `completedUnits.size / scheduledUnits.size` (0.0–1.0).
- **`consistencyScore`**: Same formula but computed over a rolling recent window for a "smoothed" score.
- **`weeklyStats`**: For each ISO week between `startDate` and today, counts `scheduled` and `completed` units. Produces a `WeekStat { weekLabel, scheduled, completed }` per week.
- **`monthlyStats`**: Same aggregation but grouped by month (YYYY-MM label).

---

## 7. Frontend Design

### 7.1 Directory Structure

```
frontend/src/
├── App.tsx                  Root component: BrowserRouter + AuthProvider + Routes
├── main.tsx                 React.createRoot entry point
├── index.css                Global reset, .btn variants, focus-visible, scrollbar
│
├── api/
│   ├── client.ts            Axios instance, JWT interceptor, 401 redirect
│   ├── auth.ts              register(), login(), getMe() API calls
│   └── habits.ts            All habit CRUD + completions + analytics API calls
│
├── context/
│   └── AuthContext.tsx      AuthProvider + useAuth hook
│
├── types/
│   ├── index.ts             Habit, HabitCompletion, AnalyticsResponse, enums, payloads
│   └── auth.ts              User, AuthResponse, LoginPayload, RegisterPayload
│
├── pages/
│   ├── LoginPage.tsx        /login — email + password sign-in form
│   ├── RegisterPage.tsx     /register — display name + email + password
│   ├── Dashboard.tsx        / — habit list, garden view, category filters
│   ├── HabitDetail.tsx      /habits/:id — full detail with charts, calendar
│   ├── CreateHabit.tsx      /habits/new — create habit form
│   ├── EditHabit.tsx        /habits/:id/edit — edit habit form (pre-filled)
│   └── AccountSettings.tsx  /account — email, display name, sign out
│
└── components/
    ├── ProtectedRoute.tsx       Redirects to /login if not authenticated
    ├── HabitCard.tsx            Habit list item with complete button
    ├── TreeBadge.tsx            Pill showing tree emoji + stage name
    ├── ContributionCalendar.tsx 52-week GitHub-style contribution grid
    ├── CompletionCalendar.tsx   (Legacy 6-week calendar, kept for reference)
    └── SkeletonCard.tsx         Shimmer loading placeholder
```

---

### 7.2 Type System

**`frontend/src/types/index.ts`** — Habit domain types

- `TreeState`: `'ALIVE' | 'DEAD' | 'RECOVERING'`
- `TreeStage`: union of 9 stage string literals
- `ScheduleType`: `'DAILY' | 'WEEKLY' | 'SPECIFIC_DAYS'`
- `DayOfWeek`: derived from `DAYS_OF_WEEK` const array `['MON', 'TUE', ...]`
- `Habit`: mirrors `HabitResponse` from backend — all habit fields
- `HabitCompletion`: mirrors `HabitCompletionResponse`
- `StreakResponse`, `TreeResponse`, `AnalyticsResponse`, `WeekStat`, `MonthStat`
- `CreateHabitPayload`, `UpdateHabitPayload`: request shapes for API calls

**`frontend/src/types/auth.ts`** — Auth domain types

- `User { id, email, displayName }` — safe user representation
- `AuthResponse { token, user }` — what register/login return
- `LoginPayload`, `RegisterPayload` — form submission shapes

---

### 7.3 API Layer

**`frontend/src/api/client.ts`** — Axios instance

- Base URL: `VITE_API_BASE_URL + /api/v1` (production) or `/api/v1` (dev, proxied by Vite to `localhost:8080`)
- **Request interceptor**: reads `commit_token` from `localStorage`, adds `Authorization: Bearer <token>` header to every outgoing request.
- **Response interceptor**: if a response is HTTP 401, clears `commit_token` and `commit_user` from localStorage and redirects to `/login`.

**`frontend/src/api/auth.ts`**

| Function | HTTP | Endpoint |
|---|---|---|
| `register(payload)` | POST | `/auth/register` |
| `login(payload)` | POST | `/auth/login` |
| `getMe()` | GET | `/auth/me` |

**`frontend/src/api/habits.ts`**

| Function | HTTP | Endpoint |
|---|---|---|
| `getHabits(includeArchived?)` | GET | `/habits` |
| `createHabit(payload)` | POST | `/habits` |
| `getHabit(id)` | GET | `/habits/{id}` |
| `updateHabit(id, payload)` | PUT | `/habits/{id}` |
| `deleteHabit(id)` | DELETE | `/habits/{id}` |
| `archiveHabit(id)` | PATCH | `/habits/{id}/archive` |
| `restoreHabit(id)` | PATCH | `/habits/{id}/restore` |
| `completeHabit(id, completionDate)` | POST | `/habits/{id}/completions` |
| `undoCompletion(id, completionDate)` | DELETE | `/habits/{id}/completions/{date}` |
| `getHistory(id)` | GET | `/habits/{id}/history` |
| `getStreak(id)` | GET | `/habits/{id}/streak` |
| `getTree(id)` | GET | `/habits/{id}/tree` |
| `getAnalytics(id)` | GET | `/habits/{id}/analytics` |

---

### 7.4 Auth Context

**`frontend/src/context/AuthContext.tsx`**

The `AuthProvider` component wraps the entire app and manages authentication state.

**State:**
- `user: User | null` — initialized from `localStorage.commit_user` (JSON) on page load
- `loading: boolean` — true while verifying an existing token with `GET /auth/me` on app start

**On mount (useEffect):**
1. If no token in localStorage → `loading = false`, not authenticated.
2. If user already in state → `loading = false` (fast path, no network call).
3. Otherwise: calls `getMe()` to verify the token is still valid. On success, updates state; on failure (expired/invalid token), clears localStorage.

**Actions:**
- `login(payload)`: calls `POST /auth/login`, stores token + user in localStorage, updates state.
- `register(payload)`: calls `POST /auth/register`, same storage logic.
- `logout()`: clears both localStorage keys, sets `user = null` (React Router will redirect to `/login` via `ProtectedRoute`).

**`useAuth()` hook**: Throws if called outside `AuthProvider` (programming error guard).

---

### 7.5 Pages

#### `LoginPage` — `/login`
Email + password form. On submit calls `useAuth().login()`. Shows inline error messages from the API. After success, navigates to `/`. If already logged in, redirects to `/` immediately.

#### `RegisterPage` — `/register`
Display name + email + password form. Client-side validation: password min 8 chars. Calls `useAuth().register()`. After success, navigates to `/`.

#### `Dashboard` — `/`
Main screen. Features:
- Header with date, "+ New" button, avatar circle → `/account`.
- Progress bar showing `completedToday / scheduledToday`.
- Category filter chips (dynamically generated from habits' categories).
- Habit list: habits due today shown at top; habits not due today shown dimmed below.
- Each habit rendered as a `HabitCard`.
- **My Garden** section: visual grid of tree emojis for all habits.
- "Show/hide archived habits" toggle calls `getHabits(true)`.
- Skeleton `SkeletonCard` placeholders while loading.
- Error state with Retry button.

#### `HabitDetail` — `/habits/:id`
Full detail page for one habit. Features:
- Edit link → `/habits/:id/edit`, Archive button (soft deletes).
- Complete today / Undo buttons.
- Stats grid: current streak, longest streak, total completions, completion rate %.
- Next milestone progress bar (towards 7/14/30/60/90 day badges).
- Milestone badge row — earned badges highlighted in gold.
- **ContributionCalendar** (52-week grid).
- **Weekly bar chart** using Recharts `BarChart` (last 16 weeks from `analyticsResponse.weeklyStats`).
- Skeleton loading with full-page shimmer placeholders.
- Error state with Retry.

#### `CreateHabit` — `/habits/new`
Form fields: name (required), description (optional), category (dropdown), schedule type (tab selector: Every day / Specific days / Once a week), day-of-week picker (shown for SPECIFIC_DAYS), start date.

#### `EditHabit` — `/habits/:id/edit`
Same form as CreateHabit but pre-populated from `getHabit(id)`. Submits to `updateHabit`. Shows skeleton while loading the habit.

#### `AccountSettings` — `/account`
Displays current user's email and display name. Includes a "Sign out" button with a confirmation step.

---

### 7.6 Components

#### `ProtectedRoute`
Wraps any page that requires authentication. While `AuthContext.loading = true`, shows a spinner. If `user = null`, renders `<Navigate to="/login" replace />`. Otherwise renders `{children}`.

#### `HabitCard`
Renders a single habit in the Dashboard list. Shows:
- Habit name (link to detail page), category badge, `TreeBadge`.
- Current streak.
- "Not due today" label if the habit is not scheduled today.
- Complete button (if due and not yet done today) or "✅ Done today" label.
- The card dims (reduced opacity) when `scheduledToday = false`.

#### `TreeBadge`
A small pill component showing the tree emoji + stage name. Green border/background when ALIVE; red when DEAD. Accepts a `size` prop (`sm` / `lg`).

#### `ContributionCalendar`
Renders a 52-week × 7-day grid of colored squares, similar to GitHub's contribution graph.
- Each cell is colored: green = completed, light red = missed/scheduled but not done, light gray = not scheduled, dark gray = future date.
- Month labels are rendered above the grid.
- Accepts `completions[]` and an optional `scheduledDates` set.

#### `SkeletonCard`
A shimmer placeholder card (animated gradient). Used in Dashboard and HabitDetail while data is loading. Contains three shimmer blocks mimicking a habit card's layout.

---

## 8. Authentication Flow

```
User visits /                 ProtectedRoute checks AuthContext
                                       │
                           ┌───────────▼────────────┐
                           │  user = null?            │
                           │  loading = true?         │
                           └───────────┬────────────┘
                                       │
               ┌───────────────────────┼─────────────────────────┐
               │                       │                         │
        loading = true           user = null              user set
        show spinner          Navigate to /login         render page
                                       │
                              User fills login form
                                       │
                              POST /api/v1/auth/login
                                       │
                              { token, user } returned
                                       │
                       localStorage.setItem('commit_token', token)
                       localStorage.setItem('commit_user', JSON)
                                       │
                              AuthContext.user set
                                       │
                              Navigate to / (Dashboard)
                                       │
                    Subsequent API calls: axios adds
                    Authorization: Bearer <token> automatically
                                       │
                    Token expires / user logs out:
                    localStorage cleared → redirect /login
```

---

## 9. API Reference

Base URL (local): `http://localhost:8080/api/v1`

OpenAPI/Swagger UI: `http://localhost:8080/swagger-ui.html`

### Auth Endpoints (no token required)

```
POST /auth/register
Body: { "email": "...", "password": "...", "displayName": "..." }
Response 201: { "token": "...", "user": { "id": 1, "email": "...", "displayName": "..." } }

POST /auth/login
Body: { "email": "...", "password": "..." }
Response 200: { "token": "...", "user": { ... } }

GET /auth/me   (requires token)
Response 200: { "id": 1, "email": "...", "displayName": "..." }
```

### Habit Endpoints (all require `Authorization: Bearer <token>`)

```
GET  /habits                          List habits
GET  /habits?includeArchived=true     Include archived
POST /habits                          Create habit
GET  /habits/{id}                     Get habit
PUT  /habits/{id}                     Update habit
DELETE /habits/{id}                   Delete habit (permanent)
PATCH /habits/{id}/archive            Archive (soft delete)
PATCH /habits/{id}/restore            Restore from archive
POST /habits/{id}/completions         Mark complete
DELETE /habits/{id}/completions/{date} Undo completion
GET  /habits/{id}/history             Completion history
GET  /habits/{id}/streak              Streak numbers
GET  /habits/{id}/tree                Tree state/stage
GET  /habits/{id}/analytics           Full analytics
```

### Error Response Format

```json
{
  "timestamp": "2026-08-26T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Habit not found with id: 42"
}
```

---

## 10. Deployment Architecture

### Production

```
GitHub → git push main
  │
  ├── GitHub Actions: backend-ci.yml
  │     Compiles Java, runs 9 tests (H2 in-memory)
  │
  ├── GitHub Actions: frontend-ci.yml
  │     npm ci, npm run build (Vite)
  │
  └── GitHub Actions: frontend-deploy.yml
        Builds with VITE_BASE_PATH=/commit/ and VITE_API_BASE_URL
        Deploys static files to GitHub Pages branch

Render Blueprint (render.yaml):
  ├── commit-db: Managed PostgreSQL (free tier)
  └── commit-backend: Docker web service
        Image built from Dockerfile (eclipse-temurin:17)
        Runs: java -jar app.jar
        DB connection injected via fromDatabase env vars
        JWT_SECRET auto-generated by Render
        Flyway runs migrations on startup
```

### Dockerfile (multi-stage)

**Stage 1 (build)**: `eclipse-temurin:17-jdk-alpine`. Copies `mvnw` + `pom.xml`, downloads dependencies offline, then copies `src/` and builds the fat JAR with `mvnw package -DskipTests`.

**Stage 2 (runtime)**: `eclipse-temurin:17-jre-alpine` (smaller image, no JDK). Copies only the JAR. Exposes port 8080. Entrypoint: `java -jar app.jar`.

---

## 11. CI/CD Pipelines

### `backend-ci.yml`
Triggers on push/PR to `main` when `backend/**` changes.
- Sets up Java 17 (Temurin)
- Runs `mvn -f backend/pom.xml test`
- Tests use H2 in-memory DB; no external services needed

### `frontend-ci.yml`
Triggers on push/PR to `main` when `frontend/**` changes.
- Sets up Node.js 20
- Runs `npm ci && npm run build`

### `frontend-deploy.yml`
Triggers on push to `main` when `frontend/**` changes.
- Builds frontend with `VITE_BASE_PATH=/commit/` (GitHub Pages subdirectory)
- Sets `VITE_API_BASE_URL` from repository variable `VITE_API_BASE_URL`
- Deploys via `actions/upload-pages-artifact` + `actions/deploy-pages`

---

## 12. Environment Variables

### Backend

| Variable | Default | Required in Production | Description |
|---|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5434/commit` | Optional | Full JDBC URL (overrides host/port/name) |
| `DB_HOST` | `localhost` | Yes (Render injects) | PostgreSQL host |
| `DB_PORT` | `5434` | Yes (Render injects) | PostgreSQL port |
| `DB_NAME` | `commit` | Yes (Render injects) | Database name |
| `DB_USERNAME` | `commit` | Yes | Database user |
| `DB_PASSWORD` | — | Yes | Database password |
| `JWT_SECRET` | dev default (≥32 chars) | **Yes** | HMAC signing key — use `openssl rand -base64 48` |
| `JWT_EXPIRATION_DAYS` | `30` | No | Token validity days |
| `CORS_ALLOWED_ORIGINS` | `*` | Yes | Set to GitHub Pages origin in production |

### Frontend (Vite)

| Variable | Used in | Description |
|---|---|---|
| `VITE_API_BASE_URL` | `api/client.ts` | Backend URL in production (e.g. `https://commit-backend.onrender.com`) |
| `VITE_BASE_PATH` | `vite.config.ts`, `App.tsx` | Sub-path for GitHub Pages (e.g. `/commit/`) |
