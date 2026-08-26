# commit.

> **Commit, period. No excuses.**

## Goals

**commit.** is a personal-first habit tracker built around one core idea: every completed habit is a commitment kept.

The application should turn habit consistency into a visible, growing ecosystem. Each habit has a living tree that grows as the streak grows. Missing a scheduled completion can damage or destroy the tree, creating a meaningful consequence without deleting the user's historical progress.

The project has two goals:

1. **Personal use:** help build consistency, reduce procrastination, and make progress visible.
2. **Skill development:** build a real full-stack product while learning backend engineering, frontend development, databases, deployment, CI/CD, authentication, mobile development, and eventually production-grade architecture.

### Core principles

- Start personal and simple.
- Build a real backend rather than a frontend-only prototype.
- Keep the backend independent from the UI.
- Preserve complete historical data even when a streak/tree is reset.
- Every version must be **deployed, usable, and stable** before starting the next.
- Avoid premature complexity.
- Design Version 1 so that later versions can evolve without major rewrites.
- The same backend/API should eventually serve web, iOS, and Android clients.

---

# Timeline

The timeline is intentionally milestone-based rather than calendar-based. A version is complete only when its acceptance criteria are met and it is deployed.

| Version | Milestone | Outcome |
|---|---|---|
| **V1** | Personal MVP | First usable web habit tracker |
| **V2** | Complete Web App | Polished personal web application |
| **V3** | Multi-user Web App | Authentication and friends-ready architecture |
| **V4** | Mobile App | iOS + Android clients using the same backend |
| **V5** | Motivation & Intelligence | Notifications, analytics, achievements, richer habit system |
| **V6** | Social / Product Ready | Sharing, friends, privacy, production hardening |

### Suggested development order

```text
V1 → V2 → V3 → V4 → V5 → V6
```

Every version ends with:

```text
Build
  ↓
Test
  ↓
Deploy
  ↓
Use personally
  ↓
Fix critical issues
  ↓
Version complete
```

---

# Architecture Direction

## V1–V3

```text
                    Internet
                       │
                       ▼
             GitHub Pages / Web
              React + TypeScript
                       │
                    HTTPS
                       │
                       ▼
              Spring Boot REST API
                       │
                       ▼
                  PostgreSQL
```

## V4+

```text
                         Spring Boot
                        REST API + Auth
                              │
             ┌────────────────┼────────────────┐
             │                │                │
         React Web         iOS App        Android App
             │                │                │
             └────────────────┼────────────────┘
                              │
                         PostgreSQL
```

---

# Suggested Tech Stack

## Frontend

- React
- TypeScript
- Vite
- React Router
- CSS / Tailwind CSS or another lightweight styling system
- REST API client using `fetch` or Axios
- Chart/calendar visualization library where useful

## Backend

- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate
- Bean Validation
- Spring Security from the authentication version onward
- REST API
- JUnit + Spring Boot testing

## Database

- PostgreSQL
- Flyway for database migrations

## Development

- Git
- GitHub
- IntelliJ IDEA
- Docker
- Docker Compose
- Postman/Insomnia or equivalent API testing tool

## Deployment

- GitHub for source control
- GitHub Actions for CI/CD
- GitHub Pages for the static web frontend
- Render for the Spring Boot backend initially
- Managed PostgreSQL on Render initially

## Mobile

- React Native
- Expo
- TypeScript
- Same Spring Boot REST API as web

---

# Domain Model Direction

The system should conceptually separate the following:

```text
User
  │
  └── Habit
        │
        ├── Schedule
        ├── Completion History
        ├── Streak
        └── Tree State
```

A habit and its tree are not the same thing.

The database should preserve historical completion records even if a streak is broken and the tree dies.

For example:

```text
Habit: Running

Historical completions:
Day 1  ✓
Day 2  ✓
Day 3  ✓
...
Day 20 ✓
Day 21 ✗

Current state:
Streak = 0
Tree = DEAD
```

The historical record remains available for statistics and future analysis.

---

# Tree / Streak Concept

The tree is a visual representation of habit consistency.

A possible progression:

```text
Seed
  ↓
Small Herb
  ↓
Shrub
  ↓
Sapling
  ↓
Young Tree
  ↓
Tree
  ↓
Flowering Tree
  ↓
Fruit Tree
  ↓
Mature Tree
```

The exact thresholds should remain configurable rather than hard-coded into the UI.

Conceptually:

```text
Current streak
      ↓
Growth rules
      ↓
Tree stage
      ↓
Frontend visualization
```

