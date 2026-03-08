Quiero que implementes en una nueva rama de git el frontend del módulo RAG de Akademia para consumir el backend ya existente de generación automática de preguntas desde documentos PDF.

## Contexto

El backend RAG ya está implementado y funcional.
Se ha integrado con:
- OpenRouter para embeddings usando openai/text-embedding-3-small
- Groq y/o OpenRouter para generación, según configuración del backend

No tienes que tocar la lógica del backend salvo que detectes un pequeño ajuste imprescindible de contrato.
El objetivo principal es implementar el frontend.

La aplicación frontend está hecha en React.
Analiza primero la estructura actual del proyecto y adáptate a sus convenciones reales:
- organización de carpetas
- cliente HTTP
- routing
- componentes UI
- gestión de estado
- diseño visual
- testing

No quiero una arquitectura paralela inventada.

---

## Objetivo funcional

Implementar una primera versión usable del frontend RAG con estas capacidades:

1. Subir documentos PDF fuente
2. Ver listado y estado de documentos fuente
3. Lanzar generación de cuestionarios IA
4. Mostrar el resultado de la generación
5. Mostrar borradores generados si ya existe endpoint/listado para ello, o dejar la UI preparada
6. Integrar todo esto en la navegación de Akademia

No quiero una demo cutre. Quiero una implementación coherente con el resto de la aplicación.

---

## Rama de trabajo

Crea y usa una rama nueva con nombre descriptivo, por ejemplo:

feature/akdmia-rag-frontend

Haz todos los cambios en esa rama.

---

## Endpoints backend esperados

Adáptate a la implementación real si difiere, pero parte de estos contratos:

### 1. Subida de documento
POST /api/sources
Content-Type: multipart/form-data

Response esperada aproximada:
{
  "sourceId": "ce-1978",
  "status": "UPLOADED"
}

### 2. Generación de cuestionario
POST /api/ai/quizzes/generate
Content-Type: application/json

Request aproximada:
{
  "sourceId": "ce-1978",
  "unitId": "titulo-ii",
  "topic": "Título II - La Corona",
  "difficulty": "MEDIUM",
  "questionCount": 15,
  "includeHints": true,
  "storeAsDraft": true
}

