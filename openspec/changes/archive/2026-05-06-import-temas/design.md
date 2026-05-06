# Design: Import Temas

## Technical Approach

Reuse the existing question-import pattern in `ManageQuestionController` verbatim: add a
`POST /api/manage/subjects/import` endpoint directly inside `ManageSubjectController` (no new
class), parse CSV/JSON in the controller, call `contentService.createSubject(...)` per row,
and return `{ created, errors: [{ row, message }] }`. On the frontend, duplicate the
questions import modal state-machine (6 state variables, one async handler) inside
`Management.tsx` scoped to the Temas tab.

## Architecture Decisions

### Decision: Duplicate detect via in-memory query before save
**Choice**: Before calling `createSubject`, query `contentService.getSubjectsBySyllabus(syllabusId, ownerId)` and check if a subject with the same trimmed name already exists. Report duplicate as a row error.
**Alternatives**: DB unique constraint (requires migration); silent skip (forbidden by spec).
**Rationale**: No DB migration needed; consistent with proposal out-of-scope constraint on upsert. The row limit (500) keeps the in-memory lookup cheap.

### Decision: Row limit enforced before any processing
**Choice**: Count data rows (excluding header) upfront; return `400` immediately if > 500.
**Alternatives**: Cap mid-loop (complex error messaging).
**Rationale**: Matches spec scenario verbatim and mirrors the `MAX_UPLOAD_SIZE` guard pattern already in `ManageQuestionController`.

### Decision: Admin guard via existing `isAdmin()` helper
**Choice**: `isAdmin(principal)` check from `ManageSubjectController`; return `403` for non-admins trying to import GLOBAL subjects (non-admins import PRIVATE, same logic as `create()`).
**Alternatives**: Spring Security annotation (`@PreAuthorize`) — not used by any existing controller, so consistency wins.
**Rationale**: Every other method in `ManageSubjectController` uses the same inline `isAdmin()` guard pattern.

### Decision: Per-row error shape with 1-based row number and message
**Choice**: `errors: [{ row: int, message: String }]` instead of the current question-import `errors: int`.
**Alternatives**: Keep int error count (simpler but loses actionable detail).
**Rationale**: Spec explicitly requires `{ row, message }` per error; the current question import int-count is a known simplification that this change improves upon.

## Data Flow

```
Browser (FormData: file, format, syllabusId)
  │
  ▼
POST /api/manage/subjects/import   (ManageSubjectController)
  │
  ├─ 401 if not authenticated
  ├─ 403 if non-admin attempts (enforced at visibility assignment, not blanket block)
  ├─ 400 if file empty / too large / > 500 rows
  │
  ├─ parseCsv() / parseJson()  →  List<ImportRow(name, description)>
  │
  └─ for each row:
       ├─ validate name not blank  →  row error
       ├─ check duplicate in syllabus  →  row error
       ├─ Subject.createGlobal/createPrivate(name, description, [ownerId,] syllabusId)
       ├─ contentService.createSubject(subject)
       └─ created++
  │
  ▼
200 OK: { created: N, errors: [{ row, message }] }
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `backend/src/main/java/com/akdemya/adapter/inbound/web/ManageSubjectController.java` | Modify | Add `POST /import` endpoint, `ImportRow` record, CSV parser, per-row error list |
| `frontend/src/components/Management.tsx` | Modify | Add subject import state variables (`subjectImportOpen`, `subjectImportFile`, etc.), `handleSubjectImport()` function, "Importar" button in Temas toolbar (visible when `filterSyllabusId && isAdmin`), import modal JSX |
| `backend/src/test/java/com/akdemya/adapter/inbound/web/ManageSubjectControllerTest.java` | Modify | Add import test cases: valid CSV, non-admin 403, >500 rows 400, partial rows with errors |

No new files. No DB migration.

## Interfaces / Contracts

```
POST /api/manage/subjects/import
Content-Type: multipart/form-data

Parameters:
  file       (required) — CSV or JSON file
  format     (optional, default "csv") — "csv" | "json"
  syllabusId (required) — UUID of the target syllabus

CSV format (header row required):
  name,description
  "Tema 1","Descripción opcional"

JSON format:
  [{ "name": "Tema 1", "description": "Desc" }]

Response 200 OK:
  { "created": 3, "errors": [{ "row": 2, "message": "name_required" }] }

Response 400 Bad Request:
  { "error": "row_limit_exceeded" }   (> 500 rows)
  { "error": "file_required" }
  { "error": "file_too_large" }

Response 401 Unauthorized
Response 403 Forbidden
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit (controller) | Valid CSV creates N subjects; non-admin gets 403; >500 rows returns 400; blank name row returns row error; duplicate name returns row error | `ManageSubjectControllerTest` — extend existing pattern, mock `ContentManagement` and `UserRepository` |
| Unit (controller) | JSON format parses correctly | Same test class |
| Integration | Not required — mirrors question import which has no dedicated integration test | N/A |
| Frontend | Import button visible only when `isAdmin && filterSyllabusId` selected; disabled submit when no file | Manual / future Vitest component test |

## Migration / Rollout

No migration required. Uses existing `subjects` table. Rollback: revert `@PostMapping("/import")` block in `ManageSubjectController` and remove subject import state/modal from `Management.tsx`.

## Open Questions

- [ ] Should non-admin users be allowed to import PRIVATE subjects? Current proposal is silent on this; the design allows it (non-admins import PRIVATE, same as `create()`). Confirm with product.
- [ ] Should `syllabusId` be optional (defaulting to no syllabus) or always required? Spec says "syllabusId parameter" without fallback; design treats it as required with a `400` if absent.
