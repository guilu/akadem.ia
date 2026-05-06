import { describe, it, expect } from 'vitest';
import { API_ROUTES } from '../constants/apiRoutes';

function hasNoTrailingSlash(path: string) {
  return !path.endsWith('/');
}

function hasNoDoubleSlash(path: string) {
  return !path.includes('//');
}

describe('API_ROUTES — static routes', () => {
  const staticRoutes: [string, string][] = [
    ['auth.login', API_ROUTES.auth.login],
    ['auth.register', API_ROUTES.auth.register],
    ['auth.oauthGoogle', API_ROUTES.auth.oauthGoogle],
    ['users.me', API_ROUTES.users.me],
    ['subjects', API_ROUTES.subjects],
    ['units.root', API_ROUTES.units.root],
    ['units.availability', API_ROUTES.units.availability],
    ['exams.attempts', API_ROUTES.exams.attempts],
    ['exams.start', API_ROUTES.exams.start],
    ['exams.startRandom', API_ROUTES.exams.startRandom],
    ['flashcards.root', API_ROUTES.flashcards.root],
    ['flashcards.unitsSummary', API_ROUTES.flashcards.unitsSummary],
    ['flashcards.studyQueue', API_ROUTES.flashcards.studyQueue],
    ['flashcards.studyNext', API_ROUTES.flashcards.studyNext],
    ['flashcards.studyReview', API_ROUTES.flashcards.studyReview],
    ['flashcards.history', API_ROUTES.flashcards.history],
    ['flashcards.export', API_ROUTES.flashcards.export],
    ['flashcards.import', API_ROUTES.flashcards.import],
    ['sources.root', API_ROUTES.sources.root],
    ['ai.quizzesGenerate', API_ROUTES.ai.quizzesGenerate],
    ['ai.quizzesDrafts', API_ROUTES.ai.quizzesDrafts],
    ['settings', API_ROUTES.settings],
    ['admin.users', API_ROUTES.admin.users],
    ['admin.subjects', API_ROUTES.admin.subjects],
    ['admin.units', API_ROUTES.admin.units],
    ['admin.questions', API_ROUTES.admin.questions],
    ['admin.questionsExport', API_ROUTES.admin.questionsExport],
    ['admin.questionsImport', API_ROUTES.admin.questionsImport],
  ];

  it.each(staticRoutes)('%s starts with /api', (_name, path) => {
    expect(path.startsWith('/api')).toBe(true);
  });

  it.each(staticRoutes)('%s has no trailing slash', (_name, path) => {
    expect(hasNoTrailingSlash(path)).toBe(true);
  });

  it.each(staticRoutes)('%s has no double slashes', (_name, path) => {
    expect(hasNoDoubleSlash(path)).toBe(true);
  });

  it('auth.login resolves to /api/auth/login', () => {
    expect(API_ROUTES.auth.login).toBe('/api/auth/login');
  });

  it('auth.register resolves to /api/auth/register', () => {
    expect(API_ROUTES.auth.register).toBe('/api/auth/register');
  });

  it('auth.oauthGoogle resolves to /api/oauth2/authorization/google', () => {
    expect(API_ROUTES.auth.oauthGoogle).toBe('/api/oauth2/authorization/google');
  });

  it('users.me resolves to /api/v1/users/me', () => {
    expect(API_ROUTES.users.me).toBe('/api/v1/users/me');
  });

  it('subjects resolves to /api/subjects', () => {
    expect(API_ROUTES.subjects).toBe('/api/subjects');
  });

  it('units.root resolves to /api/units', () => {
    expect(API_ROUTES.units.root).toBe('/api/units');
  });

  it('units.availability resolves to /api/units/availability', () => {
    expect(API_ROUTES.units.availability).toBe('/api/units/availability');
  });

  it('exams.attempts resolves to /api/exams/attempts', () => {
    expect(API_ROUTES.exams.attempts).toBe('/api/exams/attempts');
  });

  it('exams.start resolves to /api/exams/attempts/start', () => {
    expect(API_ROUTES.exams.start).toBe('/api/exams/attempts/start');
  });

  it('exams.startRandom resolves to /api/exams/attempts/start-random', () => {
    expect(API_ROUTES.exams.startRandom).toBe('/api/exams/attempts/start-random');
  });

  it('flashcards.root resolves to /api/flashcards', () => {
    expect(API_ROUTES.flashcards.root).toBe('/api/flashcards');
  });

  it('flashcards.unitsSummary resolves to /api/flashcards/units/summary', () => {
    expect(API_ROUTES.flashcards.unitsSummary).toBe('/api/flashcards/units/summary');
  });

  it('flashcards.studyQueue resolves to /api/flashcards/study/queue', () => {
    expect(API_ROUTES.flashcards.studyQueue).toBe('/api/flashcards/study/queue');
  });

  it('flashcards.studyNext resolves to /api/flashcards/study/next', () => {
    expect(API_ROUTES.flashcards.studyNext).toBe('/api/flashcards/study/next');
  });

  it('flashcards.studyReview resolves to /api/flashcards/study/review', () => {
    expect(API_ROUTES.flashcards.studyReview).toBe('/api/flashcards/study/review');
  });

  it('flashcards.history resolves to /api/flashcards/history', () => {
    expect(API_ROUTES.flashcards.history).toBe('/api/flashcards/history');
  });

  it('flashcards.export resolves to /api/flashcards/export', () => {
    expect(API_ROUTES.flashcards.export).toBe('/api/flashcards/export');
  });

  it('flashcards.import resolves to /api/flashcards/import', () => {
    expect(API_ROUTES.flashcards.import).toBe('/api/flashcards/import');
  });

  it('sources.root resolves to /api/sources', () => {
    expect(API_ROUTES.sources.root).toBe('/api/sources');
  });

  it('ai.quizzesGenerate resolves to /api/ai/quizzes/generate', () => {
    expect(API_ROUTES.ai.quizzesGenerate).toBe('/api/ai/quizzes/generate');
  });

  it('ai.quizzesDrafts resolves to /api/ai/quizzes/drafts', () => {
    expect(API_ROUTES.ai.quizzesDrafts).toBe('/api/ai/quizzes/drafts');
  });

  it('settings resolves to /api/settings', () => {
    expect(API_ROUTES.settings).toBe('/api/settings');
  });

  it('admin.users resolves to /api/admin/users', () => {
    expect(API_ROUTES.admin.users).toBe('/api/admin/users');
  });

  it('admin.subjects resolves to /api/admin/subjects', () => {
    expect(API_ROUTES.admin.subjects).toBe('/api/admin/subjects');
  });

  it('admin.units resolves to /api/admin/units', () => {
    expect(API_ROUTES.admin.units).toBe('/api/admin/units');
  });

  it('admin.questions resolves to /api/admin/questions', () => {
    expect(API_ROUTES.admin.questions).toBe('/api/admin/questions');
  });

  it('admin.questionsExport resolves to /api/admin/questions/export', () => {
    expect(API_ROUTES.admin.questionsExport).toBe('/api/admin/questions/export');
  });

  it('admin.questionsImport resolves to /api/admin/questions/import', () => {
    expect(API_ROUTES.admin.questionsImport).toBe('/api/admin/questions/import');
  });
});

