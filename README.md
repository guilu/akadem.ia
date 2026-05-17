<p align="center">
  <img src="docs/assets/akadem-ia-header.png" alt="Akadem.ia — Flashcards. Simulacros. Tu plaza." width="100%">
</p>
<p align="center">
  <img src="docs/images/home-light.png" alt="Akadem.ia home (light)" width="49%">
  <img src="docs/images/home-dark.png" alt="Akadem.ia home (dark)" width="49%">
</p>
<p align="center">
  AI-assisted exam preparation platform with timed exams, spaced repetition flashcards and AI-generated quizzes from PDFs.
</p>

<p align="center">
  <a href="https://akademia.diegobarrioh.dev">▶️ Live Demo</a>
  •
  <a href="https://github.com/guilu/akadem.ia">📦 Repository</a>
</p>

<p align="center">
  <a href="https://github.com/guilu/akadem.ia/stargazers"><img src="https://img.shields.io/github/stars/guilu/akadem.ia?style=flat&color=yellow" alt="Stars"></a>
  <a href="https://github.com/guilu/akadem.ia/commits/main"><img src="https://img.shields.io/github/last-commit/guilu/akadem.ia?color=blue" alt="Last commit"></a>
  <img src="https://img.shields.io/badge/java-21-red" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F" alt="Spring Boot">
  <img src="https://img.shields.io/badge/react-18-61DAFB" alt="React 18">
  <img src="https://img.shields.io/badge/vite-5-646CFF" alt="Vite 5">
</p>

---

## ✨ Features

- 📝 Timed exams and mock tests
- 🛒 Course store with stripe integration
- 🧠 SM-2 spaced repetition flashcards
- 🤖 AI-generated quizzes from PDFs (RAG)
- 🔐 JWT + Google OAuth2 authentication
- 📚 Subjects → Units → Questions hierarchy
- 📦 Dockerized full stack
- 📱 Responsive UI
- ⚡ Spring Boot + React + Tailwind

---

## 📸 Screenshots

### Store Page

<p align="center">
  <img src="docs/images/store-1.png" width="49%">
  <img src="docs/images/store-2.png" width="49%">
</p>
<p align="center">
  <img src="docs/images/store-3.png" width="49%">
  <img src="docs/images/store-4.png" width="49%">
</p>

---

### Exam Builder

<p align="center">
  <img src="docs/images/exam-builder.png" width="49%">
  <img src="docs/images/exam-question-session.png" width="49%">
</p>

---

### Exam Session

<p align="center">
  <img src="docs/images/exam-session.png" width="49%">
  <img src="docs/images/exam-result.png" width="49%">
</p>
---

### Flashcards Study

![Flashcards Study](docs/images/flashcards-study.png)

---

### AI PDF Quiz Generation

<p align="center">
  <img src="docs/images/rag-upload.png" width="49%">
  <img src="docs/images/question-generation.png" width="49%">
</p>

---

## 🚀 Live Demo

🔗 <https://akademia.diegobarrioh.dev>

---

## 🏗️ Tech Stack

### Backend

- Java 21
- Spring Boot 3.3
- Spring Security + JWT
- PostgreSQL 16
- Apache PDFBox
- OpenAI API

### Frontend

- React 18
- Vite
- Tailwind CSS
- Flowbite React

### Infrastructure

- Docker Compose
- Nginx
- PostgreSQL

---

## 🚀 Quick Start

```bash
docker compose up --build
```

### Services

| Service | URL |
|---|---|
| Frontend | <http://localhost:5173> |
| API | <http://localhost:8080> |
| Postgres | localhost:5432 |

---

## 🐳 Docker Network

The `docker-compose.yml` uses an external Docker network called `cluster_network`.

Create it before running the stack:

```bash
docker network create cluster_network
```

---

## 🧱 Backend Architecture

The backend follows a **Hexagonal Architecture (Ports & Adapters)** approach.

```text
com.akdemya
├── adapter
│   ├── inbound/web
│   ├── infrastructure
│   └── outbound/persistence
├── application/service
├── domain
│   ├── model
│   └── port
└── Application.java
```

---

## 🔐 Security

- JWT stateless authentication
- Google OAuth2 login
- BCrypt password hashing
- Role-based access control
- Admin-only AI endpoints

---

## 🧠 AI Features

Akadem.ia supports AI-assisted quiz generation from PDF documents:

- PDF upload
- Text extraction with Apache PDFBox
- Semantic chunking
- OpenAI embeddings
- GPT-4o-mini question generation
- Draft approval workflow

---

## 📚 Flashcards System

Built-in flashcards system with:

- SM-2 spaced repetition
- AGAIN / HARD / GOOD / EASY grading
- Study queues
- Import/export
- Review history
- Daily study limits

---

## ⚙️ Environment Variables

```env
JWT_SECRET=
GROQ_API_KEY=
OPENROUTER_API_KEY=
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
FRONTEND_URL=
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
RESEND_API_KEY=re_...
RESEND_FROM_EMAIL=
PRODUCTS_STORAGE_PATH=
```

---

## 🚚 Production Deployment

```bash
docker compose -f dist/docker-compose-prod.yaml up -d
```

---

## ✅ Roadmap

- [ ] pgvector integration
- [ ] Async PDF processing
- [ ] Advanced analytics
- [ ] Multiplayer study sessions
- [ ] Native mobile app
- [ ] AI study assistant

---

## 💖 Apoyar el proyecto

Akadem.ia es un proyecto open source mantenido en mi tiempo libre. Si te resulta útil y quieres ayudar a que siga creciendo:

- ⭐ Dale una estrella al repo — es gratis y ayuda muchísimo a la visibilidad
- 💛 [Conviérteme en sponsor en GitHub](https://github.com/sponsors/guilu) — soporte recurrente
- ☕ [Invítame a un café](https://buymeacoffee.com/diegobarrioh) — donación puntual
- 🐛 Abre issues o PRs con bugs, ideas o mejoras

Cualquier apoyo se traduce directamente en más tiempo para desarrollar features, mejorar la IA y mantener la plataforma online.

---

## 📄 License

MIT
