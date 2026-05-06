# Akadem.ia

<p align="center">
  <img src="docs/images/hero.png" alt="Akadem.ia Hero" width="100%">
</p>

<p align="center">
  AI-assisted exam preparation platform with timed exams, spaced repetition flashcards and AI-generated quizzes from PDFs.
</p>

<p align="center">
  <a href="https://akademia.diegobarrioh.dev">Live Demo</a>
  ·
  <a href="https://github.com/guilu/akadem.ia">Repository</a>
</p>

---

## ✨ Features

- 📝 Timed exams and mock tests
- 🧠 SM-2 spaced repetition flashcards
- 🤖 AI-generated quizzes from PDFs (RAG)
- 🔐 JWT + Google OAuth2 authentication
- 📚 Subjects → Units → Questions hierarchy
- 📦 Dockerized full stack
- 📱 Responsive UI
- ⚡ Spring Boot + React + Tailwind

---

## 📸 Screenshots

### Landing Page

![Landing](docs/images/home-dark.png)

---

### Exam Builder

![Exam Builder](docs/images/exam-builder.png)

---

### Exam Session

![Exam Session](docs/images/exam-session.png)

---

### Flashcards Study

![Flashcards Study](docs/images/flashcards-study.png)

---

### AI PDF Quiz Generation

![AI Resource Upload and Generate Embedings](docs/images/rag-upload.png)

![AI Quiz Generation](docs/images/question-generation.png)

---

## 🚀 Live Demo

🔗 https://akademia.diegobarrioh.dev

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
| Frontend | http://localhost:5173 |
| API | http://localhost:8080 |
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

### Seguridad
- JWT stateless (HS256)
- Filtro `JwtAuthFilter` valida `Authorization: Bearer <token>`
- Autenticación con Google OAuth2 (flujo completo vía `/api/oauth2/authorization/google`)
- Endpoints públicos:
  - `/api/auth/**`
  - `/api/oauth2/**`
  - `/api/subjects/**`
  - `/api/units/**`
  - `/api/questions/**`
- El resto requiere JWT (incluye `/api/settings`, `/api/flashcards`, `/api/exams/**`)
- Endpoints `/api/admin/**`, `/api/sources/**` y `/api/ai/**` requieren rol ADMIN

- JWT stateless authentication
- Google OAuth2 login
- BCrypt password hashing
- Role-based access control
- Admin-only AI endpoints

---

## 🧠 AI Features

### Auth
- Registro y login con JWT
- Hash de passwords con BCrypt
- **Login con Google (OAuth2)**: los usuarios que acceden por primera vez con Google son creados automáticamente con rol STUDENT. La identidad se vincula por email, por lo que si un usuario ya tiene cuenta con contraseña puede continuar usando la misma cuenta.

**Endpoints:**
- `POST /api/auth/register` `{ email, password }` → `{ accessToken }`
- `POST /api/auth/login` `{ email, password }` → `{ accessToken }`
- `GET /api/oauth2/authorization/google` → redirige al flujo OAuth2 de Google

### Contenido
- Subjects → Units → Questions → Answers
- CRUD básico para subjects, units, questions y answers

**Endpoints:**
- `GET /api/subjects`
- `POST /api/subjects`
- `DELETE /api/subjects/{id}`

- `GET /api/units?subjectId={uuid}`
- `POST /api/units`
- `DELETE /api/units/{id}`

- `GET /api/questions?unitId={uuid}`
- `POST /api/questions`
- `DELETE /api/questions/{id}`
- `POST /api/questions/{id}/answers`

### Exámenes (Simulacros)
- Start exam con selección de unidades, tiempo y dificultad (EASY/MEDIUM/HARD)
- Modo aleatorio: start exam con selección de asignatura y número de preguntas
- Submit con selección de respuestas
- Puntuación con penalización por error (configurable)

**Endpoints:**
- `POST /api/exams/attempts/start`
  ```json
  { "unitCounts": {"<unitId>": 5}, "minutes": 20, "difficulty": "MEDIUM" }
  ```
  Respuesta:
  ```json
  { "attemptId": "...", "totalTimeSeconds": 1200, "questions": [ ... ] }
  ```

- `POST /api/exams/attempts/start-random`
  ```json
  { "subjectId": "<uuid>", "count": 20, "minutes": 20, "difficulty": "EASY" }
  ```

- `PUT /api/exams/attempts/{attemptId}/answers/{questionId}`
  ```json
  { "answerId": "<uuid>" }
  ```

- `POST /api/exams/attempts/{attemptId}/submit`
  ```json
  { "selections": {"<questionId>": "<answerId>"} }
  ```
  Respuesta:
  ```json
  { "total": 20, "correct": 14, "percentage": 70.0 }
  ```

### Flashcards
- CRUD de flashcards por unidad
- Estudio con **algoritmo de repetición espaciada SM-2** (grados: AGAIN, HARD, GOOD, EASY)
- Registro de revisiones y progreso detallado
- Import/Export en formato CSV y JSON por unidad (import masivo)
- Learning steps, session counters e interval hints

