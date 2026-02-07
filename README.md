# Akdemya — MVP (Spring Boot + React + Tailwind + Postgres)

## Run
```bash
docker compose up --build
```
- Frontend: http://localhost:5173
- API: http://localhost:8080
- Postgres: localhost:5432 (akdemya / ak_user / ak_pass)

## Autenticación
- Desde la UI puedes **registrarte** o **iniciar sesión** (se emite JWT).
- El token se guarda en `localStorage` y se usa para iniciar/entregar exámenes.

## Endpoints nuevos
- `POST /api/auth/register` `{ email, password }` → `{ accessToken }`
- `POST /api/auth/login` `{ email, password }` → `{ accessToken }`
- `POST /api/exams/attempts/start` `{ unitCounts:{unitId:count}, minutes }` → `{ attemptId, totalTimeSeconds, questions[] }`
- `POST /api/exams/attempts/{attemptId}/submit` `{ selections:{questionId:answerId} }` → `{ total, correct, percentage }`

> Nota: Seguridad **stateless** con JWT (HS256). Cambia la clave de `JwtService` en producción.
