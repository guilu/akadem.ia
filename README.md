# Akdemya — MVP (Spring Boot + React + Tailwind + Postgres)

MVP para creación y ejecución de simulacros. Incluye:
- **Frontend** React + Vite + Tailwind
- **Backend** Spring Boot 3 (Hexagonal/Ports & Adapters)
- **DB** Postgres
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
- **Controllers** (adapter/inbound/web): Auth, Subjects, Units, Questions, Exams
- **Use Cases** (application/service): `AuthManager`, `ExamManager`, `ContentManagement`
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
- El resto requiere JWT

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
- Start exam con selección de unidades y tiempo
- Submit con selección de respuestas

**Endpoints:**
- `POST /api/exams/attempts/start`
  ```json
  { "unitCounts": {"<unitId>": 5}, "minutes": 20 }
  ```
  Respuesta:
  ```json
  { "attemptId": "...", "totalTimeSeconds": 1200, "questions": [ ... ] }
  ```

- `POST /api/exams/attempts/{attemptId}/submit`
  ```json
  { "selections": {"<questionId>": "<answerId>"} }
  ```
  Respuesta:
  ```json
  { "total": 20, "correct": 14, "percentage": 70.0 }
  ```

---

## 🎨 Frontend (React + Vite + Tailwind)

### Pantallas principales
- **Home**: lista de asignaturas
- **ExamBuilder**: configuración de simulacro por unidades
- **ExamRunner**: ejecución del examen con timer
- **Login/Register**: autenticación
- **Result**: resumen de resultados

### Flujo
1. Usuario se registra o inicia sesión → se guarda JWT en `localStorage` (`ak_token`)
2. Selecciona una asignatura y configura el simulacro
3. Inicia examen → backend genera preguntas
4. Responde y envía resultados

### Archivos clave
- `src/App.tsx` → flujo principal
- `src/components/Login.tsx`
- `src/components/ExamBuilder.tsx`
- `src/components/ExamRunner.tsx`

---

## ⚙️ Configuración

### Backend
- Configuración en `application.properties`
- Variables de entorno relevantes:
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`
  - `SPRING_JPA_HIBERNATE_DDL_AUTO`

### Base de datos
- `data.sql` se carga en arranque
- `ddl-auto` por defecto en `create-drop` cuando se usa docker

---

## ✅ Notas / TODO
- Cambiar secret JWT en producción
- Añadir validaciones y manejo de errores
- Añadir paginación y filtros en contenido
- Añadir roles/admin para gestión avanzada
- Añadir tests y CI

---

## 📦 Tech Stack
- Java 21
- Spring Boot 3.3
- Spring Security + JWT
- PostgreSQL 18
- React 18 + Vite + Tailwind
- Docker Compose
