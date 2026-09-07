# 🏋️ BONWO — MVP

## 🎯 Product Overview

Bonwo is a fitness training application. Users can create their own exercises (with a muscle map, level, equipment and instructions), combine them into routines, organize those routines into multi-week training programs, and log real training sessions from a routine (set by set, with weights and reps). It also has a social layer: users can publish their exercises or routines to a public feed, like or save them, and explore what others have published. A shared catalog (equipment, activities, training goals, muscle groups and sub-groups) is managed by admin users and consumed by everyone when creating content.

## 🛠️ Tech Stack

**Backend**
- ☕ Java 21 + Spring Boot 3.5
- Spring Web, Spring Security (JWT), Spring Data JPA / Hibernate
- 🐘 PostgreSQL + Flyway (migrations)
- Maven
- 📖 SpringDoc OpenAPI (Swagger UI)
- ☁️ Cloudinary (image & video storage)
- ✅ JUnit 5, Mockito, AssertJ, Testcontainers

**Frontend**
- ⚛️ React 18 + TypeScript + Vite
- TanStack Query (server state / cache)
- React Router
- React Hook Form + Zod (forms & validation)
- 🎨 Tailwind CSS + Radix UI (components)
- Axios
- dnd-kit (drag & drop — e.g. reordering exercises in a routine)

**Infrastructure**
- 🐳 Docker / Docker Compose

## 🚀 Installation & Setup

