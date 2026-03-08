# RAG Question Generation — Akademia Backend

## Qué se ha implementado

Sistema completo de generación automática de preguntas tipo test desde documentos PDF usando RAG (Retrieval-Augmented Generation), integrado en la arquitectura hexagonal existente del proyecto.

---

## Endpoints creados

### Gestión de documentos fuente

| Método | Ruta | Descripción | Autenticación |
|--------|------|-------------|---------------|
| `POST` | `/api/sources` | Subir y procesar un PDF | ADMIN |
| `GET` | `/api/sources` | Listar todos los documentos | ADMIN |
| `GET` | `/api/sources/{id}` | Obtener documento por ID | ADMIN |

**Subida de documento (multipart/form-data):**
```bash
curl -X POST http://localhost:8080/api/sources \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@constitucion.pdf"
```

**Respuesta:**
```json
{
  "id": "550e8400-...",
  "name": "constitucion.pdf",
  "type": "PDF",
  "checksum": "abc123...",
  "uploadedAt": "2026-03-08T10:00:00",
  "status": "PROCESSED"
}
```

### Generación de preguntas

| Método | Ruta | Descripción | Autenticación |
|--------|------|-------------|---------------|
| `POST` | `/api/ai/quizzes/generate` | Generar preguntas desde un PDF | ADMIN |
| `GET` | `/api/ai/quizzes/drafts?sourceId=...` | Listar borradores de un documento | ADMIN |

**Generar preguntas:**
```bash
curl -X POST http://localhost:8080/api/ai/quizzes/generate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceId": "550e8400-...",
    "unitId": "optional-unit-uuid",
    "topic": "Título II - La Corona",
    "difficulty": "MEDIUM",
    "questionCount": 10,
    "includeHints": true,
    "storeAsDraft": true
  }'
```

**Respuesta:**
```json
{
  "generated": 10,
  "questions": [
    {
      "id": "...",
      "statement": "¿Quién es el Jefe del Estado según la Constitución?",
      "answers": ["El Rey", "El Presidente del Gobierno", "El Presidente del Congreso", "El Defensor del Pueblo"],
      "correctIndex": 0,
      "hint": "Busca el artículo sobre el Título II",
      "explanation": "Según el Art. 56 CE, el Rey es el Jefe del Estado.",
      "reference": "Artículo 56",
      "status": "GENERATED"
    }
  ]
}
```

---

## Entidades y modelos añadidos

### Domain

- **`SourceDocument`** — Documento fuente (PDF). Estados: `UPLOADED → PROCESSED | FAILED`
- **`SourceChunk`** — Fragmento semántico del documento con metadata (artículo, sección, índice)
- **`GeneratedQuestionDraft`** — Pregunta generada como borrador. Estados: `GENERATED → VALIDATED | REJECTED`

### Ports (in)

- **`SourceDocumentUseCase`** — Upload + proceso + listing de documentos
- **`GenerateQuizUseCase`** — Generación de cuestionarios + listing de borradores

### Ports (out)

- **`SourceDocumentRepository`** — CRUD de documentos fuente
- **`SourceChunkRepository`** — CRUD de chunks con embeddings
- **`GeneratedQuestionDraftRepository`** — CRUD de borradores de preguntas
- **`SourceTextExtractorPort`** — Extracción de texto de documentos
- **`TextChunkerPort`** — División semántica del texto
- **`EmbeddingPort`** — Generación de embeddings vectoriales
- **`VectorSearchPort`** — Búsqueda semántica por similitud
- **`QuestionGeneratorPort`** — Generación de preguntas con LLM
- **`FileStoragePort`** — Almacenamiento de ficheros

### Tablas de BD (migración V002)

- `source_documents` — Metadatos del documento + storage_path + status
- `source_chunks` — Fragmentos con `embedding TEXT` (JSON array de floats)
- `generated_question_drafts` — Borradores de preguntas con `answers TEXT` (JSON array)

---

## Decisiones técnicas

### Extracción de texto — PDFBox 3.x
- `Loader.loadPDF()` (API de PDFBox 3.x)
- Normalización: control chars, saltos de línea, espacios múltiples
- Extensible: nuevas implementaciones de `SourceTextExtractorPort` para DOCX, TXT, etc.

### Chunking semántico — `SemanticChunker`
1. Detecta artículos/secciones con regex (`Artículo N`, `Art. N`, `ARTÍCULO N`, `Sección N`, `Capítulo N`)
2. Si hay ≥ 3 splits → chunking semántico por artículos con metadata enriquecida
3. Si hay < 3 splits → fallback a chunking por tamaño (configurable) con overlap y respeto de límites de frase

### Embeddings — OpenAI `text-embedding-3-small`
- Adapter `OpenAiEmbeddingAdapter` via Spring `RestClient`
- Configurable: modelo, proveedor, URL base
- Sin API key → error claro en tiempo de ejecución

### Vector search — In-memory coseno (V1)
- Los embeddings se persisten en DB como JSON string (`TEXT`)
- En tiempo de búsqueda: se cargan todos los chunks del documento y se computa coseno en Java
- **Upgrade path documentado** → pgvector (ver sección más abajo)

