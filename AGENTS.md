# AGENTS.md — Instrucciones del proyecto

## Formato de commits

Siempre que haya que hacer un commit, el formato es:

```
<gitmoji> <type>(<scope>): <short description>

<detailed body explaining what, why, and any relevant context>
```

**Reglas:**
- Usar un gitmoji relevante al tipo de cambio (ej: ✨ feat, 🐛 fix, 📝 docs, ♻️ refactor, ✅ test, 🔧 chore…)
- Seguir conventional commits: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `perf`, `ci`, etc.
- Incluir siempre un cuerpo detallado con contexto suficiente para entender el cambio

**Ejemplo:**
```
✨ feat(exams): add difficulty filter to exam listing endpoint

Added EASY/MEDIUM/HARD difficulty filter to GET /api/exams.
Previously all exams were returned regardless of difficulty.
This allows the frontend to request only exams matching the
user's current study level.
```

---

# Product Overview

This is a full-stack SaaS platform for exam preparation.

Core features:

- Flashcards with spaced repetition
- Exam simulations
- Progress tracking
- Question management
- Study analytics

Primary goal:

Deliver a fast MVP that can evolve into a scalable SaaS.

---

# Tech Stack

## Backend

- Java 21
- Spring Boot
- Hexagonal Architecture
- PostgreSQL
- Docker

## Frontend

- React
- TypeScript
- TailwindCSS
- Vite

## Infrastructure

- Docker Compose (dev)
- Kubernetes (future)
- Nginx reverse proxy

---

# Architecture Rules

## Backend

Pattern:

DDD + Hexagonal Architecture

Rules:

Domain:
- Pure business logic
- No framework dependencies

Application:
- Use cases
- Commands
- DTO mapping

Infrastructure:
- REST controllers
- Persistence
- External integrations

Never:

- Put business logic in controllers
- Access repositories directly from controllers
- Mix domain and persistence models

---

## Frontend

Architecture:

Feature based structure.

Example:

src/

features/
- exams
- flashcards
- subjects

components/
shared/

pages/

services/

Rules:

- Business logic in hooks or services
- UI components must stay presentational
- API calls centralized in services layer
- Avoid logic inside JSX

Prefer:

Custom hooks:

useExamSession()

instead of:

Logic inside components.

---

# API Integration Rules

Backend is source of truth.

Frontend must:

- Never duplicate business rules
- Validate UX only
- Trust backend validation

API structure:

/api/v1/

Naming:

GET /subjects

POST /exams

GET /progress

Never:

Hardcode URLs inside components.

Always use:

apiClient.ts

---

# Coding Standards

## Backend

Prefer:

records for DTOs

Constructor injection

Value Objects

Avoid:

Field injection

Null values

God services

## Frontend

Prefer:

Functional components

Type safety

Small reusable components

Avoid:

any type

Large components (>300 lines)

Duplicated UI patterns

---

# Testing Strategy

## Backend

Unit tests:

- Domain
- Use cases

Integration:

- Repositories
- Controllers

Tools:

JUnit
Testcontainers

## Frontend

Unit:

- Hooks
- Services

Component:

- Critical UI

Tools:

Vitest
Testing Library

---

# Performance Constraints

API:

P95 < 250ms

Frontend:

First load < 2s

Avoid:

Large bundles

Use:

Code splitting

Lazy loading

---

# UX Rules

UI must be:

Simple
Fast
Readable

Avoid:

Complex flows

Always:

Show progress state
Show loading states
Show error states

Forms must:

Validate instantly.

---

# Security

Always:

Validate backend input
Sanitize data
Use DTO boundaries
Prevent injection

Frontend must:

Never trust local state.

---

# Product Priorities

Order of importance:

1 Working feature
2 Simplicity
3 Maintainability
4 Performance
5 Perfect architecture

Avoid:

Premature optimization.

---

# Development Workflow

When implementing a feature:

1 Understand product goal
2 Design backend contract
3 Implement backend
4 Implement frontend
5 Add tests
6 Update docs

Never:

Start frontend without API contract.

---

# AI Agent Instructions

When generating code:

Prefer:

Simple solutions.

Avoid:

Overengineering.

When modifying code:

Follow existing patterns.

When unsure:

Ask instead of guessing.

Be critical.

Suggest improvements when detected.