The backend should store the meaningful state/rules, while the frontend decides how the tree looks.

## Tree failure mechanic

The initial concept:

- A missed scheduled habit can destroy the current tree.
- The consequence/recovery period can increase as the user progresses further.
- The exact recovery rules should be represented as domain logic rather than scattered throughout the frontend.
- Historical completions must never be deleted when the tree dies.

Potential state model:

```text
ALIVE
DEAD
RECOVERING
```

A more sophisticated health/damage system can be introduced in a later version.

---

# Version 1 — Personal MVP Web App

## Goal

Build the smallest genuinely useful version of **commit.**

At the end of V1, the user should be able to open the deployed website, create a habit, complete it every day, see the streak, watch the tree grow, miss a day, and experience the tree reset.

**V1 must be deployed and usable.**

---

## V1 Backend

### Core setup

- Spring Boot project
- REST API
- PostgreSQL database
- Spring Data JPA / Hibernate
- Basic project structure:
  - Controller
  - Service
  - Repository
  - Entity
  - DTO
- Configuration using environment variables
- Database migrations with Flyway

### Initial domain

- Habit
- Habit Completion
- Tree State

For V1, the application is single-user.

Authentication is intentionally excluded.

### Habit capabilities

- Create habit
- Get habits
- Get individual habit
- Update habit
- Deactivate/delete habit
- Define a daily schedule
- Record completion
- Prevent duplicate completion for the same habit/date
- Retrieve completion history

### Streak engine

Backend should determine:

- Current streak
- Longest streak
- Whether today is completed
- Whether a scheduled day was missed
- Whether the tree should continue growing
- Whether the tree should reset

### Tree engine

Backend should determine:

- Current tree stage
- Tree state
- Growth based on streak
- Reset/death state
- Recovery state if the V1 recovery mechanic is included

### API direction

Conceptually:

```text
GET    /api/v1/habits
POST   /api/v1/habits

GET    /api/v1/habits/{id}
PUT    /api/v1/habits/{id}
DELETE /api/v1/habits/{id}

POST   /api/v1/habits/{id}/completions
GET    /api/v1/habits/{id}/history
GET    /api/v1/habits/{id}/streak
GET    /api/v1/habits/{id}/tree
```

The exact API can evolve during development.

### Important technical requirements

- Use `LocalDate` for habit dates where appropriate.
- Define a user timezone conceptually even though V1 is single-user.
- Keep business logic out of controllers.
- Do not expose JPA entities directly as the long-term API contract.
- Use DTOs.
- Validate requests.
- Handle errors consistently.
- Add basic unit/integration tests.

---

## V1 Frontend

### Core screens

#### Dashboard

Display:

- Today's habits
- Completion status
- Current streak
- Tree
- Today's progress

#### Create Habit

Allow:

- Habit name
- Daily schedule
- Start date

#### Habit Details

Display:

- Current streak
- Longest streak
- Tree
- Completion history
- Calendar

### Core interactions

- Create habit
- Complete habit
- Undo completion where appropriate
- View today's status
- View history
- See tree growth
- See tree reset after failure

### UI direction

Keep V1 visually simple.

The important thing is the core loop:

```text
See habit
   ↓
Complete habit
   ↓
Streak increases
   ↓
Tree grows
   ↓
Come back tomorrow
```

---

## V1 Deployment

### Frontend

Deploy React production build to:

**GitHub Pages**

### Backend

Deploy Spring Boot application to:

**Render**

### Database

Use:

**Managed PostgreSQL on Render**

### CI/CD

Initial GitHub Actions:

```text
Push
 ↓
Build frontend
 ↓
Run frontend checks
 ↓
Deploy frontend
```

and:

```text
Push
 ↓
Build backend
 ↓
Run backend tests
 ↓
Deploy backend
```

### V1 acceptance criteria

V1 is complete only when:

- [ ] Website is publicly reachable
- [ ] Backend is publicly reachable
- [ ] Frontend successfully communicates with backend
- [ ] PostgreSQL is persistent
- [ ] A habit can be created
- [ ] A habit can be completed
- [ ] Completion history persists
- [ ] Streak persists
- [ ] Tree grows
- [ ] Missed habit affects tree state
- [ ] Application survives a restart/redeployment
- [ ] Basic automated tests exist
- [ ] The application is actually used personally for a period of time

---

# Version 2 — Complete Personal Web App

## Goal

Turn the MVP into a polished personal product that is pleasant enough to use every day.

V2 should still be single-user.

---