describe('API_ROUTES — dynamic route builders', () => {
  it('exams.attempt returns correct path', () => {
    expect(API_ROUTES.exams.attempt('abc-123')).toBe('/api/exams/attempts/abc-123');
  });

  it('exams.answers returns correct path', () => {
    expect(API_ROUTES.exams.answers('attempt-1', 'q-99')).toBe(
      '/api/exams/attempts/attempt-1/answers/q-99'
    );
  });

  it('exams.submit returns correct path', () => {
    expect(API_ROUTES.exams.submit('attempt-42')).toBe('/api/exams/attempts/attempt-42/submit');
  });

  it('sources.confirm returns correct path', () => {
    expect(API_ROUTES.sources.confirm('doc-1')).toBe('/api/sources/doc-1/confirm');
  });

  it('sources.single returns correct path', () => {
    expect(API_ROUTES.sources.single('src-7')).toBe('/api/sources/src-7');
  });

  it('ai.draftApprove returns correct path', () => {
    expect(API_ROUTES.ai.draftApprove('d-1')).toBe('/api/ai/drafts/d-1/approve');
  });

  it('ai.draftReject returns correct path', () => {
    expect(API_ROUTES.ai.draftReject('d-1')).toBe('/api/ai/drafts/d-1/reject');
  });

  it('admin.user returns correct path', () => {
    expect(API_ROUTES.admin.user('u-5')).toBe('/api/admin/users/u-5');
  });

  it('admin.subject returns correct path', () => {
    expect(API_ROUTES.admin.subject('s-3')).toBe('/api/admin/subjects/s-3');
  });

  it('admin.unit returns correct path', () => {
    expect(API_ROUTES.admin.unit('un-8')).toBe('/api/admin/units/un-8');
  });

  it('admin.question returns correct path', () => {
    expect(API_ROUTES.admin.question('q-12')).toBe('/api/admin/questions/q-12');
  });

  describe('no trailing or double slashes in dynamic routes', () => {
    const dynamicResults = [
      API_ROUTES.exams.attempt('x'),
      API_ROUTES.exams.answers('a', 'b'),
      API_ROUTES.exams.submit('x'),
      API_ROUTES.sources.confirm('x'),
      API_ROUTES.sources.single('x'),
      API_ROUTES.ai.draftApprove('x'),
      API_ROUTES.ai.draftReject('x'),
      API_ROUTES.admin.user('x'),
      API_ROUTES.admin.subject('x'),
      API_ROUTES.admin.unit('x'),
      API_ROUTES.admin.question('x'),
    ];

    it.each(dynamicResults)('"%s" has no trailing slash', (path) => {
      expect(hasNoTrailingSlash(path)).toBe(true);
    });

    it.each(dynamicResults)('"%s" has no double slashes', (path) => {
      expect(hasNoDoubleSlash(path)).toBe(true);
    });
  });
});
