Quiero que implementes en una nueva rama de git la primera versión funcional de un sistema de generación automática de preguntas tipo test desde documentos PDF usando RAG en mi aplicación Akademia.

## Objetivo funcional

Debes implementar el backend necesario para:

1. Subir un documento PDF fuente.
2. Extraer su texto.
3. Dividirlo en chunks semánticos.
4. Generar embeddings de esos chunks.
5. Guardarlos para búsqueda semántica.
6. Recuperar contexto relevante a partir de una petición de generación.
7. Generar preguntas tipo test con un LLM en formato JSON estructurado.
8. Validar mínimamente las preguntas generadas.
9. Persistirlas como borradores o preguntas generadas asociadas a una unidad/tema.
10. Exponer un endpoint REST para lanzar la generación.

No quiero frontend en esta tarea. Solo backend.

---

## Contexto del proyecto

La aplicación es Akademia, una plataforma para estudiar y hacer exámenes tipo test.
El stack principal es Java + Spring Boot.
Quiero una arquitectura limpia, cercana a hexagonal / DDD pragmático.
Prefiero casos de uso claros, puertos y adaptadores, sin acoplar toda la lógica a controladores o servicios de infraestructura.

Usa las convenciones ya existentes del proyecto.
Antes de cambiar nada, analiza la estructura actual y adáptate a ella.
No inventes una arquitectura paralela si ya existe una base coherente.

---

## Rama de trabajo

Crea y usa una rama nueva con un nombre descriptivo, por ejemplo:

feature/akdmia-rag-question-generation

Haz todos los cambios en esa rama.

---

## Alcance exacto de esta implementación

### 1. Subida de documentos fuente

Implementa la capacidad de subir un PDF como documento fuente.

Debes crear:

- modelo/entidad SourceDocument
- persistencia de metadatos del documento
- almacenamiento del fichero en local/filesystem para esta primera versión
- endpoint:
  POST /api/sources

Campos mínimos de SourceDocument:

- id
- name
- type
- version
- checksum
- uploadedAt
- status

Status sugeridos:
- UPLOADED
- PROCESSED
- FAILED

No hace falta UI ni gestión avanzada de storage cloud en esta fase.

---

### 2. Extracción de texto

Implementa extracción de texto del PDF.

Requisitos:

- usar una librería estable en Java, preferiblemente Apache PDFBox
- crear un puerto tipo SourceTextExtractorPort
- implementar un adaptador PDF
- normalizar el texto extraído:
  - espacios repetidos
  - saltos de línea raros
  - caracteres de control

El código debe ser extensible a futuros extractores.

---

### 3. Chunking semántico

Implementa división del documento en chunks.

Requisitos:

- crear modelo SourceChunk
- cada chunk debe guardar:
  - id
  - sourceDocumentId
  - content
  - metadata
- si el documento parece normativa o texto legal, prioriza división por artículos o secciones
- si no es posible detectar estructura legal, usar un fallback razonable por tamaño con overlap

Quiero una implementación pragmática:
- primero intenta chunking semántico por patrones tipo “Artículo X”
- si no encuentra estructura, fallback a chunking por tamaño

Añade metadatos útiles cuando se puedan detectar:
- article
- section
- page
- sourceDocumentId

---

### 4. Embeddings

Implementa generación de embeddings de cada chunk.

Requisitos:

- crear un puerto EmbeddingPort
- implementar un adaptador configurable por propiedades
- si ya existe un cliente AI en el proyecto, reutilízalo
- si no existe, crea una integración simple y limpia
- la configuración debe quedar externalizada

No acoples el dominio a un proveedor concreto.

Si no puedes completar la integración real por falta de credenciales o configuración, deja el código preparado con una implementación desacoplada y una estrategia clara, y documenta qué faltaría para activarlo.

---

### 5. Búsqueda semántica

Implementa almacenamiento y búsqueda vectorial.

Preferencia:
- PostgreSQL + pgvector, si encaja con el proyecto actual

Si el proyecto no está preparado todavía para pgvector, deja la estructura bien separada para que la búsqueda vectorial real pueda conectarse con poco esfuerzo.

Debes crear:

- puerto VectorSearchPort
- búsqueda topK por similitud
- filtrado mínimo por sourceDocumentId
- soporte para recuperar los chunks más relevantes para una petición

Si implementas pgvector:
- añade migraciones necesarias
- define índices razonables
- deja todo arrancable localmente si el proyecto ya usa Docker/Testcontainers

---

### 6. Generación de preguntas

Implementa el caso de uso principal de generación de cuestionarios.

Crear:

- GenerateQuizUseCase
- DTO/command de entrada
- puerto QuestionGeneratorPort
- adaptador LLM

Endpoint:
POST /api/ai/quizzes/generate

Request sugerida:
{
  "sourceId": "...",
  "unitId": "...",
  "topic": "Título II - La Corona",
  "difficulty": "MEDIUM",
  "questionCount": 15,
  "includeHints": true,
  "storeAsDraft": true
}

Comportamiento:

1. validar request
2. recuperar chunks relevantes
3. construir prompt estricto
4. invocar LLM
5. parsear JSON de respuesta
6. validar estructura
7. persistir resultado

---

## Prompt del generador

Quiero que el adaptador LLM use un prompt fuerte y poco ambiguo.

System prompt base:

"Eres un generador de preguntas tipo test para oposiciones. Debes usar exclusivamente el contexto proporcionado. No inventes información. Si el contexto no permite formular preguntas fiables, devuelve una lista vacía. Genera preguntas claras, no ambiguas, con 4 opciones y una sola correcta. Devuelve únicamente JSON válido."

