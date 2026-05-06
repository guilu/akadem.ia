# Proposal: Import Temas

## Intent
Admins currently add subjects (temas) one by one via the "Gestión de Temas" tab in `Management.tsx`. There is no bulk import, which makes seeding a new syllabus with many subjects slow and error-prone. This change adds CSV/JSON import of subjects into the management page, mirroring the existing question-import pattern.

## Scope
### In Scope
- Backend: `POST /api/manage/subjects/import` endpoint accepting CSV/JSON (name, description, syllabusId columns)
- Frontend: "Importar" button in the Temas tab of `Management.tsx`, opening a modal reusing the existing `DeleteModal`/import-modal pattern
- Import result feedback: count of created subjects and errors (same shape as question import)

### Out of Scope
- Importing units or questions as part of subject import
- Export of subjects
- Upsert / update-on-conflict logic

## Approach
Reuse the existing question-import pattern end-to-end:
- Backend: add `importSubjects(file, format, syllabusId, caller)` to `ContentManagement`, expose via a new `@PostMapping("/import")` in `ManageSubjectController`. Parse CSV rows (name, description) and call the existing `createSubject` for each.
- Frontend: add an "Importar" button in the subjects tab toolbar; open an inline modal (same structure as the existing questions-import modal in `Management.tsx`) with format selector, file picker, and optional syllabus pre-selection.

## Affected Areas
| Area | Impact | Description |
|------|--------|-------------|
| `backend/.../ManageSubjectController.java` | Modified | Add `POST /api/manage/subjects/import` endpoint |
| `backend/.../ContentManagement.java` | Modified | Add `importSubjects` use-case method |
| `frontend/src/components/Management.tsx` | Modified | Add import button + modal to the Temas tab |

## Risks
| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Large file causing timeout | Low | Stream rows; cap at 500 subjects per import |
| Duplicate subject names | Med | Return per-row errors in result; no silent dedup |
| Visibility escalation (non-admin sets GLOBAL) | Low | Reuse existing `isAdmin` guard in controller |

## Rollback Plan
- Backend: revert the `@PostMapping("/import")` block in `ManageSubjectController` and the `importSubjects` method in `ContentManagement`. No DB migration needed (uses existing `subjects` table).
- Frontend: remove the import button and modal state from `Management.tsx`.

## Dependencies
- Existing `ContentManagement.createSubject` and `ManageSubjectController` must remain stable.

## Success Criteria
- [ ] `POST /api/manage/subjects/import` with a valid CSV returns `{ created: N, errors: [] }` and subjects appear in `GET /api/manage/subjects`
- [ ] Importing an invalid row returns a row-level error without aborting the rest
- [ ] The "Importar" button in the Temas tab is visible only when a syllabus is selected
- [ ] Non-admin users cannot import GLOBAL subjects via the endpoint