Response aproximada:
{
  "quizId": "quiz-gen-001",
  "questionsGenerated": 15,
  "questionsStored": 13,
  "questionsRejected": 2,
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

### 3. Si existen endpoints de listado
- GET /api/sources
- GET /api/ai/questions/drafts
úsalos
Si no existen, implementa el frontend dejando el código preparado y documenta claramente el gap.

---

## Alcance exacto

### 1. Módulo frontend RAG / IA

Crear una nueva sección de la aplicación para el flujo RAG.

La sección debe incluir como mínimo:

- vista de documentos fuente
- formulario de generación
- panel de resultados
- navegación razonable entre esas partes

Puedes implementarlo como:
- una página con tabs
- varias rutas hijas
- una sección de administración
elige la opción que mejor encaje con la app existente

No metas una experiencia Frankenstein.

---

### 2. Gestión de fuentes documentales

Implementar una UI para:

- subir PDFs
- listar documentos fuente
- mostrar estado de procesamiento

Campos visuales mínimos:
- nombre
- fecha
- versión si existe
- estado
- acciones básicas si tienen sentido

Requisitos UX:
- feedback visual al subir
- error claro si el archivo no es PDF o falla la subida
- estado loading
- success/error toast o equivalente si la app ya usa uno

---

### 3. Formulario de generación de cuestionarios

Crear un formulario para invocar el backend RAG.

Campos:
- sourceId (selector)
- unitId (si aplica según dominio actual)
- topic (texto o selector)
- difficulty (EASY, MEDIUM, HARD)
- questionCount
- includeHints
- storeAsDraft

Requisitos:
- validación en cliente
- deshabilitar submit mientras genera
- feedback visual de progreso
- mensajes de error claros

No quiero que se pueda lanzar un request absurdo tipo 0 preguntas o 800 preguntas porque alguien decidió vivir peligrosamente.

---

### 4. Visualización del resultado

Mostrar de forma clara:

- resumen del proceso
- número de preguntas generadas
- cuántas se han almacenado
- cuántas se han rechazado
- listado de preguntas generadas

Cada pregunta debe mostrar:
- statement
- answers
- respuesta correcta destacada
- hint si existe
- explanation si existe
- reference si existe

No hace falta editor en esta fase.
Sí hace falta buena legibilidad.

---

### 5. Drafts generados

Si ya existe endpoint backend para drafts:
- implementa pantalla/listado de drafts

Si todavía no existe:
- deja la estructura frontend preparada
- crea tipos, servicios y componentes reutilizables cuando tenga sentido
- documenta claramente qué falta para activarlo

No inventes datos falsos en producción.
Para desarrollo/test puedes usar mocks solo en tests.

---

### 6. Integración con navegación

Añade acceso a esta funcionalidad desde la navegación existente.

Requisitos:
- coherente con la estructura actual
- si la sección es solo interna/admin, colócala donde toque
- proteger ruta si el proyecto ya maneja permisos o roles

---

## Diseño técnico esperado

Quiero una implementación limpia y mantenible.

### Capa de cliente API
Crear o ampliar una capa tipada para consumir el backend RAG:

- tipos/interfaces TypeScript
- funciones de API para sources
- funciones de API para generate quiz
- parseo razonable de errores

No hagas fetch desperdigado por veinte componentes como si esto fuese 2017.

### Componentes
Separar razonablemente:
- page/container
- form components
- result components
- list components
- shared UI si aplica

### Estado
Usa el patrón ya existente en el proyecto.
Si ya hay React Query / TanStack Query, úsalo.
Si ya hay hooks y cliente API propio, respétalo.
No metas una librería nueva salvo necesidad real.

---

## Estilo y UX

Quiero que visualmente encaje con Akademia.

- respetar sistema de componentes existente
- respetar estilos y layout actuales
- interfaz limpia y clara
- buen espaciado y jerarquía visual
- estados vacíos bien resueltos
- no sobrecargar de cajas, bordes y widgets porque sí

---

## Testing

Implementa tests razonables para el frontend.

Como mínimo:

1. test del formulario de subida
- render
- validación básica
- envío correcto

2. test del formulario de generación
- validaciones
- submit
- loading state

3. test de renderizado de resultados
- preguntas con respuesta correcta destacada
- hint/explanation/reference
- estado vacío o error

Usa la infraestructura de tests ya existente en el proyecto.
No cambies de stack de testing.

---

## Entregables obligatorios

Al terminar, quiero:

1. código implementado en la rama nueva
2. navegación integrada
3. tests pasando
4. resumen final en markdown con:
   - qué pantallas/rutas has creado
   - qué componentes has añadido
   - qué contratos backend consumes
   - qué gaps backend existen si los hubiera
   - cómo probarlo localmente

Nombre sugerido:
docs/akdmia-rag-frontend.md

---

## Restricciones importantes

- No rehagas la UI completa de Akademia
- No metas dependencias nuevas salvo necesidad real
- No dupliques lógica que ya exista
- No hardcodees URLs si ya hay configuración centralizada
- No mezcles esta tarea con refactors masivos
- No inventes endpoints backend que no existan sin documentarlo claramente

---

## Estrategia de trabajo

Quiero que trabajes en este orden:

1. inspeccionar la estructura actual del frontend
2. crear rama
3. crear tipos y cliente API del módulo RAG
4. implementar gestión de fuentes
5. implementar formulario de generación
6. implementar visualización de resultados
7. integrar navegación
8. añadir tests
9. documentar resultado final

---

## Si encuentras bloqueos

Si detectas que falta algún endpoint backend para completar una parte del frontend:
- no te pares
- implementa hasta donde sea posible
- deja el código preparado
- documenta el contrato faltante y cómo conectarlo después

---

## Qué espero al final

Quiero una primera versión funcional y limpia del frontend RAG de Akademia, lista para que un usuario interno pueda:
- subir un PDF
- lanzar generación de preguntas
- ver el resultado de forma clara

Empieza inspeccionando la estructura actual del repositorio y proponiendo brevemente el plan de cambios antes de implementar.