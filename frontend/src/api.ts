const rawBase = import.meta.env.VITE_API_URL || '';
const normalizedBase = rawBase.endsWith('/') ? rawBase.slice(0, -1) : rawBase;
const envBase = normalizedBase.endsWith('/api') ? normalizedBase.slice(0, -4) : normalizedBase;

const isExternalHost = window.location.hostname.endsWith('diegobarrioh.dev');
const defaultBase = isExternalHost ? window.location.origin : `http://${window.location.hostname}:8080`;
export const apiBase = envBase || defaultBase;

type RequestOptions = RequestInit & { timeoutMs?: number };

function mergeSignal(existing?: AbortSignal, timeoutMs?: number) {
  if (!timeoutMs) return { signal: existing, clear: () => {} };
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), timeoutMs);
  let signal = controller.signal;
  if (existing && typeof (AbortSignal as { any?: unknown }).any === 'function') {
    signal = (AbortSignal as unknown as { any: (signals: AbortSignal[]) => AbortSignal }).any([existing, controller.signal]);
  }
  return { signal, clear: () => window.clearTimeout(timeoutId) };
}

let isRefreshing = false;
let refreshSubscribers: Array<() => void> = [];

async function tryRefreshToken(): Promise<boolean> {
  if (isRefreshing) {
    return new Promise<boolean>((resolve) => {
      refreshSubscribers.push(() => resolve(true));
    });
  }
  isRefreshing = true;
  try {
    const res = await fetch(`${apiBase}/api/auth/refresh`, {
      method: 'POST',
      credentials: 'include',
    });
    if (res.ok) {
      refreshSubscribers.forEach((cb) => cb());
      refreshSubscribers = [];
      return true;
    }
    refreshSubscribers = [];
    return false;
  } finally {
    isRefreshing = false;
  }
}

export async function apiJson<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const { timeoutMs, ...fetchOptions } = options;
  const { signal, clear } = mergeSignal(fetchOptions.signal ?? undefined, timeoutMs);
  try {
    const res = await fetch(url, { ...fetchOptions, signal, credentials: 'include' });
    if (res.status === 401) {
      // Avoid refresh loops on the refresh endpoint itself
      if (!url.includes('/api/auth/')) {
        const refreshed = await tryRefreshToken();
        if (refreshed) {
          const retryRes = await fetch(url, { ...fetchOptions, signal, credentials: 'include' });
          if (!retryRes.ok) {
            const err = {
              message: 'api_error',
              status: retryRes.status,
              body: await retryRes.json().catch(() => ({})),
            };
            throw err;
          }
          if (retryRes.status === 204) return undefined as T;
          const retryText = await retryRes.text();
          if (!retryText) return undefined as T;
          return JSON.parse(retryText) as T;
        }
      }
      const err: { message: string; status: number; body: unknown; name?: string } = {
        message: 'api_error',
        status: res.status,
        body: await res.json().catch(() => ({})),
      };
      throw err;
    }
    if (!res.ok) {
      const err: { message: string; status: number; body: unknown; name?: string } = {
        message: 'api_error',
        status: res.status,
        body: await res.json().catch(() => ({})),
      };
      throw err;
    }
    if (res.status === 204) return undefined as T;
    const text = await res.text();
    if (!text) return undefined as T;
    return JSON.parse(text) as T;
  } catch (err: unknown) {
    if (err && typeof err === 'object' && 'name' in err && (err as { name: string }).name === 'AbortError') {
      const timeoutErr = Object.assign(new Error('timeout'), { code: 'timeout' });
      throw timeoutErr;
    }
    throw err;
  } finally {
    clear();
  }
}

// User auth API functions
import type {
  SourceDocument, GeneratedDraft, GenerateQuizCommand, GenerateQuizResponse,
  IndexPreview, ConfirmIndexCommand, ApprovedUnit, UserProfile
} from './types';

