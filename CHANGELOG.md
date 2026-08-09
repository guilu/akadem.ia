## [Unreleased]

---

## v1.4.0 - 2026-08-09

### ✨ Nuevas funcionalidades
- ✨ feat(analytics): integración de Google Analytics 4 con consentimiento previo (#141) — propiedad GA4 propia `Akadem.ia`, independiente de la de `diegobarrioh.dev`. El script de gtag solo se inyecta después de que el usuario acepte en el banner de consentimiento, de modo que rechazar no deja ninguna cookie de analítica (RGPD/AEPD). Sin GTM: para un SPA versionado en git añadiría una superficie de configuración fuera del repo sin aportar nada.

### 🔒 Privacidad y seguridad
- 🔒 security(analytics): el `page_view` se envía manualmente con la ruta saneada en lugar de usar el tracking automático por historial. Dos rutas llevan secretos en la URL — `/descarga/:token` (token de descarga de compra) y el callback OAuth2 (`?code=`) — y el envío automático de `page_location` los habría almacenado en Google Analytics. `sanitizePath()` colapsa además los ids de intento, temario y materia, y descarta query string y fragmento.
- 🔒 security(csp): la CSP de `index.html` permite `googletagmanager.com` en `script-src` y los endpoints de `google-analytics.com` en `connect-src`, sin ampliar nada más.

### 🔧 Infraestructura
- 🔧 chore(build): `VITE_GA_MEASUREMENT_ID` como build arg en `Dockerfile` y `compose.yaml`, siguiendo el patrón de `VITE_STRIPE_PUBLISHABLE_KEY`. Sin valor definido la analítica queda desactivada por completo, banner incluido, que es el comportamiento en dev.

### ✅ Tests
- ✅ test(analytics): 26 tests nuevos que cubren el saneado de rutas, la puerta de consentimiento y la deduplicación de `page_view`.

---

## v1.3.0 - 2026-06-12

### ✨ Nuevas funcionalidades
- [AKDMIA-214](https://akadem-ia-app.atlassian.net/browse/AKDMIA-214) ✨ feat(payments): compra digital del Temario Subalterno GVA — pago único 15 € con Stripe + email Resend con enlace de descarga + página `/descarga/:token` (guest checkout sin cuenta de usuario, idempotencia webhook + reconciliation scheduler para reintentos)
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

### 💄 Marketing y branding (#138)
- 💄 feat(branding): nuevo logo de Akadem.ia en navbar y favicons; eliminado set de iconos legacy
- 🔧 chore(branding): meta tags OG/Twitter y título SEO
- ✨ feat(marketing): footer con enlaces de sponsor y sección de soporte
- 📝 docs(readme): cabecera con marca, capturas home light/dark, badges de shields.io

### 🐛 Correcciones de la revisión de código (#139)
- 🐛 fix(auth): restauración de sesión tras expirar el access token — `/api/auth/me` ahora hace refresh+retry; recargar la página ya no desloguea con cookie de refresh válida
- 🐛 fix(auth): peticiones concurrentes ya no quedan colgadas si el refresh de token falla
- 🐛 fix(auth): registro concurrente con el mismo email devuelve `email_in_use` en vez de 500; mismo race corregido en primer login OAuth2 (retry)
- 🔒 security(auth): rate limit usa el último salto de `X-Forwarded-For` (no falsificable) y acota los buckets a 10k IPs; purga de códigos OAuth2 expirados
- 🐛 fix(payments): PaymentIntents abandonados (>24h) se cancelan en Stripe y se marcan FAILED — antes quedaban PENDING para siempre por una rama muerta (`payment_failed` no es un status)
- 🐛 fix(payments): email de descarga se envía tras el commit de la transacción — evita duplicados en rollback y no retiene conexión DB durante HTTP
- 🐛 fix(exams): submit de examen atómico (`WHERE finished_at IS NULL`) — el doble submit concurrente ya no finaliza dos veces
- 🐛 fix(rag): upload en 3 transacciones — un fallo al persistir chunks ya no descarta el estado FAILED ni hace desaparecer el documento (trampa rollback-only)
- 🐛 fix(manage): import de preguntas atómico por fila — sin preguntas huérfanas si fallan sus respuestas; export CSV registra WARN por preguntas omitidas
- 🐛 fix(flashcards): lock pesimista en registro de reviews — el doble-tap ya no aplica la calificación SM-2 dos veces
- ♻️ refactor(exams): eliminados comentarios de andamiaje y factory muerta `ExamAttempt.start()`
- 🔧 chore(config): eliminada config duplicada `app.flashcards.*` del perfil dev

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
