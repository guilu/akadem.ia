# Tasks: Import Temas

## Phase 1: Backend Implementation

- [x] 1.1 Add `ImportSubjectRow` record and `importSubjects()` method stub to `ManageSubjectController.java` — record fields: `String name, String description`; method signature: `@PostMapping(value = "/import", consumes = "multipart/form-data") public ResponseEntity<Object> importSubjects(@RequestParam("file") MultipartFile file, @RequestParam(defaultValue = "csv") String format, @RequestParam UUID syllabusId, @AuthenticationPrincipal User principal)`

- [x] 1.2 Add guard clauses to `importSubjects()` in `ManageSubjectController.java`: null principal → 401; empty file → 400 `file_required`; file > 10 MB → 400 `file_too_large`; null/missing `syllabusId` → 400 `syllabusId_required`. Reuse the `MAX_UPLOAD_SIZE` constant already in `ManageQuestionController` — copy it into `ManageSubjectController`.

- [x] 1.3 Add CSV parser and JSON parser to `importSubjects()` in `ManageSubjectController.java`. Reuse the `parseCsv()` pattern from `ManageQuestionController`. Count data rows (excluding header); if > 500 return 400 `row_limit_exceeded` before processing any row.

- [x] 1.4 Add per-row processing loop to `importSubjects()` in `ManageSubjectController.java`: validate name not blank; query `contentService.getSubjectsByScope(callerId, isAdmin, visibility)` to detect duplicate name in syllabusId; call `Subject.createGlobal(name, desc, syllabusId)` for admins or `Subject.createPrivate(name, desc, callerId, syllabusId)` for non-admins; call `contentService.createSubject(subject)`; accumulate `List<Map<String,Object>> errors` with `{row, message}` entries and `int created` counter. Return `200 OK: { created, errors }`.

## Phase 2: Frontend Implementation

- [x] 2.1 Add six subject-import state variables to `Management.tsx` after the existing `importDone` / `importStats` block (lines 172–178): `subjectImportOpen`, `subjectImportFile`, `subjectImportFormat`, `subjectImportLoading`, `subjectImportMessage`, `subjectImportResult` (type `{ created: number; errors: { row: number; message: string }[] } | null`).

- [x] 2.2 Add `handleSubjectImport()` async function to `Management.tsx` (after `handleImport()`): build `FormData` with `file`, `format`, and `syllabusId=filterSyllabusId`; `POST` to `/api/manage/subjects/import`; set `subjectImportResult` from response; call `loadSubjects()` on success. Block resubmission while `subjectImportLoading` is true.

- [x] 2.3 Add "Importar" button to the Temas tab toolbar in `Management.tsx` (inside the `tab === 'subjects'` block, in the `flex items-center justify-between` header div at line ~417). Render only when `isAdmin && filterSyllabusId`. Use `btnOutline` class with `<FileImport className="w-6 h-6" />` icon. On click: set `subjectImportOpen(true)`.

- [x] 2.4 Add subject import modal JSX to `Management.tsx` (at the bottom of the `tab === 'subjects'` section, mirroring the questions import modal pattern). Include: format selector (csv/json), file input (disabled submit when no file), loading state, result summary showing "Subjects creados: N" and inline error list. Modal closes only via explicit cancel/close button, not automatically on success.

## Phase 3: Testing

- [x] 3.1 Add test `importSubjectsValidCsvCreatesSubjects` to `ManageSubjectControllerTest.java`: admin principal, 3-row CSV for `syllabusId=UUID`, mock `contentService.getSubjectsByScope` to return empty list (no duplicates), mock `contentService.createSubject` to return subject — assert `200 OK`, `created=3`, `errors` empty. Covers spec scenario "Valid CSV import creates subjects".

- [x] 3.2 Add test `importSubjectsNonAdminReturnsForbidden` to `ManageSubjectControllerTest.java`: non-admin principal — verify `403 Forbidden` and `contentService.createSubject` never called. Covers spec scenario "Non-admin user is rejected".

- [x] 3.3 Add test `importSubjectsExceedingRowLimitReturnsBadRequest` to `ManageSubjectControllerTest.java`: admin, CSV with 501 data rows — assert `400 Bad Request` with `error=row_limit_exceeded`, `createSubject` never called. Covers spec scenario "File exceeds row limit".

- [x] 3.4 Add test `importSubjectsPartialErrorReportsRowErrors` to `ManageSubjectControllerTest.java`: admin, 3-row CSV where row 2 has blank name — assert `200 OK`, `created=2`, `errors` list contains one entry with `row=2`. Covers spec scenario "Partial import with one invalid row".

- [x] 3.5 Add test `importSubjectsDuplicateNameReportsRowError` to `ManageSubjectControllerTest.java`: admin, 1-row CSV with name "Tema A", mock `contentService.getSubjectsByScope` to return a subject named "Tema A" — assert `200 OK`, `created=0`, `errors` list has one entry. Covers spec scenario "Duplicate subject name in same syllabus".
