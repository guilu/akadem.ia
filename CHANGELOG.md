## [Unreleased]

### ✨ Nuevas funcionalidades
- [AKDMIA-50](https://akadem-ia-app.atlassian.net/browse/AKDMIA-50) ✨ feat(flashcards): bulk import/export por unidad en CSV y JSON
- [AKDMIA-48](https://akadem-ia-app.atlassian.net/browse/AKDMIA-48) ✨ feat(settings): límites de estudio configurables por usuario
- [AKDMIA-75](https://akadem-ia-app.atlassian.net/browse/AKDMIA-75) 🎨 feat: actualizar assets e icono del home

### ✨ RAG — Generación de preguntas desde PDF
- ✨ feat(rag): RAG pipeline — generación automática de preguntas desde PDF (#61)
- ♻️ refactor(rag): 4 use cases, two-step indexing y draft review (#62)
- 🐛 fix(rag): fix semantic chunker init + dev api url (#63)

### 🎨 UX / UI
- ✨ feat(flashcards): navegación por materias y share icon en export
- ✨ feat(flashcards): reemplazar botones por Cerrar tras importación completada
- ✨ feat(settings): mejora UX del modal de importación de preguntas
- 🐛 fix(flashcards): contenido largo de tarjeta hace scroll en lugar de expandir
- 🐛 fix(flashcards): modal de importar carga todas las materias y mazos

---

## v1.2.0 - 2026-03-04

### ✨ Flashcards — Sistema completo de tarjetas de estudio
- [AKDMIA-38](https://akadem-ia-app.atlassian.net/browse/AKDMIA-38) 🗃️ feat(backend): migración inicial flashcards + esquema completo a Flyway
- [AKDMIA-39](https://akadem-ia-app.atlassian.net/browse/AKDMIA-39) ✨ feat(backend): dominio y puertos de Flashcards
- [AKDMIA-40](https://akadem-ia-app.atlassian.net/browse/AKDMIA-40) 🗃️ feat(backend): adaptadores de persistencia JPA para Flashcards
- [AKDMIA-41](https://akadem-ia-app.atlassian.net/browse/AKDMIA-41) ✨ feat(backend): implement SM-2 scheduler v1 (repetición espaciada)
- [AKDMIA-42](https://akadem-ia-app.atlassian.net/browse/AKDMIA-42) ✨ feat(backend): study queue y dashboard use cases + db healthcheck
- [AKDMIA-43](https://akadem-ia-app.atlassian.net/browse/AKDMIA-43) ✨ feat(backend): register flashcard review
- [AKDMIA-44](https://akadem-ia-app.atlassian.net/browse/AKDMIA-44) ✨ feat(backend): flashcards REST API
- [AKDMIA-45](https://akadem-ia-app.atlassian.net/browse/AKDMIA-45) ✨ feat(frontend): flashcards examinar UI (unit stats, labels y CTA)
- [AKDMIA-46](https://akadem-ia-app.atlassian.net/browse/AKDMIA-46) ✨ feat(frontend): flashcards study UI v2 (dark theme, colores, seed Flyway)
- [AKDMIA-47](https://akadem-ia-app.atlassian.net/browse/AKDMIA-47) ✨ feat(frontend): global study queue summary y refetch on review
- [AKDMIA-72](https://akadem-ia-app.atlassian.net/browse/AKDMIA-72) ✨ feat(frontend): learning steps, study next, session counters, interval hints y study button en home

### 🎨 Diseño — Rediseño completo de la interfaz
- 💄 redesign: full landing page redesign con hero, stats, features y CTA
- 💄 redesign: modernizar Navbar con menú móvil animado y toggle hamburguesa/X
- 💄 redesign: modernizar páginas de examen para seguir el design system
- 💄 redesign: modernizar Flashcards pages para seguir el design system
- 💄 redesign: modernizar SubjectsPage para seguir el design system
- 💄 redesign: modernizar Settings admin panel para seguir el design system
- 💄 redesign: modernizar formularios de login y register
- 🎨 feat: añadir imagen hero al landing page

### 🔒 Seguridad
- [AKDMIA-73](https://akadem-ia-app.atlassian.net/browse/AKDMIA-73) 🔒 security: fix vulnerabilidades críticas y mayores
- [AKDMIA-73](https://akadem-ia-app.atlassian.net/browse/AKDMIA-73) 💄 feat: UI/UX improvements en flujos de examen y estudio
- 🐛 fix: añadir origin red local a CORS y CSP connect-src
- 🐛 fix: resolver TypeScript build error en api.ts (AbortSignal)

### ✨ Mejoras de exámenes
- [AKDMIA-71](https://akadem-ia-app.atlassian.net/browse/AKDMIA-71) ✨ feat(frontend): color de nota por rango en resultado de examen

### ⚙️ Infraestructura
- 🗃️ refactor(backend): migrar esquema completo a Flyway
- ⬆️ chore(backend): actualizar Spring Boot 3.4/3.5 + Flyway 11/12
- 🐳 chore(infra): añadir db healthcheck para startup del API
- ⚙️ chore(infra): configurar Flyway para entorno de producción
- 🧩 chore(data): unidades constitución + reasignar preguntas

---

## v1.1.0 - 2026-02-22

- [AKDMIA-57](https://akadem-ia-app.atlassian.net/browse/AKDMIA-57) 🐛 Robustez: mensajes de error consistentes en formularios
- [AKDMIA-58](https://akadem-ia-app.atlassian.net/browse/AKDMIA-58) 🐛 Robustez: manejar timeouts en llamadas de examen
- [AKDMIA-59](https://akadem-ia-app.atlassian.net/browse/AKDMIA-59) 🧹 Refactor: constantes de rutas y labels
- [AKDMIA-60](https://akadem-ia-app.atlassian.net/browse/AKDMIA-60) 🧹 Refactor: duplicación de lógica entre páginas
- [AKDMIA-61](https://akadem-ia-app.atlassian.net/browse/AKDMIA-61) ✨ UX: confirmación al salir de examen
- [AKDMIA-62](https://akadem-ia-app.atlassian.net/browse/AKDMIA-62) ✨ UX: estado vacío en Historial con CTA
- [AKDMIA-63](https://akadem-ia-app.atlassian.net/browse/AKDMIA-63) ✨ UX: selector de dificultad en creación de examen
- [AKDMIA-64](https://akadem-ia-app.atlassian.net/browse/AKDMIA-64) ⚡ Performance: memoizar listas grandes en Settings
- [AKDMIA-65](https://akadem-ia-app.atlassian.net/browse/AKDMIA-65) ⚡ Performance: paginación en admin preguntas/usuarios
- [AKDMIA-66](https://akadem-ia-app.atlassian.net/browse/AKDMIA-66) 📦 Gestión: import/export de preguntas
- [AKDMIA-67](https://akadem-ia-app.atlassian.net/browse/AKDMIA-67) 📦 Gestión: ordenar preguntas por unidad y dificultad
- [AKDMIA-68](https://akadem-ia-app.atlassian.net/browse/AKDMIA-68) 📦 Gestión: filtro por materia/tema en admin
- [AKDMIA-69](https://akadem-ia-app.atlassian.net/browse/AKDMIA-69) ✅ Buenas prácticas: seeds por entorno
- [AKDMIA-70](https://akadem-ia-app.atlassian.net/browse/AKDMIA-70) ✅ Buenas prácticas: README de despliegue mínimo