export async function getMe(): Promise<{ email: string; role: string }> {
  return apiJson<{ email: string; role: string }>(`${apiBase}/api/auth/me`);
}

export async function logout(): Promise<void> {
  return apiJson<void>(`${apiBase}/api/auth/logout`, { method: 'POST' });
}

export async function getMyProfile(): Promise<UserProfile> {
  return apiJson<UserProfile>(`${apiBase}/api/v1/users/me`);
}

export async function updateMyProfile(
  data: { firstName: string | null; lastName: string | null }
): Promise<UserProfile> {
  return apiJson<UserProfile>(`${apiBase}/api/v1/users/me`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  });
}

// RAG module API functions

export async function uploadSource(file: File, subjectId: string): Promise<IndexPreview> {
  const form = new FormData();
  form.append('file', file);
  form.append('subjectId', subjectId);
  const res = await fetch(`${apiBase}/api/sources`, {
    method: 'POST',
    body: form,
    credentials: 'include'
  });
  if (!res.ok) {
    const err = Object.assign(new Error('api_error'), {
      status: res.status,
      body: await res.json().catch(() => ({})),
    });
    throw err;
  }
  return res.json() as Promise<IndexPreview>;
}

export async function confirmIndex(
  documentId: string,
  approvedUnits: ApprovedUnit[]
): Promise<{ document: SourceDocument; savedUnits: { id: string; name: string }[] }> {
  const cmd: ConfirmIndexCommand = { approvedUnits };
  return apiJson(`${apiBase}/api/sources/${documentId}/confirm`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(cmd)
  });
}

export async function getSources(subjectId?: string): Promise<SourceDocument[]> {
  const url = subjectId
    ? `${apiBase}/api/sources?subjectId=${subjectId}`
    : `${apiBase}/api/sources`;
  return apiJson<SourceDocument[]>(url);
}

export async function generateQuiz(cmd: GenerateQuizCommand): Promise<GenerateQuizResponse> {
  return apiJson<GenerateQuizResponse>(`${apiBase}/api/ai/quizzes/generate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(cmd),
    timeoutMs: 120_000
  });
}

export async function getDrafts(sourceId: string, status?: string): Promise<GeneratedDraft[]> {
  const url = status
    ? `${apiBase}/api/ai/quizzes/drafts?sourceId=${sourceId}&status=${status}`
    : `${apiBase}/api/ai/quizzes/drafts?sourceId=${sourceId}`;
  return apiJson<GeneratedDraft[]>(url);
}

export async function approveDraft(draftId: string): Promise<GeneratedDraft> {
  return apiJson<GeneratedDraft>(`${apiBase}/api/ai/drafts/${draftId}/approve`, {
    method: 'POST'
  });
}

export async function rejectDraft(draftId: string): Promise<GeneratedDraft> {
  return apiJson<GeneratedDraft>(`${apiBase}/api/ai/drafts/${draftId}/reject`, {
    method: 'POST'
  });
}

export async function getUnitsForSubject(subjectId: string): Promise<{ id: string; name: string }[]> {
  return apiJson<{ id: string; name: string }[]>(`${apiBase}/api/units?subjectId=${subjectId}`);
}

export async function deleteSource(id: string): Promise<void> {
  return apiJson<void>(`${apiBase}/api/sources/${id}`, { method: 'DELETE' });
}

export async function exportFlashcardsBySubject(
  subjectId: string,
  format: 'csv' | 'json'
): Promise<string> {
  const res = await fetch(
    `${apiBase}/api/flashcards/export?subjectId=${subjectId}&format=${format}`,
    { credentials: 'include' }
  );
  if (!res.ok) {
    const err = Object.assign(new Error('api_error'), { status: res.status });
    throw err;
  }
  return res.text();
}

export async function apiAuthJson<T>(url: string, options: RequestOptions = {}): Promise<T> {
  return apiJson<T>(url, options);
}
