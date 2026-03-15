# Plan: AKDMIA-50 — Flashcards bulk import/export (Issue #66)

## Objetivo
Implementar importación y exportación masiva de flashcards por unidad, en formatos CSV y JSON.

---

## Backend

### 1. Use Case Interface
**Nuevo archivo:** `domain/port/in/FlashcardImportExportUseCase.java`
```
interface FlashcardImportExportUseCase {
  ImportResult importFlashcards(UUID unitId, String format, String content);
  String exportFlashcards(UUID unitId, String format);
}
```
- `ImportResult` record: `int imported, int skipped, List<String> errors`

### 2. Application Service
**Nuevo archivo:** `application/service/FlashcardImportExportService.java`
- Implementa `FlashcardImportExportUseCase`
- **CSV parse:** split por líneas, separar `front,back` con soporte de valores quoted
- **JSON parse:** Jackson `ObjectMapper` — array de objetos `{front, back}`
- Valida cada fila (front/back no vacíos)
- Reutiliza `FlashcardManagementUseCase.createFlashcard()` para insertar
- Retorna `ImportResult` con contadores

### 3. DTOs
**Modificar:** `adapter/inbound/web/dto/FlashcardDto.java`
- Añadir `record ImportResult(int imported, int skipped, List<String> errors)`

### 4. Controller endpoints
**Modificar:** `adapter/inbound/web/FlashcardController.java`

**Import:**
```
POST /api/flashcards/import?unitId={uuid}&format=csv|json
Content-Type: text/plain  (para CSV)
Content-Type: application/json  (para JSON)
Body: contenido del archivo
→ 200 { imported: N, skipped: M, errors: [...] }
```

**Export:**
```
GET /api/flashcards/export?unitId={uuid}&format=csv|json
→ 200 con Content-Disposition: attachment; filename="flashcards.csv"
    Content-Type: text/csv  o  application/json
```

Formato CSV exportado:
```
front,back
"¿Qué es X?","Es Y"
```

Formato JSON exportado:
```json
[{"front":"¿Qué es X?","back":"Es Y"}]
```

---

## Frontend

### 5. Botón Import/Export en FlashcardsPage
**Modificar:** `frontend/src/pages/FlashcardsPage.tsx`
- Añadir botones "Importar" / "Exportar" junto al listado de unidades
- El botón de exportar requiere seleccionar una unidad → dropdown por unidad en UnitCard
- El botón de importar abre un modal

### 6. Modal de importación
**Nuevo componente:** `frontend/src/components/flashcards/FlashcardImportModal.tsx`
- Selector de unidad (si no hay unitId preseleccionado)
- Selector de formato (CSV / JSON)
- Input de archivo (`<input type="file">`) + textarea opcional para pegar contenido
- Botón "Importar" → llama a `POST /api/flashcards/import`
- Muestra resultado: "X flashcards importadas, Y omitidas"

### 7. Botón Export por unidad
**Modificar:** `frontend/src/components/flashcards/UnitCard.tsx`
- Añadir menú contextual (3 puntos ⋯) o pequeño icono de descarga
- Opciones: "Exportar CSV" / "Exportar JSON"
- Llama a `GET /api/flashcards/export?unitId=...&format=...`
- Fuerza descarga del archivo vía `<a href download>`

---

## Archivos a crear/modificar

| Acción | Archivo |
|--------|---------|
| CREAR  | `backend/.../domain/port/in/FlashcardImportExportUseCase.java` |
| CREAR  | `backend/.../application/service/FlashcardImportExportService.java` |
| CREAR  | `frontend/src/components/flashcards/FlashcardImportModal.tsx` |
| MODIFICAR | `backend/.../web/dto/FlashcardDto.java` (añadir `ImportResult`) |
| MODIFICAR | `backend/.../web/FlashcardController.java` (añadir 2 endpoints) |
| MODIFICAR | `frontend/src/pages/FlashcardsPage.tsx` (botón import) |
| MODIFICAR | `frontend/src/components/flashcards/UnitCard.tsx` (botón export) |

---

## Decisiones de diseño
- **No se añade dependencia nueva**: CSV se parsea manualmente (formato sencillo de 2 columnas), Jackson ya está disponible para JSON.
- **Import vía body (no multipart)**: más simple, el frontend envía el contenido del archivo como texto plano.
- **Export sin streaming**: los volúmenes esperados son pequeños (flashcards por unidad), respuesta directa en memoria.
- **Sin seeds nuevas**: la importación usa los endpoints existentes de creación, respetando la arquitectura hexagonal.
