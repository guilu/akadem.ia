export const ROUTES = {
  home: '/',
  login: '/login',
  register: '/register',
  subjects: '/subjects',
  subjectBuilder: (subjectId: string) => `/subjects/${subjectId}/builder`,
  examRunner: '/exam',
  examAttempt: (attemptId: string) => `/exams/attempts/${attemptId}`,
  examResult: '/result',
  settings: '/settings',
  flashcards: '/flashcards',
  flashcardsStudy: '/flashcards/study',
  flashcardsHistory: '/flashcards/history',
  flashcardsExamine: '/flashcards/examine',
  rag: '/rag',
  oauth2Callback: '/oauth2/callback'
} as const;
