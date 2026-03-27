# PROCEDURE.md — Akademia

Procedimiento específico del repo **akdemya-mvp-auth**. Complementa al `PROCEDURE.md` global del workspace.

---
## 1) Contexto del proyecto

### Jira
- Base URL: `https://dbhlab.atlassian.net`
- Board: `https://dbhlab.atlassian.net/jira/software/projects/AKDMIA/boards/1`
- Proyecto: `AKDMIA`

### GitHub
- Repo: `guilu/akadem.ia`
- Reviewer por defecto en PRs abiertas: `guilu` (cuando GitHub lo permita)

### Entorno local
- Dev compose: `/home/diegobarrioh/code/akdemya-mvp-auth/compose.yaml`
- Pro compose: `/home/diegobarrioh/code/akdemya-mvp-auth/dist/compose.yaml`
- Pro env: `/home/diegobarrioh/code/akdemya-mvp-auth/dist/.env`
- Dev URL: `http://localhost:3000`
- API local habitual: `http://localhost:8080`
- Pro URL actual: `http://localhost:5173`

---
## 2) Reglas específicas de Akademia

- Las PR abiertas deben asignarse a `guilu` como reviewer cuando sea posible.
- El login/autenticación pública debe pensarse en torno al dominio configurado de Akademia, no a IPs privadas locales como solución final de producto.
- El prefijo `/api` forma parte del flujo actual y debe tratarse de forma consistente en frontend, backend y proxy.

---
## 3) Flujo operativo por ticket en este repo

### Preparación
1. Crear rama desde `main`.
2. Refinar ticket Jira antes de pasarlo a `In Progress`.
3. Cuando la tarea esté lista para implementación, crear o enlazar la GitHub issue que servirá como prompt para Claude.

### Implementación
4. Implementar en rama de trabajo, nunca directamente en `main`.
5. Formato de commit obligatorio en este proyecto: **gitmoji + clave Jira + conventional commit**.
   - formato: `<gitmoji> <AKDMIA-XXX>: <tipo>: <mensaje>`
   - ejemplo: `✨ AKDMIA-190: feat: integrate google users with internal roles`
6. Ejecutar tests relevantes.
7. Si toca backend/frontend de forma visible, validar el flujo manualmente en entorno dev.
8. Si hay cambios de frontend significativos, acompañar la PR con evidencia visual cuando aporte valor.

### Sync / entorno
8. Para resincronizar rama actual y regenerar dev:
   - `git pull --rebase origin <rama actual>`
   - `docker compose down`
   - `docker compose up --build -d`
   - `docker compose ps`

### PR / cierre
9. La PR debe incluir Summary, Tests y Report.
10. Enlazar la PR en Jira.
11. Pasar ticket a `Reviewing` al terminar.
12. Tras merge:
   - cerrar ticket en Jira si procede
   - volver a `main`
   - `git pull`
   - limpiar ramas locales/remotas si toca

---
## 4) Convenciones útiles del repo

- Usar etiquetas de Jira y GitHub coherentes con la capa afectada (`frontend`, `backend`, `ia`, etc.).
- Si una tarea es de investigación técnica, indicarlo claramente como **spike** en título o descripción.
- Si una tarea afecta a IA/RAG, dejar explícito qué parte es exploración y qué parte es implementación cerrada.