## V2 Backend

### Improve habit model

Introduce:

- Better habit scheduling
- Habit start/end dates
- Active/archived states
- Habit descriptions
- Habit categories
- Habit-specific settings
- User timezone
- More robust streak calculation

### Scheduling

Support:

```text
DAILY
WEEKLY
SPECIFIC DAYS
```

Example:

```text
Running → Every day
Gym → Mon / Wed / Fri
Reading → Tue / Thu / Sat
```

### Tree engine improvements

Introduce:

- Configurable growth stages
- Better recovery rules
- Milestones
- Tree health/state
- Tree history
- Growth events

### Analytics APIs

Support:

- Completion percentage
- Current streak
- Longest streak
- Total completions
- Weekly statistics
- Monthly statistics
- Habit consistency

### Reliability

Add:

- Better exception handling
- Structured logging
- API documentation
- More integration tests
- Database constraints
- Database indexes
- Flyway migration discipline

---

## V2 Frontend

### Dashboard redesign

Potential layout:

```text
                    commit.

       Today's Progress: 4 / 5

     🌳 Running          27 day streak
     🌱 Reading           4 day streak
     🌿 DSA              12 day streak

             [ Today's habits ]

                 My Garden
```

### Features

- GitHub-style contribution calendar
- Habit-specific calendar
- Statistics
- Progress charts
- Longest streak
- Total completions
- Tree animations
- Better tree illustrations
- Habit filtering
- Categories
- Archive habits
- Edit habits
- Better mobile-responsive web UI

### V2 acceptance criteria

- [ ] All V1 functionality remains stable
- [ ] Daily use feels significantly better than V1
- [ ] Multiple schedules work
- [ ] Calendar history works
- [ ] Statistics are accurate
- [ ] Tree progression is polished
- [ ] Mobile browser experience is good
- [ ] Automated tests cover important business logic
- [ ] CI/CD remains functional
- [ ] V2 is deployed
- [ ] V2 is used personally before moving on

---

## V2 Deployment

Same infrastructure:

```text
GitHub
   │
   ├── React → GitHub Pages
   │
   └── Spring Boot → Render
                     │
                 PostgreSQL
```

Introduce Docker for local development if not already done.

---

# Version 3 — Multi-User Web App

## Goal

Transform **commit.** from a personal application into a real multi-user web application.

At the end of V3, friends should be able to create accounts and use their own private habit systems.

---

## V3 Backend

### Authentication

Introduce:

- Spring Security
- User accounts
- Secure password handling if using password authentication
- Session/JWT strategy
- Authentication endpoints
- Authorization
- User-specific data isolation

Potential authentication options:

- Email/password
- Google OAuth
- GitHub OAuth

Start with one clean authentication method rather than implementing everything.

### User model

```text
User
 │
 ├── Habits
 ├── Completions
 ├── Tree States
 ├── Preferences
 └── Statistics
```

Every habit must belong to a user.

### Security requirements

- Users can only access their own habits
- Users cannot manipulate another user's data
- Validate ownership at the service/API layer
- Protect sensitive endpoints
- Secure secrets through deployment environment variables

### User settings

Support:

- Timezone
- Preferred reminder time
- Display preferences
- Account settings

---

## V3 Frontend

### Authentication UI

- Login
- Signup
- Logout
- Account settings
- Authentication persistence

### User dashboard

Each user sees only their own:

- Habits
- Garden
- Calendar
- Statistics
- Settings

### UX improvements

- Loading states
- Error states
- Empty states
- Offline/error recovery
- Better form validation
- Responsive design

---

## V3 Deployment

Production architecture:

```text
                     Users
                       │
                       ▼
                  React Web
                 GitHub Pages
                       │
                      HTTPS
                       │
                       ▼
                Spring Boot API
                    Render
                       │
                       ▼
                  PostgreSQL
```

Introduce:

- Production environment variables
- Separate development/production configuration
- CORS configuration
- Proper authentication configuration
- Monitoring/logging
- Backup strategy

### V3 acceptance criteria

- [ ] Multiple users can register
- [ ] Users can log in/out
- [ ] User data is isolated
- [ ] Authentication is secure
- [ ] Existing personal data can be migrated to the new user model
- [ ] Friends can independently use the app
- [ ] Production deployment works
- [ ] Database backups are understood/configured
- [ ] V3 is publicly deployed and ready to use

---

# Version 4 — Mobile App

## Goal

Build iOS and Android applications using the same backend.

The backend should not need to be rewritten.

---

## V4 Backend

