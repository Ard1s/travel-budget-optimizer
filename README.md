# ✈️ Travel Budget Optimizer

> AI-powered travel planning with real-time flight price alerts.

A REST API where a user enters a budget, dates and a destination — an LLM builds an
optimal day-by-day itinerary within that budget. A scheduled AWS Lambda watches flight
prices and pushes a notification (via SNS) when a price drops below the user's target.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen)
![Coverage](https://img.shields.io/badge/coverage-~88%25-brightgreen)
![Build](https://img.shields.io/badge/tests-33%20passing-success)

---

## 🚀 Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Security | Spring Security + JWT (jjwt 0.12) |
| Database | PostgreSQL 16 |
| ORM / Migrations | Spring Data JPA (Hibernate) + Flyway |
| AI | Claude API (`claude-sonnet-4-6`) |
| Cloud | AWS Lambda, SNS, CloudWatch, ECS, ECR |
| CI/CD | Azure DevOps → AWS ECR → ECS |
| Build | Maven |
| Containers | Docker (multi-stage) + Docker Compose |
| Testing | JUnit 5 + Mockito + JaCoCo (≥ 80% gate) |
| Docs | OpenAPI / Swagger UI (SpringDoc) |

---

## 📐 Architecture

```
                         ┌─────────────────────────────────────────┐
   HTTP + JWT            │            Spring Boot API               │
 ┌────────────┐          │  Controller → Service → Repository       │
 │  Client /  │ ───────▶ │      │           │            │          │
 │  Swagger   │          │      │      (owner checks)     ▼          │
 └────────────┘          │      │                   ┌───────────┐   │
                         │      │                   │PostgreSQL │   │
                         │      ▼                   │ (Flyway)  │   │
                         │  AiClient ──▶ Claude API  └───────────┘   │
                         └─────────────────────────────────────────┘

        ┌───────────────── independent deployable ─────────────────┐
        │  EventBridge (hourly) ─▶ PriceCheckLambda ─▶ SNS ─▶ email │
        └──────────────────────────────────────────────────────────┘
```

---

## 🏃 Quick Start

### Option A — Full stack with Docker (prod-like, PostgreSQL)
```bash
git clone https://github.com/<username>/travel-budget-optimizer
cd travel-budget-optimizer
cp .env.example .env          # fill JWT_SECRET, AI_API_KEY
docker compose up --build
```
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

### Option B — No Docker (in-memory H2, mocked AI)
Runs without a database or API key — great for a quick look:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local     # Linux/macOS
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"  # Windows PowerShell
```
The `local` profile uses H2 and a `MockAiClient`, so `POST /api/trips/{id}/optimize`
returns a sample plan with no external calls.

---

## ⚙️ Configuration (environment variables)

| Variable | Description | Default (dev) |
|----------|-------------|---------------|
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/travel_budget` |
| `DB_USERNAME` / `DB_PASSWORD` | DB credentials | `postgres` / `postgres` |
| `JWT_SECRET` | HMAC key for JWT (≥ 256 bit) | dev placeholder |
| `AI_API_KEY` | Anthropic API key | — |

Secrets are read from `.env` (git-ignored). Never committed.

---

## 📡 API Endpoints

All `/api/trips/**` require a JWT: `Authorization: Bearer <token>`.

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|:----:|
| POST | `/api/auth/register` | Register, returns JWT | — |
| POST | `/api/auth/login` | Login, returns JWT | — |
| POST | `/api/trips` | Create a trip | ✅ |
| GET | `/api/trips` | List my trips | ✅ |
| GET | `/api/trips/{id}` | Get one trip | ✅ |
| PATCH | `/api/trips/{id}` | Partial update | ✅ |
| DELETE | `/api/trips/{id}` | Delete a trip | ✅ |
| POST | `/api/trips/{id}/optimize` | Run AI itinerary optimization | ✅ |
| GET | `/api/trips/{id}/budget-breakdown` | Budget breakdown by category | ✅ |

Errors use a consistent shape: `{"code": "TRIP_NOT_FOUND", "message": "..."}`.

Users can only access their own trips — ownership is enforced in the service layer
(returns `403 FORBIDDEN` otherwise).

---

## 🧪 Testing

```bash
./mvnw verify          # runs all tests + enforces the JaCoCo 80% line-coverage gate
```
- 29 tests in the API (unit with Mockito + integration with `@SpringBootTest`/MockMvc on H2)
- 4 tests in the Lambda module
- Coverage report: `target/site/jacoco/index.html` (~88% line coverage)

The `AiClient` is an interface, so AI is mocked in tests — no network, no API key needed.

---

## 📦 Project Structure

```
src/main/java/com/travelbudget/
├── controller/   REST controllers (Auth, Trip)
├── service/      business logic (Auth, Trip, TripOptimization, Jwt, UserDetails)
├── repository/   Spring Data JPA repositories
├── entity/       JPA entities + enums
├── dto/          request/response records (never expose entities)
├── client/       AiClient interface + Anthropic + Mock implementations
├── config/       Security, JWT filter, RestClient, OpenAPI, JPA auditing
└── exception/    custom exceptions + GlobalExceptionHandler

src/main/resources/db/migration/   Flyway migrations (V1__ … V4__)
lambda/                            standalone AWS Lambda project (see lambda/AWS_SETUP.md)
```

---

## ☁️ AWS Lambda — price monitoring

An independent Maven project in [`lambda/`](lambda/), deployed separately (no Spring Boot,
minimal cold start). Triggered hourly by EventBridge, it reads active price alerts, checks
current prices, and publishes to SNS on a drop. Build & deploy steps: [`lambda/AWS_SETUP.md`](lambda/AWS_SETUP.md).

---

## 🔄 CI/CD (Azure DevOps → AWS)

`azure-pipelines.yml`, on push to `main`:
1. **Test** — `mvn verify` (tests + JaCoCo coverage gate)
2. **Build** — Docker image → push to AWS ECR (`{BuildId}`, `latest`)
3. **Deploy** — `aws ecs update-service --force-new-deployment`

---

## 🗺️ Status & roadmap

Implemented: auth, trip CRUD, AI optimization, budget breakdown, Dockerized build,
CI/CD pipeline, tests with coverage gate, price-check Lambda skeleton.

Planned / pluggable:
- `GET /api/alerts/active` endpoint + Alerts CRUD (the Lambda consumes it)
- real flight-price provider in the Lambda (`fetchCurrentPrice` is currently a stub)

---

## 📅 Timeline

Solo learning project · 2025–2026
