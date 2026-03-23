# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FSAICenter is an AI Gateway / API management platform. It proxies requests to multiple AI providers (OpenAI, Anthropic, Qwen, Doubao, Spark, Wenxin, Ollama, vLLM, etc.), manages API keys, enforces rate limiting and quotas, tracks billing, and provides an admin dashboard. The project is a monorepo with a Java backend and React frontend.

## Development Environment Setup

Start infrastructure services first:
```bash
docker-compose up -d   # PostgreSQL 16 on :5000, Redis 7 on :5001
```

## Build & Run Commands

### Backend (fsaicenter-backend/)
```bash
cd fsaicenter-backend
./mvnw spring-boot:run                    # Run dev server (port 5080)
./mvnw clean package -DskipTests          # Build JAR
./mvnw test                                # Run all tests
./mvnw test -Dtest=ClassName               # Run single test class
./mvnw test -Dtest=ClassName#methodName    # Run single test method
```

### Frontend (fsaicenter-frontend/)
```bash
cd fsaicenter-frontend
npm install           # Install dependencies
npm run dev           # Dev server on port 3000 (proxies /api/* to :5080)
npm run build         # TypeScript check + Vite production build
```

## Architecture

### Backend - DDD Layered Architecture (Spring Boot 3.2.3, Java 17)

The backend follows Domain-Driven Design with four layers under `com.fsa.aicenter`:

- **interfaces/** — HTTP controllers and servlet filters
  - `admin/controller/` — Admin management APIs (users, roles, models, providers, API keys, billing, logs)
  - `api/controller/` — Public AI proxy endpoints: `ChatController`, `EmbeddingsController`, `ImageController`
  - `v1/` — V1 versioned API (in progress)
  - `filter/` — API key authentication filter, rate limit filter

- **application/** — Application services orchestrating domain logic
  - `service/` — `AiProxyService` (core proxy logic), `QuotaManager`, `BillingStatisticsService`, `LogQueryService`, etc.
  - `dto/` — Request/response DTOs
  - `event/` — Domain events (e.g., `RequestLogEvent`)

- **domain/** — Business domain models, organized by bounded context
  - `apikey/` — API key aggregate with quota, rate limit, access control value objects
  - `billing/` — Billing record aggregate with billing rules and cost calculation
  - `model/` — AI model aggregate with model types and configurations
  - `admin/` — Admin user, role, permission aggregates
  - `log/` — Request log aggregate
  - Each context has: `aggregate/`, `repository/` (interfaces), `valueobject/`

- **infrastructure/** — Technical implementations
  - `adapter/` — AI provider adapters implementing a common interface (`AiProviderAdapter`). Each provider (openai, qwen, doubao, spark, wenxin, ollama, vllm, generic) has its own adapter. `AdapterFactory` selects the right adapter. `generic/GenericOpenAiAdapter` handles OpenAI-compatible APIs.
  - `persistence/` — MyBatis Plus mappers, PO entities, repository implementations
  - `event/` — Spring event publishing and async listeners
  - `cache/` — Redis + Caffeine caching
  - `ratelimit/` — Redis-based rate limiting with Lua scripts
  - `auth/` — Sa-Token authentication utilities

- **common/** — Cross-cutting: configs, annotations, AOP aspects, exception handling

### Frontend - React SPA (Vite + TypeScript)

- **UI Components**: Shadcn/ui pattern (Radix UI primitives + Tailwind CSS)
- **State Management**: Zustand with localStorage persistence (auth store uses `satoken` key)
- **Routing**: React Router v6 with `ProtectedRoute` wrapper
- **API Layer**: Axios with token interceptor (auto-attaches Sa-Token header, handles 401 logout)
- **Pages**: LoginPage, DashboardPage, ModelsPage, ProvidersPage, ApiKeysPage, BillingPage, LogsPage, UsersPage, RolesPage
- **Path alias**: `@/` maps to `src/`

### Key Data Flow: AI Proxy Request

1. Request arrives at `ChatController` (or Embeddings/Image controller)
2. API key authentication filter validates the key and checks access permissions
3. Rate limit filter enforces per-key rate limits
4. `AiProxyService` resolects provider, resolves model config, picks adapter via `AdapterFactory`
5. Adapter translates request to provider-specific format and calls the upstream API
6. Streaming responses use SSE (Server-Sent Events) via WebFlux/Reactor `Flux`
7. `RequestLogEvent` is published asynchronously for logging and billing

## Tech Stack Quick Reference

| Layer | Technology |
|-------|-----------|
| Auth | Sa-Token 1.37.0 (JWT/UUID, Redis-backed sessions) |
| ORM | MyBatis Plus 3.5.7 |
| Database | PostgreSQL 16 (Flyway migrations in `resources/db/migration/`) |
| Cache | Redis 7 + Caffeine |
| HTTP Client | OkHttp3 |
| Reactive | Spring WebFlux / Reactor |
| API Docs | Knife4j (Swagger, dev only, port 5080) |
| Frontend Build | Vite 6 |
| Charts | Recharts |

## Database

- Flyway migrations live in `fsaicenter-backend/src/main/resources/db/migration/`
- Migration naming: `V{N}__{description}.sql`
- Dev database: `fsaicenter_dev` on localhost:5000 (postgres/postgres)
- Key tables: `api_key`, `ai_model`, `ai_provider`, `model_api_key`, `request_log`, `request_log_detail`, `billing_record`, `admin_user`, `admin_role`, `admin_permission`, `operation_log`

## Configuration Profiles

- `application.yml` — Base config (server port 8080, shared settings)
- `application-dev.yml` — Dev overrides (port 5080, local DB/Redis, DEBUG logging, Knife4j enabled)
- `application-prod.yml` — Production (env vars for all secrets, SSL, WARN logging, Knife4j disabled)
- Local overrides: `application-local.yml` (gitignored)

## Language

This project uses Chinese for commit messages, code comments, and documentation. Respond in Chinese when communicating about this project.
