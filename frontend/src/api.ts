import { API_ROUTES } from './constants/apiRoutes';

const rawBase = import.meta.env.VITE_API_URL || '';
const normalizedBase = rawBase.endsWith('/') ? rawBase.slice(0, -1) : rawBase;
const envBase = normalizedBase.endsWith('/api') ? normalizedBase.slice(0, -4) : normalizedBase;

const isExternalHost = window.location.hostname.endsWith('diegobarrioh.dev');
const defaultBase = isExternalHost ? window.location.origin : `http://${window.location.hostname}:8080`;
export const apiBase = envBase || defaultBase;

export function apiUrl(path: string): string {
  return `${apiBase}${path}`;
}

type RequestOptions = RequestInit & { timeoutMs?: number };

function mergeSignal(existing?: AbortSignal, timeoutMs?: number) {
  if (!timeoutMs) return { signal: existing, clear: () => {} };
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), timeoutMs);
  let signal = controller.signal;
  if (existing && typeof (AbortSignal as any).any === 'function') {
    signal = (AbortSignal as any).any([existing, controller.signal]);
  }
  return { signal, clear: () => window.clearTimeout(timeoutId) };
}

export async function apiJson<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const { timeoutMs, ...fetchOptions } = options;
  const { signal, clear } = mergeSignal(fetchOptions.signal ?? undefined, timeoutMs);
  try {
    const res = await fetch(url, { ...fetchOptions, signal });
    if (!res.ok) {
      const err: any = new Error('api_error');
      err.status = res.status;
      err.body = await res.json().catch(() => ({}));
      throw err;
    }
    if (res.status === 204) return undefined as T;
    const text = await res.text();
    if (!text) return undefined as T;
    return JSON.parse(text) as T;
  } catch (err: any) {
    if (err?.name === 'AbortError') {
      const timeoutErr: any = new Error('timeout');
      timeoutErr.code = 'timeout';
      throw timeoutErr;
    }
    throw err;
  } finally {
    clear();
  }
}

// User profile API functions
import type {
  SourceDocument, GeneratedDraft, GenerateQuizCommand, GenerateQuizResponse,
  IndexPreview, ConfirmIndexCommand, ApprovedUnit, UserProfile
} from './types';

export async function getMyProfile(token: string): Promise<UserProfile> {
  return apiAuthJson<UserProfile>(apiUrl(API_ROUTES.users.me), token);
}

export async function updateMyProfile(
  token: string,
  data: { firstName: string | null; lastName: string | null }
): Promise<UserProfile> {
  return apiAuthJson<UserProfile>(apiUrl(API_ROUTES.users.me), token, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
}

// RAG module API functions

export async function uploadSource(token: string, file: File, subjectId: string): Promise<IndexPreview> {
  const form = new FormData();
  form.append('file', file);
  form.append('subjectId', subjectId);
  const res = await fetch(apiUrl(API_ROUTES.sources.root), {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: form
  });
  if (!res.ok) {
    const err: any = new Error('api_error');
    err.status = res.status;
    err.body = await res.json().catch(() => ({}));
    throw err;
  }
  return res.json();
}

export async function confirmIndex(
  token: string,
  documentId: string,
  approvedUnits: ApprovedUnit[]
): Promise<{ document: SourceDocument; savedUnits: { id: string; name: string }[] }> {
  const cmd: ConfirmIndexCommand = { approvedUnits };
  return apiAuthJson(apiUrl(API_ROUTES.sources.confirm(documentId)), token, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(cmd)
  });
}

export async function getSources(token: string, subjectId?: string): Promise<SourceDocument[]> {
  const url = subjectId
    ? `${apiUrl(API_ROUTES.sources.root)}?subjectId=${subjectId}`
    : apiUrl(API_ROUTES.sources.root);
  return apiAuthJson<SourceDocument[]>(url, token);
}

export async function generateQuiz(token: string, cmd: GenerateQuizCommand): Promise<GenerateQuizResponse> {
  return apiAuthJson<GenerateQuizResponse>(apiUrl(API_ROUTES.ai.quizzesGenerate), token, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(cmd),
    timeoutMs: 120_000
  });
}

export async function getDrafts(token: string, sourceId: string, status?: string): Promise<GeneratedDraft[]> {
  const url = status
    ? `${apiUrl(API_ROUTES.ai.quizzesDrafts)}?sourceId=${sourceId}&status=${status}`
    : `${apiUrl(API_ROUTES.ai.quizzesDrafts)}?sourceId=${sourceId}`;
  return apiAuthJson<GeneratedDraft[]>(url, token);
}