The existing Spring Boot API remains the central backend.

Add mobile-oriented capabilities where necessary:

- Device registration
- Push notification infrastructure
- Notification preferences
- Mobile-specific authentication handling if necessary
- API performance improvements
- Better API versioning
- Rate limiting where appropriate

### Notification concepts

Examples:

```text
Your tree needs watering 🌱
```

```text
Don't break your 23-day streak.
```

```text
Your Running tree reached a new stage 🌳
```

---

## V4 Frontend — Mobile

Technology:

- React Native
- Expo
- TypeScript

Build:

- iOS app
- Android app

### Reuse

The mobile applications reuse:

- Backend
- REST API
- Domain model
- Authentication
- Database
- Habit data

The mobile UI is independent.

### Mobile features

- Login
- Dashboard
- Today's habits
- Complete habit
- Tree view
- Calendar
- Statistics
- Habit creation/editing
- Notifications
- Settings

### Mobile UX

Add:

- Native navigation
- Gestures
- Haptic feedback where appropriate
- Push notifications
- Offline-aware UI
- Local caching
- App lifecycle handling

---

## V4 Deployment

### Web

GitHub Pages

### Backend

Render

### Database

Managed PostgreSQL

### Mobile

- Apple App Store
- Google Play Store

### CI/CD

Eventually:

```text
GitHub
  │
  ├── Web → GitHub Pages
  │
  ├── Backend → Render
  │
  └── Mobile → App build/release pipeline
```

### V4 acceptance criteria

- [ ] iOS app works
- [ ] Android app works
- [ ] Both use production backend
- [ ] Authentication works
- [ ] Habit completion synchronizes across devices
- [ ] Tree state synchronizes across devices
- [ ] Notifications work
- [ ] Web and mobile show consistent data
- [ ] App builds can be reproduced
- [ ] Apps are released or ready for release
- [ ] V4 is fully usable

---

# Version 5 — Motivation, Analytics & Intelligence

## Goal

Make **commit.** substantially more motivating and useful rather than simply adding complexity.

---

## V5 Backend

### Advanced analytics

Track:

- Completion rate
- Best days
- Weak days
- Habit consistency
- Streak history
- Recovery history
- Monthly trends
- Habit correlations

Potential future insights:

```text
You complete Running 31% more often on days
when you complete your morning routine.
```

### Achievements

Examples:

```text
First Commitment
7 Day Streak
30 Day Streak
100 Completions
First Tree
First Mature Tree
Tree Survivor
```

### Goals

Allow:

- Weekly targets
- Monthly targets
- Completion goals
- Habit milestones

### Reminder engine

Introduce scheduled jobs for:

- Habit reminders
- Streak warnings
- Milestone notifications
- Recovery notifications

Use background scheduling carefully rather than making the core habit state dependent on a scheduler.

---

## V5 Frontend

### Garden

Multiple habits become a personal garden:

```text
                  MY GARDEN

       🌳 Running       🌸 Reading

       🌲 DSA           🌱 Meditation

       🌿 Workout       🌳 Sleep
```

### Enhanced visualization

- Tree growth animations
- Seasonal changes
- Flowers
- Fruits
- Mature trees
- Garden progression
- Habit-specific visual identity

### Analytics dashboard

- Weekly overview
- Monthly overview
- Habit comparison
- Streak history
- Completion heatmap
- Personal records

### Motivation

- Milestone celebrations
- Achievement system
- Progress indicators
- Positive recovery messaging
- Personal records

Avoid making the product excessively punitive.

---

## V5 Deployment

Same production infrastructure, with additional scheduled/background infrastructure if required.

Potentially introduce:

- Redis only if a real need emerges
- Background job infrastructure if notification volume requires it
- Monitoring/observability
- Error tracking

Do **not** add infrastructure simply because it is popular.

### V5 acceptance criteria

- [ ] Analytics are accurate
- [ ] Achievements work
- [ ] Notifications work reliably
- [ ] Garden system works
- [ ] User preferences work
- [ ] Performance remains acceptable
- [ ] New infrastructure has a demonstrated purpose
- [ ] V5 is deployed and usable

---

# Version 6 — Social & Product-Ready

## Goal

If the application proves useful personally and friends enjoy it, turn it into a more complete product.

---

## V6 Backend

Potential features:

### Social relationships

```text
User
 │
 ├── Friends
 ├── Followers
 └── Shared achievements
```

### Privacy

Users decide:

- Private habits
- Public profile
- Shared achievements
- Friends-only statistics
- Public garden
- Hidden habits

