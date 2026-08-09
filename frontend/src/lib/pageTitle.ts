// Per-route document.title.
//
// The SPA never updated document.title, so every route reported the home
// title to Google Analytics and every GA report grouped by page_title
// collapsed into a single row. Titles are keyed by the SAME sanitized path
// analytics uses, so a route can never be titled with a raw token or an
// OAuth2 code.

import { sanitizePath } from './analytics';

export const SITE_NAME = 'Akadem.ia';

const HOME_TITLE = 'Akadem.ia — Prepara tus oposiciones con IA';

/** Keys are sanitized paths — see sanitizePath in ./analytics. */
const ROUTE_TITLES: Readonly<Record<string, string>> = {
  '/': HOME_TITLE,
  '/login': 'Iniciar sesión',
  '/register': 'Crear cuenta',
  '/syllabuses': 'Temarios',
  '/syllabuses/:syllabusId/subjects': 'Temas del temario',
  '/syllabuses/:syllabusId/exam-builder': 'Configurar examen',
  '/subjects': 'Temas',
  '/subjects/:subjectId/builder': 'Configurar examen',
  '/exam': 'Examen en curso',
  '/exams/attempts/:attemptId': 'Intento de examen',
  '/result': 'Resultado del examen',
  '/settings': 'Ajustes',
  '/manage': 'Gestionar',
  '/profile': 'Perfil',
  '/flashcards': 'Flashcards',
  '/flashcards/study': 'Estudiar flashcards',
  '/flashcards/history': 'Historial de flashcards',
  '/flashcards/examine': 'Examinar flashcards',
  '/rag': 'Generación de preguntas',
  '/temario': 'Temario',
  '/temario/subalterno-gva': 'Temario Subalterno GVA',
  '/descarga/:token': 'Descarga de tu temario',
  '/oauth2/callback': 'Accediendo…',
};

export function titleForPath(pathname: string): string {
  const title = ROUTE_TITLES[sanitizePath(pathname)];
  if (!title) return SITE_NAME;
  return title === HOME_TITLE ? title : `${title} · ${SITE_NAME}`;
}
