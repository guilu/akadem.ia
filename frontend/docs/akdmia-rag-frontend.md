# RAG Frontend — Módulo de generación automática de preguntas

## Descripción

Interfaz de administrador para gestionar el pipeline RAG (Retrieval-Augmented Generation) de Akademia. Permite subir documentos PDF, generar preguntas tipo test automáticamente usando LLMs, y consultar los borradores guardados.

**Acceso**: Solo usuarios con rol `ADMIN`. Enlace "IA" en la Navbar.
**Ruta**: `/rag`
**Rama**: `feature/akdmia-rag-frontend`

---

## Arquitectura del frontend

### Estructura de archivos

```
src/
├── pages/
│   └── RagPage.tsx             # Página principal con pestañas
├── components/rag/
│   ├── SourceUpload.tsx        # Zona drag-and-drop para subir PDFs
│   ├── SourceList.tsx          # Lista de documentos con estado
│   ├── QuizGenerateForm.tsx    # Formulario de configuración de generación
│   ├── QuizResults.tsx         # Vista de resultados con respuestas resaltadas
│   └── DraftList.tsx           # Lista de borradores guardados por documento
├── __tests__/rag/
│   ├── SourceUpload.test.tsx
│   ├── QuizGenerateForm.test.tsx
│   └── QuizResults.test.tsx
└── __tests__/
    └── setup.ts                # @testing-library/jest-dom
```

### Flujo de estado

- Estado local en `RagPage.tsx` (patrón del proyecto: sin Redux)
- `token` y `subjects` recibidos como props desde `App.tsx`
- `sources` cargadas al montar; se actualizan optimísticamente al subir un nuevo documento

---

## API

Las funciones RAG se añaden a `src/api.ts`:

| Función | Método | Endpoint |
|---------|--------|----------|
| `uploadSource(token, file)` | `POST` multipart | `/api/sources` |
| `getSources(token)` | `GET` | `/api/sources` |
| `generateQuiz(token, cmd)` | `POST` JSON | `/api/ai/quizzes/generate` |
| `getDrafts(token, sourceId)` | `GET` | `/api/ai/quizzes/drafts?sourceId=...` |
| `getUnitsForSubject(token, subjectId)` | `GET` | `/api/subjects/:id/units` |

La generación tiene un timeout de **120 segundos** (el LLM puede tardar).

---

## Tipos añadidos (`src/types.ts`)

```typescript
SourceDocument      // id, fileName, fileType, status, uploadedAt
GeneratedDraft      // statement, answers[], correctIndex, hint, explanation, reference, status
GenerateQuizCommand // sourceId, unitId?, topic, difficulty, questionCount, includeHints, storeAsDraft
GenerateQuizResponse // generated, questions: GeneratedDraft[]
AdminUnit           // id, name
```

---

## Componentes

### `RagPage`
Página principal con tres pestañas: **Fuentes**, **Generar**, **Borradores**.

### `SourceUpload`
- Zona drag-and-drop + click para seleccionar
- Validación client-side: solo PDF, máximo 50 MB
- Muestra estado de carga y errores

### `SourceList`
- Lista clicable de documentos con badge de estado (UPLOADED → amarillo, PROCESSED → verde, FAILED → rojo)
- Resalta el documento seleccionado

### `QuizGenerateForm`
- Selección de fuente (solo documentos PROCESSED)
- Campo de tema libre
- Dificultad (Fácil / Media / Difícil)
- Número de preguntas (1–20)
- Asignatura + unidad opcionales (cargadas dinámicamente)
- Checkboxes: incluir pistas, guardar como borradores

### `QuizResults`
- Vista de preguntas generadas con respuestas resaltadas en verde
- Muestra explicación, pista y referencia cuando están presentes
- Botón "Nueva generación" para resetear

### `DraftList`
- Selector de documento fuente
- Carga borradores bajo demanda

---

## Tests

Framework: **Vitest** + **@testing-library/react** + **jsdom**

```bash
cd frontend
npm install
npm test          # run once
npm run test:watch  # watch mode
```

Cobertura:
- `SourceUpload`: validación PDF, tamaño, upload OK, error de servidor
- `QuizGenerateForm`: renderizado, validación de campos, llamada correcta a onGenerate
- `QuizResults`: renderizado de preguntas, resaltado correcto, onReset

---

## Navbar

Se añade enlace "IA" con icono sparkles (inline SVG de Heroicons) visible solo para admins, en desktop y menú móvil.

---

## Dependencias añadidas

```json
"devDependencies": {
  "@testing-library/jest-dom": "^6.6.3",
  "@testing-library/react": "^16.1.0",
  "@testing-library/user-event": "^14.5.2",
  "jsdom": "^25.0.1",
  "vitest": "^2.1.4"
}
```

`vite.config.ts` actualizado con:
```typescript
test: {
  environment: 'jsdom',
  globals: true,
  setupFiles: './src/__tests__/setup.ts'
}
```