export async function approveDraft(token: string, draftId: string): Promise<GeneratedDraft> {
  return apiAuthJson<GeneratedDraft>(apiUrl(API_ROUTES.ai.draftApprove(draftId)), token, {
    method: 'POST'
  });
}

export async function rejectDraft(token: string, draftId: string): Promise<GeneratedDraft> {
  return apiAuthJson<GeneratedDraft>(apiUrl(API_ROUTES.ai.draftReject(draftId)), token, {
    method: 'POST'
  });
}

export async function getUnitsForSubject(token: string, subjectId: string): Promise<{ id: string; name: string }[]> {
  return apiAuthJson<{ id: string; name: string }[]>(`${apiUrl(API_ROUTES.units.root)}?subjectId=${subjectId}`, token);
}

export async function deleteSource(token: string, id: string): Promise<void> {
  return apiAuthJson<void>(apiUrl(API_ROUTES.sources.single(id)), token, { method: 'DELETE' });
}

export async function exportFlashcardsBySubject(
  token: string,
  subjectId: string,
  format: 'csv' | 'json'
): Promise<string> {
  const res = await fetch(
    `${apiUrl(API_ROUTES.flashcards.export)}?subjectId=${subjectId}&format=${format}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  if (!res.ok) {
    const err: any = new Error('api_error');
    err.status = res.status;
    throw err;
  }
  return res.text();
}

// Subjects API functions

export async function getSubjects(token: string): Promise<import('./types').Subject[]> {
  return apiAuthJson<import('./types').Subject[]>(apiUrl(API_ROUTES.subjects), token);
}

// Exams API functions

export async function startExam(
  token: string,
  cfg: { unitCounts: Record<string, number>; minutes: number; difficulty?: 'EASY' | 'MEDIUM' | 'HARD' }
): Promise<import('./types').ExamStartResponse> {
  return apiAuthJson<import('./types').ExamStartResponse>(apiUrl(API_ROUTES.exams.start), token, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(cfg),
    timeoutMs: 15000
  });
}

export async function startRandomExam(
  token: string,
  cfg: { subjectId: string; count: number; minutes: number; difficulty?: 'EASY' | 'MEDIUM' | 'HARD' }
): Promise<import('./types').ExamStartResponse> {
  return apiAuthJson<import('./types').ExamStartResponse>(apiUrl(API_ROUTES.exams.startRandom), token, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(cfg),
    timeoutMs: 15000
  });
}

export async function submitExam(
  token: string,
  attemptId: string,
  selections: Record<string, string>
): Promise<import('./types').ExamResult> {
  return apiAuthJson<import('./types').ExamResult>(apiUrl(API_ROUTES.exams.submit(attemptId)), token, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ selections }),
    timeoutMs: 15000
  });
}

export async function getExamAttempts(token: string): Promise<import('./types').ExamAttemptSummary[]> {
  return apiAuthJson<import('./types').ExamAttemptSummary[]>(apiUrl(API_ROUTES.exams.attempts), token);
}

export async function getExamAttempt<T>(token: string, attemptId: string): Promise<T> {
  return apiAuthJson<T>(apiUrl(API_ROUTES.exams.attempt(attemptId)), token, { timeoutMs: 15000 });
}

export async function saveExamAnswer(
  token: string,
  attemptId: string,
  questionId: string,
  selectedAnswerId: string | undefined
): Promise<void> {
  return apiAuthJson<void>(apiUrl(API_ROUTES.exams.answers(attemptId, questionId)), token, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ selectedAnswerId }),
    timeoutMs: 15000
  });
}

// Flashcards API functions

export async function getFlashcardsUnitsSummary<T>(token: string): Promise<T> {
  return apiAuthJson<T>(apiUrl(API_ROUTES.flashcards.unitsSummary), token);
}

export async function getFlashcardsStudyQueue<T>(token: string, unitId?: string): Promise<T> {
  const url = unitId
    ? `${apiUrl(API_ROUTES.flashcards.studyQueue)}?unitId=${unitId}`
    : apiUrl(API_ROUTES.flashcards.studyQueue);
  return apiAuthJson<T>(url, token);
}

export async function getFlashcardsStudyNext<T>(token: string, unitId: string): Promise<T> {
  return apiAuthJson<T>(`${apiUrl(API_ROUTES.flashcards.studyNext)}?unitId=${unitId}`, token);
}

export async function reviewFlashcard(
  token: string,
  body: { flashcardId: string; grade: 'AGAIN' | 'GOOD' | 'EASY'; reviewedAt: string }
): Promise<void> {
  return apiAuthJson<void>(apiUrl(API_ROUTES.flashcards.studyReview), token, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
}

export async function getFlashcardsHistory<T>(token: string, limit?: number): Promise<T> {
  const url = limit !== undefined
    ? `${apiUrl(API_ROUTES.flashcards.history)}?limit=${limit}`
    : apiUrl(API_ROUTES.flashcards.history);
  return apiAuthJson<T>(url, token);
}

export async function getFlashcardsByUnit<T>(token: string, unitId: string): Promise<T> {
  return apiAuthJson<T>(`${apiUrl(API_ROUTES.flashcards.root)}?unitId=${unitId}`, token);
}

// Flashcards import API function

export async function importFlashcards(
  token: string,
  unitId: string,
  format: 'csv' | 'json',
  content: string
): Promise<{ imported: number; skipped: number; errors: string[] }> {
  const res = await fetch(
    `${apiUrl(API_ROUTES.flashcards.import)}?unitId=${unitId}&format=${format}`,
    {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'text/plain; charset=UTF-8',
      },
      body: content,
    }
  );
  if (!res.ok) throw new Error(`Error ${res.status}`);
  return res.json();
}

export async function exportFlashcardsByUnit(
  token: string,
  unitId: string,
  format: 'csv' | 'json'
): Promise<string> {
  const res = await fetch(
    `${apiUrl(API_ROUTES.flashcards.export)}?unitId=${unitId}&format=${format}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  if (!res.ok) {
    const err: any = new Error('api_error');
    err.status = res.status;
    throw err;
  }
  return res.text();
}

// Units availability API function

export async function getUnitsAvailability(
  token: string,
  subjectId: string,
  difficulty?: string
): Promise<import('./types').UnitAvailability[]> {
  const diffQuery = difficulty ? `&difficulty=${difficulty}` : '';
  return apiAuthJson<import('./types').UnitAvailability[]>(
    `${apiUrl(API_ROUTES.units.availability)}?subjectId=${subjectId}${diffQuery}`,
    token
  );
}

// Settings API functions

export async function getUserSettings(token: string): Promise<{ newCardsLimit: number; reviewCardsLimit: number }> {
  return apiAuthJson<{ newCardsLimit: number; reviewCardsLimit: number }>(apiUrl(API_ROUTES.settings), token);
}

export async function updateUserSettings(
  token: string,
  data: { newCardsLimit: number; reviewCardsLimit: number }
): Promise<{ newCardsLimit: number; reviewCardsLimit: number }> {
  return apiAuthJson<{ newCardsLimit: number; reviewCardsLimit: number }>(apiUrl(API_ROUTES.settings), token, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
}

// Admin API functions

export async function getAdminUsers(
  token: string,
  page: number,
  size = 10
): Promise<{ items: import('./components/Settings').AdminUser[]; page: number; totalPages: number }> {
  return apiAuthJson(
    `${apiUrl(API_ROUTES.admin.users)}?page=${page}&size=${size}`,
    token
  );
}

export async function saveAdminUser(
  token: string,
  form: { id?: string; email: string; firstName: string; lastName: string; occupation: string; role: string },
  isEditing: boolean
): Promise<void> {
  const body = JSON.stringify({
    email: form.email,
    firstName: form.firstName || null,
    lastName: form.lastName || null,
    occupation: form.occupation || null,
    role: form.role,
  });
  return apiAuthJson(
    isEditing ? apiUrl(API_ROUTES.admin.user(form.id!)) : apiUrl(API_ROUTES.admin.users),
    token,
    { method: isEditing ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' }, body }
  );
}

export async function deleteAdminUser(token: string, id: string): Promise<void> {
  return apiAuthJson(apiUrl(API_ROUTES.admin.user(id)), token, { method: 'DELETE' });
}

export async function getAdminSubjects(token: string): Promise<import('./components/Settings').AdminSubject[]> {
  return apiAuthJson(apiUrl(API_ROUTES.admin.subjects), token);
}

export async function saveAdminSubject(
  token: string,
  form: { id?: string; name: string; description: string },
  isEditing: boolean
): Promise<void> {
  const body = JSON.stringify({ name: form.name, description: form.description || null });
  return apiAuthJson(
    isEditing ? apiUrl(API_ROUTES.admin.subject(form.id!)) : apiUrl(API_ROUTES.admin.subjects),
    token,
    { method: isEditing ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' }, body }
  );
}

export async function deleteAdminSubject(token: string, id: string): Promise<void> {
  return apiAuthJson(apiUrl(API_ROUTES.admin.subject(id)), token, { method: 'DELETE' });
}

export async function getAdminUnits(token: string, subjectId: string): Promise<import('./components/Settings').AdminUnit[]> {
  return apiAuthJson(`${apiUrl(API_ROUTES.admin.units)}?subjectId=${subjectId}`, token);
}

export async function saveAdminUnit(
  token: string,
  form: { id?: string; subjectId: string; name: string; description: string; orderIndex: number },
  isEditing: boolean
): Promise<void> {
  const payload = {
    subjectId: form.subjectId,
    name: form.name,
    description: form.description || null,
    orderIndex: form.orderIndex || 0,
  };
  return apiAuthJson(
    isEditing ? apiUrl(API_ROUTES.admin.unit(form.id!)) : apiUrl(API_ROUTES.admin.units),
    token,
    { method: isEditing ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }
  );
}

export async function deleteAdminUnit(token: string, id: string): Promise<void> {
  return apiAuthJson(apiUrl(API_ROUTES.admin.unit(id)), token, { method: 'DELETE' });
}

export async function getAdminQuestions(
  token: string,
  unitId: string,
  page: number,
  size = 10
): Promise<{ items: import('./components/Settings').AdminQuestion[]; page: number; totalPages: number }> {
  return apiAuthJson(
    `${apiUrl(API_ROUTES.admin.questions)}?unitId=${unitId}&page=${page}&size=${size}`,
    token
  );
}

export async function saveAdminQuestion(
  token: string,
  form: import('./components/Settings').AdminQuestion & { unitId: string },
  isEditing: boolean
): Promise<void> {
  const payload = {
    unitId: form.unitId,
    text: form.text,
    explanation: form.explanation || null,
    difficulty: form.difficulty,
    answers: form.answers.map((a) => ({ text: a.text, correct: a.correct })),
  };
  return apiAuthJson(
    isEditing ? apiUrl(API_ROUTES.admin.question(form.id)) : apiUrl(API_ROUTES.admin.questions),
    token,
    { method: isEditing ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) }
  );
}

export async function deleteAdminQuestion(token: string, id: string): Promise<void> {
  return apiAuthJson(apiUrl(API_ROUTES.admin.question(id)), token, { method: 'DELETE' });
}

export async function exportAdminQuestions(
  token: string,
  format: 'csv' | 'json',
  unitId?: string
): Promise<Blob> {
  const query = unitId ? `?unitId=${unitId}&format=${format}` : `?format=${format}`;
  const res = await fetch(`${apiUrl(API_ROUTES.admin.questionsExport)}${query}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error('export_failed');
  return res.blob();
}

export async function importAdminQuestions(
  token: string,
  format: 'csv' | 'json',
  unitId: string,
  file: File
): Promise<{ created?: number; [key: string]: unknown }> {
  const formData = new FormData();
  formData.append('file', file);
  const res = await fetch(
    `${apiUrl(API_ROUTES.admin.questionsImport)}?format=${format}&unitId=${unitId}`,
    { method: 'POST', headers: { Authorization: `Bearer ${token}` }, body: formData }
  );
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error || 'import_failed');
  return data;
}

export async function apiAuthJson<T>(url: string, token: string, options: RequestOptions = {}): Promise<T> {
  const { timeoutMs, ...fetchOptions } = options;
  const { signal, clear } = mergeSignal(fetchOptions.signal ?? undefined, timeoutMs);
  try {
    const res = await fetch(url, {
      ...fetchOptions,
      signal,
      headers: { ...(fetchOptions.headers || {}), Authorization: `Bearer ${token}` }
    });
    if (!res.ok) {
      const err: any = new Error('api_error');
      err.status = res.status;
      err.body = await res.json().catch(() => ({}));
      throw err;
    }
    if (res.status === 204) return undefined as T;
    const text = await res.text();
    if (!text) return undefined as T;
    return JSON.parse(text) as T;
  } catch (err: any) {
    if (err?.name === 'AbortError') {
      const timeoutErr: any = new Error('timeout');
      timeoutErr.code = 'timeout';
      throw timeoutErr;
    }
    throw err;
  } finally {
    clear();
  }
}
