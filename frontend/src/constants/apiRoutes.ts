export const API_ROUTES = {
  auth: {
    login: '/api/auth/login',
    register: '/api/auth/register',
    oauthGoogle: '/api/oauth2/authorization/google',
  },
  users: {
    me: '/api/v1/users/me',
  },
  subjects: '/api/subjects',
  units: {
    root: '/api/units',
    availability: '/api/units/availability',
  },
  exams: {
    attempts: '/api/exams/attempts',
    start: '/api/exams/attempts/start',
    startRandom: '/api/exams/attempts/start-random',
    attempt: (id: string) => `/api/exams/attempts/${id}`,
    answers: (attemptId: string, questionId: string) =>
      `/api/exams/attempts/${attemptId}/answers/${questionId}`,
    submit: (id: string) => `/api/exams/attempts/${id}/submit`,
  },
  flashcards: {
    root: '/api/flashcards',
    unitsSummary: '/api/flashcards/units/summary',
    studyQueue: '/api/flashcards/study/queue',
    studyNext: '/api/flashcards/study/next',
    studyReview: '/api/flashcards/study/review',
    history: '/api/flashcards/history',
    export: '/api/flashcards/export',
    import: '/api/flashcards/import',
  },
  sources: {
    root: '/api/sources',
    confirm: (id: string) => `/api/sources/${id}/confirm`,
    single: (id: string) => `/api/sources/${id}`,
  },
  ai: {
    quizzesGenerate: '/api/ai/quizzes/generate',
    quizzesDrafts: '/api/ai/quizzes/drafts',
    draftApprove: (id: string) => `/api/ai/drafts/${id}/approve`,
    draftReject: (id: string) => `/api/ai/drafts/${id}/reject`,
  },
  settings: '/api/settings',
  admin: {
    users: '/api/admin/users',
    user: (id: string) => `/api/admin/users/${id}`,
    subjects: '/api/admin/subjects',
    subject: (id: string) => `/api/admin/subjects/${id}`,
    units: '/api/admin/units',
    unit: (id: string) => `/api/admin/units/${id}`,
    questions: '/api/admin/questions',
    question: (id: string) => `/api/admin/questions/${id}`,
    questionsExport: '/api/admin/questions/export',
    questionsImport: '/api/admin/questions/import',
  },
} as const;