### Generación de preguntas — OpenAI `gpt-4o-mini`
- System prompt estricto + `response_format: {type: json_object}` para forzar JSON puro
- Temperatura 0.3 para respuestas deterministas
- Validación post-LLM: 4 respuestas exactas, correctIndex 0-3, sin duplicados, enunciado no vacío

### Seguridad
- Todos los endpoints nuevos bajo `@PreAuthorize("hasRole('ADMIN')")` (`@EnableMethodSecurity` ya estaba activo)

---

## Configuración necesaria

En `application.properties` o variables de entorno:

```properties
# REQUERIDO para que el sistema funcione
AI_API_KEY=sk-...                           # OpenAI API key

# Opcionales (tienen defaults razonables)
AI_CHAT_MODEL=gpt-4o-mini                   # Modelo para generar preguntas
AI_EMBEDDING_MODEL=text-embedding-3-small   # Modelo para embeddings
AI_BASE_URL=https://api.openai.com/v1       # Base URL del proveedor
RAG_STORAGE_PATH=/tmp/akademia-sources      # Directorio de PDFs subidos
RAG_CHUNK_SIZE=1000                         # Tamaño máximo de chunk (chars)
RAG_CHUNK_OVERLAP=200                       # Overlap entre chunks
RAG_RETRIEVAL_TOP_K=8                       # Chunks a recuperar por búsqueda
```

---

## Cómo probarlo localmente

### 1. Arrancar el stack

```bash
docker compose up -d
```

### 2. Obtener un token de admin

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"..."}' | jq -r .token)
```

### 3. Subir un PDF

```bash
curl -X POST http://localhost:8080/api/sources \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/ruta/a/constitucion.pdf"
```

> El endpoint procesa el documento sincrónicamente: extrae texto, crea chunks, genera embeddings y actualiza el status a `PROCESSED`.

### 4. Generar preguntas

```bash
curl -X POST http://localhost:8080/api/ai/quizzes/generate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceId": "<ID_DEL_PASO_3>",
    "topic": "Título II - La Corona",
    "difficulty": "MEDIUM",
    "questionCount": 5,
    "includeHints": true,
    "storeAsDraft": true
  }'
```

### 5. Ver borradores generados

```bash
curl "http://localhost:8080/api/ai/quizzes/drafts?sourceId=<ID>" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Limitaciones pendientes

### V1 → Upgrade path a pgvector

**Actualmente:** los embeddings se almacenan como `TEXT` (JSON array) y la similitud se computa en memoria. Funciona bien para documentos pequeños/medianos.

**Para escalar a producción real:**

```sql
-- 1. Activar extensión (una vez)
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Añadir columna vectorial
ALTER TABLE source_chunks ADD COLUMN embedding_vec vector(1536);

-- 3. Backfill (requiere script que lea embedding JSON y lo castee)

-- 4. Crear índice
CREATE INDEX ON source_chunks USING ivfflat (embedding_vec vector_cosine_ops)
  WITH (lists = 100);
```

Luego implementar `PgVectorSearchAdapter` que sustituya a `InMemoryVectorSearchAdapter`:
```java
@Query(value = "SELECT * FROM source_chunks WHERE source_document_id = :docId ORDER BY embedding_vec <=> :vec::vector LIMIT :topK", nativeQuery = true)
```

La interfaz `VectorSearchPort` no cambia — cero impacto en el dominio ni en los servicios.

### Otras limitaciones

- **Processing síncrono:** la subida de un PDF grande bloquea la request. Upgrade: mover el procesamiento a un job asíncrono (Spring `@Async` o cola de mensajes).
- **Storage local:** los PDFs se guardan en filesystem local. No sobrevive a redeployments en entornos efímeros. Upgrade: S3/GCS/Azure Blob via un nuevo adaptador de `FileStoragePort`.
- **Sin soporte DOCX/TXT:** solo PDF. Extensible añadiendo nuevas implementaciones de `SourceTextExtractorPort`.
- **Sin deduplicación de documentos:** si el mismo PDF se sube dos veces, se procesa dos veces (aunque el checksum queda registrado para futura validación).
- **Embeddings en cascada:** si el servidor cae durante el procesamiento de un doc grande, queda en estado `UPLOADED` sin finalizar. Pendiente: endpoint de reprocesamiento.
- **Sin paginación en `/api/sources` ni en `/api/ai/quizzes/drafts`.**

---

## Tests implementados

| Test | Tipo | Descripción |
|------|------|-------------|
| `SemanticChunkerTest` | Unitario | Chunking semántico por artículos, fallback por tamaño, metadata, indices secuenciales |
| `GenerateQuizServiceTest` | Unitario (mocks) | Caso de uso completo con stubs, almacenamiento condicional, validaciones de comando |
| `RagPersistenceAdapterTest` | Integración (H2) | CRUD de las 3 entidades nuevas, round-trip de embeddings y answers JSON |
