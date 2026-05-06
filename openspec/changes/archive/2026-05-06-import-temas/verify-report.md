## Verification Report

**Change**: import-temas
**Verified**: 2026-05-06 (re-run after ownership fix)

---

### Completeness

| Metric | Value |
|--------|-------|
| Tasks total | 10 |
| Tasks complete | 10 |
| Tasks incomplete | 0 |

All 10 tasks in phases 1–3 are marked `[x]`.

---

### Build & Tests

**Backend Tests**: ✅ 27 passed / 0 failed / 0 skipped
**Frontend TypeScript**: ✅ No errors (exit 0)

Backend test run: `./gradlew test --tests "*ManageSubjectControllerTest"` — BUILD SUCCESSFUL in 13s.
TS check: `npx tsc --noEmit` — no output, exit 0.

---

### Spec Compliance Matrix

| Requirement | Scenario | Test | Result |
|-------------|----------|------|--------|
| Backend — Import endpoint | Valid CSV import creates subjects (admin) | `importSubjectsValidCsvCreatesSubjects` | ✅ |
| Backend — Import endpoint | Non-admin user imports into their own syllabus | `importSubjectsNonAdminEmptyFileReturnsBadRequest` (partial: tests guard ordering, not full success path) | ⚠️ |
| Backend — Import endpoint | Non-admin imports into syllabus they do not own (403) | `importSubjects_nonOwnerSyllabus_returnsForbidden` | ✅ |
| Backend — Import endpoint | Unauthenticated request rejected (401) | No dedicated test — deferred to Spring Security filter (consistent with all other endpoints) | ⚠️ |
| Backend — Row limit | File exceeds row limit (501 rows → 400) | `importSubjectsExceedingRowLimitReturnsBadRequest` | ✅ |
| Backend — Per-row error reporting | Partial import with one invalid row | `importSubjectsPartialErrorReportsRowErrors` | ✅ |
| Backend — Per-row error reporting | Duplicate subject name in same syllabus | `importSubjectsDuplicateNameReportsRowError` | ✅ |
| Frontend — Import button visibility | Admin sees button when syllabus selected | Code confirmed: `{isAdmin && filterSyllabusId && <button>Importar</button>}` | ✅ |
| Frontend — Import button visibility | Button hidden when no syllabus selected | Code confirmed: guarded by `filterSyllabusId` | ✅ |
| Frontend — Import button visibility | Non-admin does not see import button | Code confirmed: guarded by `isAdmin` | ✅ |
| Frontend — Import modal flow | Successful import shows result summary | Code confirmed: sets `subjectImportMessage` to `Temas creados: N` | ✅ |
| Frontend — Import modal flow | Partial import shows errors inline | Code confirmed: renders `errors.map(err => <li>Fila {err.row}: {err.message}</li>)` | ✅ |
| Frontend — Import modal flow | No file selected — submit disabled | Code confirmed: `disabled={subjectImportLoading \|\| !subjectImportFile}` | ✅ |

**Compliance**: 11/13 scenarios fully verified (2 warnings — see Issues below)

---

### Correctness (Static)

| Requirement | Status | Notes |
|-------------|--------|-------|
| Ownership check: non-admin 403 for foreign syllabus | ✅ | `contentService.getSyllabusById(syllabusId)` → check `caller.getId().equals(syllabus.getOwnerId())`; returns 403 if null or not owner |
| Row limit enforced before processing | ✅ | `dataRowCount > MAX_IMPORT_ROWS` check before loop |
| Blank name reported as row error, no abort | ✅ | `if (name.isEmpty()) { errors.add(...); continue; }` |
| Duplicate detection scoped to syllabusId | ✅ | Pre-loads subjects, filters by `syllabusId.equals(s.getSyllabusId())` |
| Created names added to in-memory set | ✅ | `existingNames.add(name.toLowerCase())` after each successful create — prevents intra-file dupes |
| JSON format supported | ✅ | `else` branch parses `ImportSubjectRow[]` via Jackson |
| 1-based row numbers in error list | ✅ | `int rowNum = i;` (i starts at 1 for CSV; `i + 1` for JSON) |
| `syllabusId` null check is redundant | ⚠️ | `@RequestParam UUID syllabusId` — Spring rejects null with 400 before method body; explicit null check at line 186 is unreachable dead code (harmless) |
| `format` and `syllabusId` passed as query params | ✅ | Frontend sends `?format=...&syllabusId=...`; Spring `@RequestParam` binds from both multipart form fields and query string |
| Modal closes only via explicit cancel | ✅ | No `setSubjectImportOpen(false)` on success path; only on cancel button click |
| `loadSubjects()` and `onSubjectsChanged()` called after success | ✅ | Both called after `res.ok` check |
| Spec says "Subjects creados: N" but UI shows "Temas creados: N" | ⚠️ | Minor wording deviation — "Temas" is the Spanish term used throughout the app; functionally correct |

