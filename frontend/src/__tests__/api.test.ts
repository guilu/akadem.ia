import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  getMyProfile, updateMyProfile, apiAuthJson, apiJson, exportFlashcardsBySubject,
  uploadSource, confirmIndex, getSources, generateQuiz, getDrafts, approveDraft,
  rejectDraft, getUnitsForSubject, deleteSource
} from '../api';

// Mock global fetch
const mockFetch = vi.fn();
vi.stubGlobal('fetch', mockFetch);

function makeResponse(body: unknown, status = 200, ok = true): Response {
  return {
    ok,
    status,
    text: () => Promise.resolve(body !== undefined ? JSON.stringify(body) : ''),
    json: () => Promise.resolve(body),
  } as unknown as Response;
}

describe('getMyProfile', () => {
  beforeEach(() => {
    mockFetch.mockReset();
  });

  it('calls the correct endpoint with credentials and returns parsed profile', async () => {
    const profile = { id: 'u1', email: 'a@b.com', firstName: 'Ana', lastName: 'García' };
    mockFetch.mockResolvedValue(makeResponse(profile));

    const result = await getMyProfile();

    expect(mockFetch).toHaveBeenCalledOnce();
    const [url, options] = mockFetch.mock.calls[0];
    expect(url).toContain('/api/v1/users/me');
    expect(options.credentials).toBe('include');
    expect(result).toEqual(profile);
  });

  it('throws api_error when response is not ok', async () => {
    mockFetch.mockResolvedValue(makeResponse({ message: 'Unauthorized' }, 401, false));

    await expect(getMyProfile()).rejects.toMatchObject({ message: 'api_error', status: 401 });
  });
});

describe('updateMyProfile', () => {
  beforeEach(() => {
    mockFetch.mockReset();
  });

  it('sends PATCH with JSON body and returns updated profile', async () => {
    const updated = { id: 'u1', email: 'a@b.com', firstName: 'María', lastName: 'García' };
    mockFetch.mockResolvedValue(makeResponse(updated));

    const result = await updateMyProfile({ firstName: 'María', lastName: 'García' });

    expect(mockFetch).toHaveBeenCalledOnce();
    const [url, options] = mockFetch.mock.calls[0];
    expect(url).toContain('/api/v1/users/me');
    expect(options.method).toBe('PATCH');
    expect(options.headers['Content-Type']).toBe('application/json');
    expect(options.credentials).toBe('include');
    expect(JSON.parse(options.body)).toEqual({ firstName: 'María', lastName: 'García' });
    expect(result).toEqual(updated);
  });

  it('sends null values when firstName and lastName are null', async () => {
    const updated = { id: 'u1', email: 'a@b.com', firstName: null, lastName: null };
    mockFetch.mockResolvedValue(makeResponse(updated));

    await updateMyProfile({ firstName: null, lastName: null });

    const [, options] = mockFetch.mock.calls[0];
    expect(JSON.parse(options.body)).toEqual({ firstName: null, lastName: null });
  });

  it('throws api_error on non-ok response', async () => {
    mockFetch.mockResolvedValue(makeResponse({ message: 'Bad Request' }, 400, false));

    await expect(updateMyProfile({ firstName: null, lastName: null })).rejects.toMatchObject({
      message: 'api_error',
      status: 400,
    });
  });
});

describe('apiAuthJson', () => {
  beforeEach(() => {
    mockFetch.mockReset();
  });

  it('returns undefined for 204 response', async () => {
    mockFetch.mockResolvedValue({ ok: true, status: 204, text: () => Promise.resolve(''), json: () => Promise.resolve(undefined) } as unknown as Response);

    const result = await apiAuthJson<void>('http://localhost/test');
    expect(result).toBeUndefined();
  });

  it('returns undefined when response body is empty', async () => {
    mockFetch.mockResolvedValue({ ok: true, status: 200, text: () => Promise.resolve(''), json: () => Promise.resolve(undefined) } as unknown as Response);

    const result = await apiAuthJson<void>('http://localhost/test');
    expect(result).toBeUndefined();
  });

  it('rethrows non-AbortError errors', async () => {
    mockFetch.mockRejectedValue(new Error('network failure'));

    await expect(apiAuthJson('http://localhost/test')).rejects.toThrow('network failure');
  });

  it('converts AbortError to timeout error', async () => {
    const abortErr = Object.assign(new Error('aborted'), { name: 'AbortError' });
    mockFetch.mockRejectedValue(abortErr);

    await expect(apiAuthJson('http://localhost/test')).rejects.toMatchObject({
      message: 'timeout',
      code: 'timeout',
    });
  });
});

