# Akadem.ia — MVP (Spring Boot + React + Tailwind + Postgres)
Version: 1.2.0

MVP para creación y ejecución de simulacros, estudio con flashcards y generación automática de preguntas desde PDFs con IA. Incluye:
- **Frontend** React + Vite + Tailwind + Flowbite
- **Backend** Spring Boot 3 (Hexagonal/Ports & Adapters)
- **DB** Postgres
- **IA** OpenAI API (embeddings + GPT-4o-mini)
- **Docker Compose** para levantar todo el stack

---

## 🚀 Run rápido
```bash
docker compose up --build
```

Servicios:
- **Frontend**: http://localhost:5173
- **API**: http://localhost:8080
- **Postgres**: `localhost:5432` (DB: `akdemya`, user: `ak_user`, pass: `ak_pass`)

> Nota: el `docker-compose.yml` usa una red **externa** llamada `cluster_network`. Debes crearla antes:
```bash
docker network create cluster_network
```

---

## 🧱 Arquitectura (Backend)
El backend sigue un enfoque **hexagonal**:

```
com.akdemya
├── adapter
│   ├── inbound/web           # Controllers REST
│   ├── infrastructure        # AI (OpenAI), Storage, PDF, Security
│   └── outbound/persistence  # JPA adapters
├── application/service        # Casos de uso (use cases)
├── domain
│   ├── model                  # Entidades del dominio
│   └── port
│       ├── in                 # Puertos de entrada (UseCases)
│       └── out                # Puertos de salida (Repos, Hash, Token)
└── Application.java
```

### Capas principales
- **Controllers** (adapter/inbound/web): Auth, Subjects, Units, Questions, Exams, Flashcards, AI
- **Use Cases** (application/service): `AuthManager`, `ExamManager`, `ContentManagement`, `FlashcardManagementService`, `FlashcardStudyService`, `UserSettingsService`, `IndexDocumentService`, `GenerateQuizService`
- **Infraestructura** (adapter/infrastructure): `OpenAiEmbeddingAdapter`, `OpenAiQuestionGeneratorAdapter`, `PdfBoxTextExtractor`
- **Persistencia** (adapter/outbound/persistence): adapters JPA + repositorios Spring Data
- **Dominio** (domain/model): entidades puras

### Seguridad
- JWT stateless (HS256)
- Filtro `JwtAuthFilter` valida `Authorization: Bearer <token>`
- Endpoints públicos:
  - `/api/auth/**`
  - `/api/subjects/**`
  - `/api/units/**`
  - `/api/questions/**`
- El resto requiere JWT (incluye `/api/settings`, `/api/flashcards`, `/api/exams/**`)
- Endpoints `/api/admin/**`, `/api/sources/**` y `/api/ai/**` requieren rol ADMIN

> ⚠️ **Importante**: la clave JWT está hardcodeada en `JwtService` y debe cambiarse en producción.

---

## 🧩 Funcionalidad (Backend)

### Auth
- Registro y login con JWT
- Hash de passwords con BCrypt

**Endpoints:**
- `POST /api/auth/register` `{ email, password }` → `{ accessToken }`
- `POST /api/auth/login` `{ email, password }` → `{ accessToken }`

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
- `POST /api/flashcards/import?unitId={uuid}&format=csv|json` — importar CSV o JSON como `text/plain` en el body de la petición
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

## 🎨 Frontend (React + Vite + Tailwind)

### Pantallas principales
- **Home**: landing con CTA, stats y hero
- **Subjects**: lista de asignaturas del usuario
- **ExamBuilder**: configuración de simulacro por unidades, modo aleatorio y dificultad (EASY/MEDIUM/HARD)
- **ExamRunner / ExamAttempt**: ejecución del examen con timer
- **ExamResult**: resumen de resultados con puntuación y color por rango
- **Login/Register**: autenticación
- **Flashcards**: dashboard de materias y mazos con cola de estudio global
- **FlashcardsStudy**: estudio de tarjetas con repetición espaciada SM-2
- **FlashcardsHistory**: historial de revisiones con estado vacío y CTA
- **FlashcardsExamine**: detalle de unidad con stats de tarjetas
- **Settings**: gestión de contenido, límites de estudio, import/export de preguntas
- **RAG** *(solo Admin)*: subida de PDFs, indexación y generación de preguntas IA

### Flujo principal
1. Usuario se registra o inicia sesión → se guarda JWT en `localStorage` (`ak_token`)
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

## ⚙️ Configuración

### Backend
- Configuración en `application.properties`
- Variables de entorno relevantes:
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`
  - `SPRING_JPA_HIBERNATE_DDL_AUTO`
  - `SPRING_PROFILES_ACTIVE` (dev/prod)
  - `OPENAI_API_KEY` — requerida para funcionalidad RAG/IA

### Base de datos
- Seeds de desarrollo en `data-dev.sql` (solo en profile `dev`)
- `ddl-auto` por defecto en `create-drop` cuando se usa docker

---

## 🚚 Despliegue mínimo (prod)

1. **Crear red Docker (solo primera vez)**
   ```bash
   docker network create prod_network
   ```

2. **Preparar variables de entorno** (archivo `.env` o variables del sistema)
   ```env
   AKADEMIA_DB_USER=...
   AKADEMIA_DB_PASSWORD=...
   JWT_SECRET=...
   OPENAI_API_KEY=sk-...
   ```

3. **Levantar stack de producción**
   ```bash
   docker compose -f dist/docker-compose-prod.yaml up -d
   ```

4. **Verificar servicios**
   - API: http://localhost:8082
   - Web: http://localhost:5173

> En prod **no** se cargan seeds (SQL init en `never`).

---

## ✅ Notas / TODO
- Cambiar secret JWT en producción
- Añadir validaciones y manejo de errores más granulares
- Añadir paginación en endpoints de contenido
- Añadir tests y CI
- Migrar búsqueda vectorial a **pgvector** en producción (actualmente en memoria)
- Hacer el procesamiento de PDFs **asíncrono** para documentos grandes

---

## 📦 Tech Stack
- Java 21
- Spring Boot 3.3
- Spring Security + JWT
- Apache PDFBox 3 (extracción de texto PDF)
- PostgreSQL 16
- React 18 + Vite + Tailwind CSS
- Flowbite React (componentes UI)
- OpenAI API (embeddings + generación de preguntas)
- Vitest (testing frontend)
- Docker Compose
