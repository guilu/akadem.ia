# Subjects Import Specification

## Purpose

Allows users to bulk-import subjects (temas) into a syllabus via a CSV or JSON file,
returning per-row results (created count + errors) without aborting on partial failures.
Admins can import into any syllabus; non-admin users can only import into syllabuses they own.

---

## Requirements

### Requirement: Backend — Import endpoint

The system MUST expose `POST /api/manage/subjects/import` accepting a multipart file and a
`syllabusId` parameter. Admins may import into any syllabus. Non-admin users may import only
into syllabuses they own (PRIVATE syllabuses where `ownerId` matches the authenticated user).
On success it MUST return `{ created: N, errors: [{ row, message }] }`.

#### Scenario: Valid CSV import creates subjects (admin)

- GIVEN an admin user is authenticated
- AND a CSV file with 3 valid rows (name, description) is prepared for `syllabusId=42`
- WHEN `POST /api/manage/subjects/import` is called with the file and `syllabusId=42`
- THEN the response is `200 OK` with `{ created: 3, errors: [] }`
- AND the 3 subjects appear in `GET /api/manage/subjects?syllabusId=42`

#### Scenario: Non-admin user imports into their own syllabus

- GIVEN a non-admin user is authenticated
- AND the `syllabusId` in the request belongs to that user (PRIVATE, ownerId matches)
- WHEN `POST /api/manage/subjects/import` is called with a valid file
- THEN the response is `200 OK` with the import results
- AND the subjects are created as PRIVATE subjects owned by that user

#### Scenario: Non-admin user imports into a syllabus they do not own

- GIVEN a non-admin user is authenticated
- AND the `syllabusId` in the request belongs to a different user (or does not exist)
- WHEN `POST /api/manage/subjects/import` is called
- THEN the response is `403 Forbidden`
- AND no subjects are created

#### Scenario: Unauthenticated request is rejected

- GIVEN no authentication token is present
- WHEN `POST /api/manage/subjects/import` is called
- THEN the response is `401 Unauthorized`

---

### Requirement: Backend — Row limit

The endpoint MUST reject files containing more than 500 rows with a `400 Bad Request`
before any row is processed.

#### Scenario: File exceeds row limit

- GIVEN an admin submits a CSV file with 501 data rows
- WHEN the endpoint receives the request
- THEN the response is `400 Bad Request` with an error message indicating the row limit
- AND no subjects are created

---

### Requirement: Backend — Per-row error reporting

The endpoint MUST process all rows independently. A validation failure on one row MUST NOT
abort processing of subsequent rows. Invalid rows MUST be reported in the `errors` array
with their 1-based row number and a human-readable message.

#### Scenario: Partial import with one invalid row

- GIVEN a CSV file with 3 rows where row 2 has a blank `name` field
- WHEN `POST /api/manage/subjects/import` is called
- THEN the response is `200 OK` with `{ created: 2, errors: [{ row: 2, message: "..." }] }`
- AND the 2 valid subjects are persisted

#### Scenario: Duplicate subject name in same syllabus

- GIVEN a subject named "Tema A" already exists in `syllabusId=42`
- AND the import file contains a row with `name=Tema A` for the same syllabus
- WHEN `POST /api/manage/subjects/import` is called
- THEN the duplicate row is included in `errors` with a message indicating duplication
- AND the row is NOT created (no silent upsert)

---

### Requirement: Frontend — Import button visibility

The Temas tab in the Management page MUST display an "Importar" button only when the admin
has a syllabus selected. The button MUST NOT be visible to non-admin users.

#### Scenario: Admin sees import button when syllabus is selected

- GIVEN the user is an admin and a syllabus is selected in the Temas tab
- WHEN the Temas tab is rendered
- THEN the "Importar" button is visible

#### Scenario: Button hidden when no syllabus selected

- GIVEN the admin has not yet selected a syllabus
- WHEN the Temas tab is rendered
- THEN the "Importar" button is not visible

#### Scenario: Non-admin does not see import button

- GIVEN the user is not an admin
- WHEN the Temas tab is rendered
- THEN the "Importar" button is not present in the UI

---

### Requirement: Frontend — Import modal flow

The import modal MUST allow the user to select a format (CSV or JSON), pick a file, and
submit. After submission the modal MUST display the result: total created count and a list
of row-level errors if any. The modal MUST prevent resubmission while a request is in flight.

#### Scenario: Successful import shows result summary

- GIVEN the admin opens the import modal with a syllabus selected
- AND uploads a valid CSV file
- WHEN the user clicks "Importar"
- THEN a loading state is shown while the request is in flight
- AND on success the modal displays "Subjects creados: N" with an empty errors section

#### Scenario: Partial import shows errors inline

- GIVEN the admin submits a file with 2 valid rows and 1 invalid row
- WHEN the response returns `{ created: 2, errors: [{ row: 3, message: "..." }] }`
- THEN the modal displays "Subjects creados: 2" and lists the row-3 error message
- AND the modal does not close automatically

#### Scenario: No file selected — submit disabled

- GIVEN the import modal is open
- AND no file has been picked
- WHEN the user views the modal
- THEN the "Importar" button is disabled