describe('apiJson', () => {
  beforeEach(() => {
    mockFetch.mockReset();
  });

  it('fetches and returns parsed JSON', async () => {
    const data = { foo: 'bar' };
    mockFetch.mockResolvedValue(makeResponse(data));

    const result = await apiJson<{ foo: string }>('http://localhost/data');
    expect(result).toEqual(data);
  });

  it('throws api_error on non-ok response', async () => {
    mockFetch.mockResolvedValue(makeResponse({ error: 'not found' }, 404, false));

    await expect(apiJson('http://localhost/data')).rejects.toMatchObject({ message: 'api_error', status: 404 });
  });

  it('returns undefined for 204 response', async () => {
    mockFetch.mockResolvedValue({ ok: true, status: 204, text: () => Promise.resolve(''), json: () => Promise.resolve(undefined) } as unknown as Response);

    const result = await apiJson<void>('http://localhost/data');
    expect(result).toBeUndefined();
  });

  it('converts AbortError to timeout error', async () => {
    const abortErr = Object.assign(new Error('aborted'), { name: 'AbortError' });
    mockFetch.mockRejectedValue(abortErr);

    await expect(apiJson('http://localhost/data')).rejects.toMatchObject({ message: 'timeout', code: 'timeout' });
  });
});

describe('apiAuthJson with timeoutMs', () => {
  beforeEach(() => {
    mockFetch.mockReset();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('clears timeout after successful response', async () => {
    vi.useFakeTimers();
    const clearSpy = vi.spyOn(window, 'clearTimeout');
    mockFetch.mockResolvedValue(makeResponse({ data: 'ok' }));

    const result = await apiAuthJson<{ data: string }>('http://localhost/test', { timeoutMs: 5000 });

    expect(result).toEqual({ data: 'ok' });
    expect(clearSpy).toHaveBeenCalled();
    vi.useRealTimers();
  });
});

describe('RAG API functions', () => {
  beforeEach(() => {
    mockFetch.mockReset();
  });

  it('uploadSource posts FormData and returns IndexPreview', async () => {
    const preview = { documentId: 'doc-1', units: [] };
    mockFetch.mockResolvedValue(makeResponse(preview));

    const file = new File(['content'], 'test.pdf', { type: 'application/pdf' });
    const result = await uploadSource(file, 'subj-1');

    expect(mockFetch).toHaveBeenCalledOnce();
    const [url, options] = mockFetch.mock.calls[0];
    expect(url).toContain('/api/sources');
    expect(options.method).toBe('POST');
    expect(options.credentials).toBe('include');
    expect(result).toEqual(preview);
  });

  it('uploadSource throws api_error on failure', async () => {
    mockFetch.mockResolvedValue({ ok: false, status: 400, json: () => Promise.resolve({}) } as unknown as Response);

    const file = new File([''], 'test.pdf');
    await expect(uploadSource(file, 'subj-1')).rejects.toMatchObject({ message: 'api_error', status: 400 });
  });

  it('confirmIndex posts to confirm endpoint and returns result', async () => {
    const result = { document: { id: 'doc-1' }, savedUnits: [] };
    mockFetch.mockResolvedValue(makeResponse(result));

    const res = await confirmIndex('doc-1', []);

    const [url, options] = mockFetch.mock.calls[0];
    expect(url).toContain('/api/sources/doc-1/confirm');
    expect(options.method).toBe('POST');
    expect(res).toEqual(result);
  });

  it('getSources returns list of sources', async () => {
    const sources = [{ id: 'src-1', name: 'Document' }];
    mockFetch.mockResolvedValue(makeResponse(sources));

    const result = await getSources();

    const [url] = mockFetch.mock.calls[0];
    expect(url).toContain('/api/sources');
    expect(result).toEqual(sources);
  });

  it('getSources with subjectId appends query param', async () => {
    mockFetch.mockResolvedValue(makeResponse([]));

    await getSources('subj-1');

    const [url] = mockFetch.mock.calls[0];
    expect(url).toContain('subjectId=subj-1');
  });

  it('generateQuiz posts command and returns response', async () => {
    const quizResponse = { quizId: 'q-1', questions: [] };
    mockFetch.mockResolvedValue(makeResponse(quizResponse));

    // generateQuiz uses timeoutMs internally; stub clearTimeout so it works in jsdom
    const origClearTimeout = window.clearTimeout;
    vi.stubGlobal('clearTimeout', vi.fn());

    const result = await generateQuiz({
      sourceId: 'src-1',
      unitId: 'unit-1',
      difficulty: 'MEDIUM',
      questionCount: 5,
      includeHints: false,
      storeAsDraft: false,
    });

    vi.stubGlobal('clearTimeout', origClearTimeout);

    const [url, options] = mockFetch.mock.calls[0];
    expect(url).toContain('/api/ai/quizzes/generate');
    expect(options.method).toBe('POST');
    expect(result).toEqual(quizResponse);
  });

  it('getDrafts returns filtered drafts by sourceId', async () => {
    const drafts = [{ id: 'd-1', status: 'PENDING' }];
    mockFetch.mockResolvedValue(makeResponse(drafts));

    const result = await getDrafts('src-1');

    const [url] = mockFetch.mock.calls[0];
    expect(url).toContain('sourceId=src-1');
    expect(result).toEqual(drafts);
  });

  it('getDrafts appends status query param when provided', async () => {
    mockFetch.mockResolvedValue(makeResponse([]));

    await getDrafts('src-1', 'PENDING');

    const [url] = mockFetch.mock.calls[0];
    expect(url).toContain('status=PENDING');
  });

  it('approveDraft posts to approve endpoint', async () => {
    const draft = { id: 'd-1', status: 'APPROVED' };
    mockFetch.mockResolvedValue(makeResponse(draft));

    const result = await approveDraft('d-1');

    const [url, options] = mockFetch.mock.calls[0];
    expect(url).toContain('/api/ai/drafts/d-1/approve');
    expect(options.method).toBe('POST');
    expect(result).toEqual(draft);
  });

  it('rejectDraft posts to reject endpoint', async () => {
    const draft = { id: 'd-1', status: 'REJECTED' };
    mockFetch.mockResolvedValue(makeResponse(draft));

    const result = await rejectDraft('d-1');

    const [url, options] = mockFetch.mock.calls[0];
    expect(url).toContain('/api/ai/drafts/d-1/reject');
    expect(options.method).toBe('POST');
    expect(result).toEqual(draft);
  });

  it('getUnitsForSubject returns units list', async () => {
    const units = [{ id: 'u-1', name: 'Unit 1' }];
    mockFetch.mockResolvedValue(makeResponse(units));

    const result = await getUnitsForSubject('subj-1');

    const [url] = mockFetch.mock.calls[0];
    expect(url).toContain('/api/units?subjectId=subj-1');
    expect(result).toEqual(units);
  });

  it('deleteSource sends DELETE request', async () => {
    mockFetch.mockResolvedValue({ ok: true, status: 204, text: () => Promise.resolve(''), json: () => Promise.resolve(undefined) } as unknown as Response);

    await deleteSource('src-1');

    const [url, options] = mockFetch.mock.calls[0];
    expect(url).toContain('/api/sources/src-1');
    expect(options.method).toBe('DELETE');
  });
});

describe('exportFlashcardsBySubject', () => {
  beforeEach(() => {
    mockFetch.mockReset();
  });

  it('returns text content on success', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      text: () => Promise.resolve('front,back\nHello,Hola\n'),
    } as unknown as Response);

    const result = await exportFlashcardsBySubject('subj-1', 'csv');

    expect(mockFetch).toHaveBeenCalledOnce();
    const [url, options] = mockFetch.mock.calls[0];
    expect(url).toContain('subjectId=subj-1');
    expect(url).toContain('format=csv');
    expect(options.credentials).toBe('include');
    expect(result).toBe('front,back\nHello,Hola\n');
  });

  it('returns JSON text when format is json', async () => {
    const jsonContent = '[{"front":"Hello","back":"Hola"}]';
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      text: () => Promise.resolve(jsonContent),
    } as unknown as Response);

    const result = await exportFlashcardsBySubject('subj-2', 'json');

    const [url] = mockFetch.mock.calls[0];
    expect(url).toContain('format=json');
    expect(result).toBe(jsonContent);
  });

  it('throws with status when response is not ok', async () => {
    mockFetch.mockResolvedValue({
      ok: false,
      status: 403,
    } as unknown as Response);

    await expect(exportFlashcardsBySubject('subj-3', 'csv')).rejects.toMatchObject({
      message: 'api_error',
      status: 403,
    });
  });

  it('throws network error when fetch rejects', async () => {
    mockFetch.mockRejectedValue(new Error('network failure'));

    await expect(exportFlashcardsBySubject('subj-4', 'csv')).rejects.toThrow('network failure');
  });
});