### Social interactions

Potentially:

- Friends
- Encouragement
- Reactions
- Shared milestones
- Challenges
- Group habits

### Challenges

Example:

```text
30 Day Reading Challenge
```

```text
Run 20 times this month
```

```text
7 Day No-Skip Challenge
```

Avoid public leaderboards by default if they make the product unnecessarily competitive.

---

## V6 Frontend

Potential screens:

```text
Home
Habits
Garden
Calendar
Analytics
Achievements
Friends
Challenges
Profile
Settings
```

### Social UX

- Friend discovery
- Friend profiles
- Shared achievements
- Challenges
- Celebration animations
- Privacy controls

---

## V6 Deployment

Move toward production-grade infrastructure only if usage justifies it.

Potential improvements:

- Dedicated production database strategy
- CDN
- Better monitoring
- Error tracking
- Automated backups
- Disaster recovery
- Security audits
- Rate limiting
- API documentation
- Load testing
- Performance monitoring

### V6 acceptance criteria

- [ ] Social features are privacy-aware
- [ ] User data remains isolated
- [ ] Challenges work
- [ ] Privacy controls work
- [ ] Mobile and web remain synchronized
- [ ] Production monitoring exists
- [ ] Backup/recovery process is understood
- [ ] Application is ready for broader sharing

---

# Version Summary

| Version | Web | Backend | Database | Auth | Mobile | Main Outcome |
|---|---|---|---|---|---|---|
| **V1** | React + TS | Spring Boot REST | PostgreSQL | No | No | Personal MVP |
| **V2** | Polished React | Improved Spring Boot | PostgreSQL | No | No | Complete personal web app |
| **V3** | Multi-user React | Spring Security + REST | PostgreSQL | Yes | No | Public multi-user web app |
| **V4** | React Web | Shared API | PostgreSQL | Yes | React Native + Expo | iOS + Android |
| **V5** | Web + Mobile | Analytics + jobs | PostgreSQL | Yes | Yes | Motivation/intelligence |
| **V6** | Web + Mobile | Social/product infrastructure | PostgreSQL | Yes | Yes | Product-ready platform |

---

# Deployment Philosophy

Every version must end in a usable deployment.

There should never be a situation where:

```text
V2 = "80% complete, but not deployed"
```

Instead:

```text
V1
 ↓
Build
 ↓
Test
 ↓
Deploy
 ↓
Use
 ↓
Learn
 ↓
V2
```

Each release becomes a stable foundation for the next.

---

# What Not To Build Too Early

Avoid premature infrastructure and features such as:

- Microservices
- Kubernetes
- Kafka
- GraphQL
- Redis without a demonstrated requirement
- Multiple databases
- AI features before the core product works
- Complex social systems
- Elaborate authentication systems in V1
- Native Swift/Kotlin applications before the web architecture is proven

The preferred architecture should remain simple until complexity is justified.

```text
React
   ↓
Spring Boot
   ↓
PostgreSQL
```

Complexity should be earned by actual requirements.

---

# Long-Term Vision

The core loop should always remain:

```text
Commit
  ↓
Complete
  ↓
Streak
  ↓
Growth
  ↓
Visible progress
  ↓
Come back tomorrow
  ↓
Commit again
```

The garden is a representation of the user's consistency.

The application should make the user think:

> **"I don't want to break what I've built."**

But when a streak is broken, the product should also communicate:

> **"You didn't lose everything. Your history is still here. Start growing again."**

---

# Final Target Architecture

```text
                                commit.
                                   │
             ┌─────────────────────┼─────────────────────┐
             │                     │                     │
          Web App              iOS App             Android App
       React + TypeScript    React Native/Expo    React Native/Expo
             │                     │                     │
             └─────────────────────┼─────────────────────┘
                                   │
                                  HTTPS
                                   │
                                   ▼
                          Spring Boot REST API
                                   │
                    ┌──────────────┼──────────────┐
                    │              │              │
                Auth/Security   Habit Engine   Analytics
                    │              │              │
                    └──────────────┼──────────────┘
                                   │
                                   ▼
                              PostgreSQL
                                   │
                                   ▼
                             Persistent Data
```

## The ultimate progression

```text
V1
Personal habit tracker
        ↓
V2
Polished personal web app
        ↓
V3
Multi-user web application
        ↓
V4
Web + iOS + Android
        ↓
V5
Motivation + analytics + notifications
        ↓
V6
Social + product-ready platform
```

**commit.**

*Commit, period. No excuses.*