**Endpoints:**
- `GET /api/flashcards?unitId={uuid}` — listar flashcards
- `POST /api/flashcards` — crear flashcard `{ front, back, unitId }`
- `PUT /api/flashcards/{id}` — actualizar flashcard
- `DELETE /api/flashcards/{id}` — eliminar flashcard
- `POST /api/flashcards/review` — registrar revisión `{ flashcardId, grade }`
- `POST /api/flashcards/import?unitId={uuid}` — importar CSV/JSON (multipart)
- `GET /api/flashcards/export?unitId={uuid}&format=csv|json` — exportar

### Configuración de usuario (Límites de estudio)
- Cada usuario puede configurar sus límites diarios de estudio para flashcards

**Endpoints:**
- `GET /api/settings` — obtener configuración `{ newCardsLimit, reviewCardsLimit }`
- `PUT /api/settings` — actualizar límites `{ newCardsLimit, reviewCardsLimit }`

> Requiere autenticación JWT. Los límites controlan cuántas tarjetas nuevas y de repaso se sirven por sesión.

### Generación IA desde PDF (RAG) — Solo ADMIN
- Subida y procesamiento de documentos PDF
- Extracción de texto con **Apache PDFBox**
- Chunking semántico y generación de **embeddings** con OpenAI
- Generación de preguntas tipo test con **GPT-4o-mini**
- Gestión de borradores de preguntas generadas

**Requiere:** `OPENAI_API_KEY` configurado en entorno.

**Endpoints:**
- `POST /api/sources` — subir PDF (multipart)
- `GET /api/sources` — listar documentos indexados
- `POST /api/ai/quizzes/generate` `{ sourceId, unitId, count }` — generar preguntas
- `GET /api/ai/quizzes/drafts` — listar borradores generados
- `POST /api/ai/quizzes/drafts/{id}/approve` — aprobar borrador como pregunta real

---

## 📚 Flashcards System

Built-in flashcards system with:

### Flujo principal
1. Usuario se registra, inicia sesión con email/contraseña o accede con Google → se guarda JWT en `localStorage` (`ak_token`)
2. Selecciona una asignatura y configura el simulacro (unidades, dificultad, tiempo)
3. Inicia examen → backend genera preguntas aleatorias
4. Responde y envía resultados

### Flujo flashcards
1. Selecciona materia → unidad → mazo de tarjetas
2. Estudia las tarjetas y puntúa cada una (AGAIN/HARD/GOOD/EASY)
3. El algoritmo SM-2 de repetición espaciada prioriza las tarjetas más difíciles
4. Puede importar/exportar tarjetas en CSV o JSON
5. Los límites diarios (nuevas/repaso) son configurables por usuario en Settings

### Archivos clave
- `src/App.tsx` → router principal con todas las rutas
- `src/constants/routes.ts` → constantes de rutas centralizadas (`ROUTES`)
- `src/pages/` → una página por ruta (LoginPage, RegisterPage, ExamBuilderPage, etc.)
- `src/components/ExamBuilder.tsx`, `ExamRunner.tsx` — componentes de examen
- `src/components/flashcards/` — componentes de flashcards (tabs, import modal, tarjetas)
- `src/components/rag/` — `SourceUpload.tsx`, `QuizGenerateForm.tsx`, `DraftList.tsx`
- `src/components/Settings.tsx` — panel de administración y configuración de usuario

---

## ⚙️ Environment Variables

### Backend
- Configuración en `application.properties`
- Variables de entorno relevantes:
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`
  - `SPRING_JPA_HIBERNATE_DDL_AUTO`
  - `SPRING_PROFILES_ACTIVE` (dev/prod)
  - `OPENAI_API_KEY` — requerida para funcionalidad RAG/IA

### Compra digital del Temario (Stripe + Resend)
La ruta pública `/temario/subalterno-gva` permite a un visitante anónimo
comprar el PDF (15 €) y recibir un enlace de descarga por email. Configuración
de webhook Stripe, dominio Resend, ubicación del PDF y variables (`STRIPE_*`,
`RESEND_*`, `PRODUCTS_STORAGE_PATH`, `VITE_STRIPE_PUBLISHABLE_KEY`) en
[`docs/digital-purchases.md`](docs/digital-purchases.md).

### Base de datos
- Seeds de desarrollo en `data-dev.sql` (solo en profile `dev`)
- `ddl-auto` por defecto en `create-drop` cuando se usa docker

---

## 🚚 Production Deployment

```bash
docker compose -f dist/docker-compose-prod.yaml up -d
```

---

## ✅ Notas / TODO
- Cambiar secret JWT en producción
- Añadir validaciones y manejo de errores más granulares
- Añadir paginación en endpoints de contenido
- Migrar búsqueda vectorial a **pgvector** en producción (actualmente en memoria)
- Hacer el procesamiento de PDFs **asíncrono** para documentos grandes

---

## 📄 License

MIT