El user prompt debe incluir:
- difficulty
- topic
- questionCount
- requisito de 4 respuestas
- 1 única correcta
- hint opcional
- explicación breve
- referencia al artículo si existe
- contexto recuperado

Formato JSON esperado:
{
  "questions": [
    {
      "statement": "...",
      "answers": [
        {"text": "..."},
        {"text": "..."},
        {"text": "..."},
        {"text": "..."}
      ],
      "correctIndex": 1,
      "hint": "...",
      "explanation": "...",
      "reference": "Artículo 62"
    }
  ]
}

No quiero texto libre. Solo JSON parseable.

---

### 7. Validación automática mínima

Implementa validación estructural de las preguntas generadas.

Reglas mínimas:
- exactamente 4 respuestas
- correctIndex entre 0 y 3
- statement no vacío
- respuestas no vacías
- evitar duplicados exactos entre respuestas

Si ves fácil añadir una validación contextual básica sin complicar demasiado:
- comprobar que la reference o explicación está relacionada con el contexto recuperado

Pero no conviertas esta fase en un proyecto aparte.

---

### 8. Persistencia

Quiero persistir las preguntas generadas.

Implementa una solución coherente con el modelo actual del proyecto.

Opciones aceptables:
- guardar como GeneratedQuestionDraft y dejar preparadas para revisión
- o guardar como Question/Answer si ya existe un modelo claro y encaja bien

Mi preferencia para esta fase:
- crear GeneratedQuestionDraft como paso intermedio
- asociarlo a sourceDocumentId y, si aplica, a unitId
- dejar preparado un mapper o conversión futura a Question

Campos sugeridos:
- id
- sourceDocumentId
- unitId
- topic
- difficulty
- statement
- answers
- correctIndex
- hint
- explanation
- reference
- createdAt
- status

Status sugeridos:
- GENERATED
- VALIDATED
- REJECTED

---

## Diseño técnico esperado

Quiero separación razonable por capas, por ejemplo:

- application
  - use cases
  - commands / queries
  - DTOs
  - ports
- domain
  - modelos
  - lógica de validación simple
- infrastructure
  - persistence
  - PDF extraction
  - embeddings
  - LLM client
  - vector search
  - REST controllers

Adáptalo a la estructura real del proyecto.
No fuerces nombres si ya hay una convención clara.

---

## Calidad de implementación

Quiero que trabajes como un ingeniero senior:

- primero inspecciona el proyecto
- entiende estructura, dependencias y convenciones
- minimiza cambios innecesarios
- no rompas funcionalidades existentes
- no metas refactors masivos no relacionados
- mantén naming coherente
- añade logs útiles, no ruido
- evita clases gigantes
- evita meter lógica compleja en controladores

---

## Base de datos y migraciones

Si el proyecto usa Flyway o Liquibase, usa lo que ya exista.
Añade las migraciones necesarias para:

- source_document
- source_chunk
- generated_question_draft
- campos vectoriales si aplica

No cambies tecnología de migración.
Usa la que ya tenga el proyecto.

---

## Configuración

Añade configuración externalizada para:

- storage path de documentos
- proveedor de embeddings
- proveedor de LLM
- claves/API base URL si aplica
- tamaño de chunk y overlap
- topK de retrieval

Usa application.yml / application.properties según convención del proyecto.

---

## Testing

Quiero tests razonables, no humo.

Implementa como mínimo:

1. tests unitarios para:
- chunking
- validación estructural de preguntas
- caso de uso GenerateQuizUseCase con mocks

2. tests de integración para:
- subida de documento
- generación de cuestionario
- persistencia básica

Si el proyecto usa Testcontainers, intégralo.
Si no es viable dejar una integración completa por dependencias externas del LLM, mockea el adaptador LLM en los tests de integración internos.

---

## Entregables obligatorios

Al terminar, quiero que dejes:

1. código implementado en la rama nueva
2. migraciones de BD
3. configuración mínima documentada
4. tests pasando
5. un resumen final en markdown con:
   - qué has implementado
   - qué endpoints has creado
   - qué entidades/modelos has añadido
   - qué decisiones técnicas has tomado
   - qué limitaciones quedan pendientes
   - cómo probarlo localmente

Nombre sugerido del documento:
docs/akdmia-rag-question-generation.md

---

## Restricciones importantes

- No implementes frontend
- No rehagas medio proyecto
- No cambies convenciones globales del código si no es imprescindible
- No introduzcas dependencias raras si hay una alternativa estable
- No uses soluciones mágicas ni frameworks innecesarios
- No dejes código muerto
- No hardcodees claves ni rutas absolutas

---

## Estrategia de trabajo

Quiero que avances en este orden:

1. analizar estructura actual del proyecto
2. crear rama
3. implementar subida de documentos
4. extracción de texto
5. chunking
6. embeddings
7. vector search
8. generación de preguntas
9. validación
10. persistencia
11. tests
12. documentación final

---

## Si te encuentras bloqueos

Si encuentras una limitación real del proyecto o una integración externa que no puedes completar del todo, no te pares.
Haz la mejor implementación posible, deja el código desacoplado y documenta claramente:
- qué está resuelto
- qué queda pendiente
- cómo activarlo después

---

## Qué espero al final

No quiero solo “código que compila”.
Quiero una primera versión funcional, limpia y extensible del backend RAG de Akademia para generación de preguntas desde PDFs.

Empieza inspeccionando la estructura actual del repositorio y proponiendo brevemente el plan de cambios antes de implementar.