### Requirements
- Java 21
- Node.js 20+
- Docker (for the database, or to run the whole stack — see below)
- A [Cloudinary](https://cloudinary.com/) account (free tier is enough) for image/video uploads

### 1. Environment variables

Copy `.env.example` to `.env` in the project root and fill in the values (see full breakdown below):

```
cp .env.example .env
```

### 2. Database

With Docker running, start Postgres only:

```
docker compose up -d postgres
```

### 3. Backend

```
source .env
./mvnw spring-boot:run
```

> ⚠️ `.env` isn't loaded automatically by `mvnw` — you need to `source .env` (or export the variables another way) before starting the backend, otherwise the Cloudinary/JWT configuration ends up empty and fails silently.

The backend listens on `http://localhost:8080`, with every endpoint under the `/api/v1` prefix.

### 4. Frontend

```
cd frontend
npm install
npm run dev
```

The frontend runs on `http://localhost:5173`, with a dev proxy (`vite.config.ts`) forwarding `/api/*` to the backend on `localhost:8080`.

## 🐳 Running with Docker

Besides using Docker just for the database (previous step), the project has a `docker-compose.yml` that spins up **the entire stack** (Postgres + backend + frontend) without needing Java or Node installed locally:

```
docker compose up --build
```

This builds the images (`Dockerfile` at the root for the backend, `frontend/Dockerfile` for the frontend, served with nginx) and starts:

| Service    | Local port | Description                                  |
|------------|-----------|------------------------------------------------|
| `postgres` | `5432`    | Database                                        |
| `backend`  | `8080`    | Spring Boot API                                 |
| `frontend` | `3000`    | Production frontend build, served by nginx      |

The backend container reads its environment variables from the same root `.env` file. To stop everything: `docker compose down`.

To start only one specific service (e.g. just the database, to develop the rest locally), pass its name:

```
docker compose up -d postgres
```

## 🔐 Environment Variables

All documented in `.env.example`:

| Variable | Description | Required |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Postgres connection | Yes |
| `JWT_SECRET` | Secret used to sign JWTs (min. 32 bytes) | Yes — change the default value outside local dev |
| `JWT_ACCESS_EXPIRATION_MS` | Access token expiration (ms) | No (default 15 min) |
| `JWT_REFRESH_EXPIRATION_MS` | Refresh token expiration (ms) | No (default 30 days) |
| `MEDIA_PENDING_TTL_MINUTES` | Minutes an upload stays "pending" before expiring if unused | No (default 15) |
| `MEDIA_ORPHAN_GRACE_HOURS` | Grace period before cleaning up orphaned images | No (default 24) |
| `MEDIA_ORPHAN_SWEEP_INTERVAL_HOURS` | How often the orphan-cleanup job runs | No (default 24) |
| `ADMIN_EMAIL`, `ADMIN_USERNAME`, `ADMIN_PASSWORD` | If all three are set, an admin user is seeded on startup (skipped if it already exists) | No |
| `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | Cloudinary credentials | Yes |

## 🗄️ Database Migrations

Managed with **Flyway**, located in `src/main/resources/db/migration/` (`V1__init_schema.sql` through `V9__training_sessions_table.sql`, roughly one per bounded context as the model was built out).

They're applied **automatically when the backend starts** — no separate command needed. Spring Boot validates the schema state against the migration history and applies whatever's missing (`spring.flyway.baseline-on-migrate: true` in `application.yaml`).

## 🏗️ Architecture

The backend follows a **hexagonal architecture (ports & adapters)**, with one module per business domain: `user`, `catalog`, `exercise` (with a `publication` sub-package for the social layer), `routine`, `program`, `session`, `media`, and `shared` for cross-cutting concerns (security, global exception handling).

Every module follows the same internal structure:

```
<module>/
  domain/            domain model, in/out ports (interfaces), domain exceptions
  application/        use cases (services), DTOs, mappers
  infrastructure/
    rest/              controllers
    persistence/        JPA entities, repositories, adapters
```

`domain/port/out` interfaces are what the domain layer needs (e.g. a repository); `infrastructure/persistence` implements them. This keeps the domain free of Spring/JPA dependencies and makes it easy to unit test business logic with mocks.

The frontend is a React SPA consuming the API via Axios/TanStack Query, organized by feature (one folder per domain: `exercise/`, `routine/`, `program/`, `session/`, `library/`, `admin/`, etc.).

## 🔑 Authentication & Roles

- **Stateless JWT** with an access token + refresh token pair. The access token (`Authorization: Bearer <token>`) is validated on every request via a filter (`JwtAuthenticationFilter`); the refresh token is single-use — refreshing rotates it and revokes the old one (`POST /auth/refresh`).
- Passwords are stored hashed (BCrypt via Spring Security).
- Two roles: **`USER`** (default on signup) and **`ADMIN`**. The first admin can be seeded via environment variables (`ADMIN_EMAIL`/`ADMIN_USERNAME`/`ADMIN_PASSWORD`) or promoted later via `PATCH /admin/users/{id}/role`.
- Account states: `ACTIVE` and `BANNED` (blocks login and refresh, doesn't delete data). Deleting an account is permanent and irreversible — it removes the user row entirely, not just its status (see `DELETE /users/me` / `DELETE /admin/users/{id}` below).
- Two authorization layers: admin routes (`/admin/**`, catalog mutations) restricted by role via `@PreAuthorize("hasRole('ADMIN')")`; ownership of personal resources (e.g. "this routine isn't yours") checked at the service layer, returning 403.

## 📖 Swagger / OpenAPI Docs

With the backend running locally:

- **Swagger UI:** http://localhost:8080/api/v1/swagger-ui/index.html
- **OpenAPI JSON:** http://localhost:8080/api/v1/v3/api-docs

(The path includes `/api/v1` because that's the backend's configured `context-path`.)

## 🧪 Test Suites

The backend has **unit and integration test suites**, run together via Maven Surefire:

- **Unit tests**: per application service and domain model, using Mockito to isolate dependencies (`*ServiceTest`, domain model tests).
- **Integration tests**: against a real Postgres instance spun up with **Testcontainers** — controller tests (`@WebMvcTest`, mocking only the use case) and repository/specification tests (`@DataJpaTest`, suffixed `*IT`, e.g. `ExerciseRepositoryAdapterIT`), the latter validating real filtering queries (JPA Criteria/Specifications) against the database.

To run them:

```
./mvnw test
```

(Requires Docker running — Testcontainers spins up an ephemeral Postgres container for the integration tests.)

The frontend doesn't have an automated test suite yet.

## 🔀 Workflow (Issues, Branches, Pull Requests)

Development was organized as **one branch per task**, prefixed by change type.

Work is planned on the **User Stories** board (GitHub Projects, link below), and each branch is merged into `dev` once the feature is complete (tested), and finally merged into `main` once the project is ready.

## ✅ Implemented Features

- 🔑 Authentication (register, login, token refresh/rotation, logout), own-profile management and self-service account deletion
- 🛡️ Admin panel: user management (ban/unban, role change, permanent account deletion) and catalog management (equipment, activities, training goals, muscle groups/sub-groups)
- 🏋️ Full CRUD for personal exercises, with a muscle map (primary/secondary/stabilizer activation per sub-group), video and image
- 🔍 Filtering exercises/routines by muscle sub-group (primary activation only), equipment, activity, goal and title
- 📢 Publishing exercises to the public feed, with likes and saves
- 📋 Full CRUD for routines (exercises organized into sets, with drag & drop reordering) and training programs (routines organized by day)
- ⏱️ Training sessions: start a session from a routine, log real sets (reps/weight/time), complete or delete the session
- 🖼️ Image and video upload (Cloudinary) with automatic cleanup of expired pending uploads and orphaned files

## 📋 User Stories

Planning & tracking board: https://github.com/users/AlePesole/projects/5

## 🌐 Endpoints

Common prefix: `/api/v1`. Endpoints marked **Admin** require the `ADMIN` role; the rest of the non-public ones require authentication (`Authorization: Bearer <accessToken>`).

### 🔑 `/auth` — Authentication (public)
| Method | Route | Description |
|---|---|---|
| POST | `/auth/register` | Registers a new user |
| POST | `/auth/login` | Logs in, returns an access + refresh token |
| POST | `/auth/refresh` | Rotates the refresh token and returns a new pair |
| POST | `/auth/logout` | Revokes the refresh token |

### 👤 `/users` — User profile
| Method | Route | Description |
|---|---|---|
| GET | `/users/me` | Full profile of the authenticated user |
| GET | `/users/{username}` | Another user's public profile |
| PATCH | `/users/me` | Updates your own profile |
| DELETE | `/users/me` | Permanently deletes your own account and everything you created |

### 🛡️ `/admin/users` — User management (Admin)
| Method | Route | Description |
|---|---|---|
| GET | `/admin/users` | Lists users (paginated) |
| GET | `/admin/users/{userId}` | User details |
| PATCH | `/admin/users/{userId}` | Updates a user's data |
| POST | `/admin/users/{userId}/ban` | Bans the user |
| POST | `/admin/users/{userId}/unban` | Removes the ban |
| DELETE | `/admin/users/{userId}` | Permanently deletes the user and everything they created |
| PATCH | `/admin/users/{userId}/role` | Changes the role (USER/ADMIN) |

### 📦 `/catalog` — Shared catalog (public read, Admin write)
| Method | Route | Description |
|---|---|---|
| GET | `/catalog/equipment` | Lists equipment |
| POST/PUT/DELETE | `/catalog/equipment[/{id}]` | Creates/edits/deletes equipment — **Admin** |
| GET | `/catalog/activities` | Lists activities |
| POST/PUT/DELETE | `/catalog/activities[/{id}]` | Creates/edits/deletes activities — **Admin** |
| GET | `/catalog/training-goals` | Lists training goals |
| POST/PUT/DELETE | `/catalog/training-goals[/{id}]` | Creates/edits/deletes goals — **Admin** |

### 💪 `/catalog/muscles` — Muscle groups (public read, Admin write)
| Method | Route | Description |
|---|---|---|
| GET | `/catalog/muscles` | Lists muscle groups (with their sub-groups) |
| POST/PUT/DELETE | `/catalog/muscles[/{id}]` | Creates/edits/deletes a group — **Admin** |
| POST/PUT/DELETE | `/catalog/muscles/sub-groups[/{id}]` | Creates/edits/deletes a sub-group — **Admin** |

### 🏋️ `/exercises` — Personal exercises
| Method | Route | Description |
|---|---|---|
| GET | `/exercises` | Lists my exercises (filters: muscle, equipment, activity, goal, title) |
| GET | `/exercises/{id}` | Exercise detail |
| POST | `/exercises` | Creates an exercise |
| PUT | `/exercises/{id}` | Edits an exercise |
| DELETE | `/exercises/{id}` | Deletes an exercise |

### 📢 `/exercise-publications` — Public exercise feed
| Method | Route | Description |
|---|---|---|
| POST | `/exercise-publications` | Publishes an owned exercise |
| GET | `/exercise-publications` | Browses the public feed |
| GET | `/exercise-publications/mine` | My publications |
| GET | `/exercise-publications/liked` | Publications I liked |
| GET | `/exercise-publications/saved` | Publications I saved |
| GET | `/exercise-publications/{id}` | Publication detail |
| PUT | `/exercise-publications/{id}` | Edits my own publication |
| DELETE | `/exercise-publications/{id}` | Deletes my own publication |
| POST/DELETE | `/exercise-publications/{id}/like` | Likes/unlikes |
| POST/DELETE | `/exercise-publications/{id}/save` | Saves/unsaves |

### 📋 `/routines` — Routines
| Method | Route | Description |
|---|---|---|
| GET | `/routines` | Lists my routines (same filters as exercises) |
| GET | `/routines/{id}` | Routine detail |
| POST | `/routines` | Creates a routine |
| PUT | `/routines/{id}` | Edits a routine |
| DELETE | `/routines/{id}` | Deletes a routine |

### 📅 `/training-programs` — Training programs
| Method | Route | Description |
|---|---|---|
| GET | `/training-programs` | Lists my programs |
| GET | `/training-programs/{id}` | Program detail (with its routines) |
| POST | `/training-programs` | Creates a program |
| PUT | `/training-programs/{id}` | Edits a program |
| DELETE | `/training-programs/{id}` | Deletes a program |

### ⏱️ `/training-sessions` — Training sessions
| Method | Route | Description |
|---|---|---|
| GET | `/training-sessions` | Lists my sessions |
| GET | `/training-sessions/{id}` | Session detail |
| POST | `/training-sessions` | Starts a session from a routine |
| PUT | `/training-sessions/{id}` | Updates session progress (completed sets) |
| POST | `/training-sessions/{id}/complete` | Marks the session as completed |
| DELETE | `/training-sessions/{id}` | Deletes a session |

### 🖼️ `/media` — File uploads
| Method | Route | Description |
|---|---|---|
| POST | `/media/videos/upload` | Uploads a video, returns a temporary `uploadToken` |
| POST | `/media/images/upload` | Uploads an image, returns a temporary `uploadToken` |

The `uploadToken` is then passed as `thumbnailUploadToken`/`mainVideoUploadToken` when creating or editing the resource that will reference it (exercise, routine, program, profile).