---

### Coherence (Design)

| Decision | Followed? | Notes |
|----------|-----------|-------|
| No new classes — all changes in existing files | ✅ | `ManageSubjectController.java`, `Management.tsx`, `ManageSubjectControllerTest.java` only |
| No DB migration | ✅ | Uses existing `subjects` table |
| Inline `isAdmin()` guard (not `@PreAuthorize`) | ✅ | Consistent with all other methods in the controller |
| `{ row, message }` error shape (not int count) | ✅ | Implemented as `List<Map<String,Object>> errors` with `row` and `message` keys |
| Duplicate detect via in-memory query before save | ✅ | Single `getSubjectsByScope` call before loop; no N+1 |
| Non-admins import PRIVATE subjects into own syllabus only | ✅ | Ownership check added; non-admins get 403 for foreign syllabuses; for own syllabus they import PRIVATE |
| Row limit: 500 (MAX_IMPORT_ROWS constant) | ✅ | Constant defined, matches spec |
| File size limit: 10 MB (MAX_UPLOAD_SIZE constant) | ✅ | Constant defined |

---

### Issues Found

**WARNING 1**: Spec scenario "Non-admin user imports into their own syllabus" (success path) is only partially covered.
`importSubjectsNonAdminEmptyFileReturnsBadRequest` tests the empty-file guard (400) for a non-admin, not the happy-path where a non-admin with a valid file and owned syllabus gets a 200 with subjects created as PRIVATE. The ownership check fires after the file guard, so the new test (`importSubjects_nonOwnerSyllabus_returnsForbidden`) only validates the 403 path. A complementary test for the non-admin success path (own syllabus, valid file) would give full coverage.

**WARNING 2**: Spec scenario "Unauthenticated request is rejected (401)" has no controller-level test.
The design defers this to the Spring Security filter chain, which is consistent with every other endpoint in the project. The existing `nullPrincipalReturnsUnauthorized` test covers the defensive null-check in the controller body (not the filter). Acceptable, but worth noting.

**SUGGESTION**: The `if (syllabusId == null)` guard at line 186 of `ManageSubjectController` is dead code — Spring's `@RequestParam UUID syllabusId` will throw a `MissingServletRequestParameterException` (400) before the method body is entered if `syllabusId` is absent. It can be removed safely on a future cleanup pass.

**SUGGESTION**: The message "Temas creados: N" diverges from the spec's "Subjects creados: N". The Spanish version is more consistent with the app's UI language and is not a functional issue.

---

### Verdict

**PASS WITH WARNINGS**

Implementation is complete (10/10 tasks), all 27 backend tests pass (including the new `importSubjects_nonOwnerSyllabus_returnsForbidden` test), TypeScript compiles cleanly. The ownership check is correctly implemented and tested. All critical scenarios are covered. The two warnings are minor: one is a missing happy-path test for the non-admin success scenario (low risk given the logic is shared with the admin path), and one is a known architectural decision (auth delegated to filter). No blocking issues found.

**Recommended next step**: `/sdd-archive import-temas` — sync the spec to reflect the final non-admin ownership semantics and archive the change.
