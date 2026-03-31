import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { getMyProfile, updateMyProfile, apiAuthJson, apiJson } from '../api';

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

  it('calls the correct endpoint with Authorization header and returns parsed profile', async () => {
    const profile = { id: 'u1', email: 'a@b.com', firstName: 'Ana', lastName: 'García' };
    mockFetch.mockResolvedValue(makeResponse(profile));

    const result = await getMyProfile('my-token');

    expect(mockFetch).toHaveBeenCalledOnce();
    const [url, options] = mockFetch.mock.calls[0];
    expect(url).toContain('/api/v1/users/me');
    expect(options.headers.Authorization).toBe('Bearer my-token');
    expect(result).toEqual(profile);
  });

  it('throws api_error when response is not ok', async () => {
    mockFetch.mockResolvedValue(makeResponse({ message: 'Unauthorized' }, 401, false));

    await expect(getMyProfile('bad-token')).rejects.toMatchObject({ message: 'api_error', status: 401 });
  });
});

describe('updateMyProfile', () => {
  beforeEach(() => {
    mockFetch.mockReset();
  });

  it('sends PATCH with JSON body and returns updated profile', async () => {
    const updated = { id: 'u1', email: 'a@b.com', firstName: 'María', lastName: 'García' };
    mockFetch.mockResolvedValue(makeResponse(updated));

    const result = await updateMyProfile('tok', { firstName: 'María', lastName: 'García' });

    expect(mockFetch).toHaveBeenCalledOnce();
    const [url, options] = mockFetch.mock.calls[0];
    expect(url).toContain('/api/v1/users/me');
    expect(options.method).toBe('PATCH');
    expect(options.headers['Content-Type']).toBe('application/json');
    expect(options.headers.Authorization).toBe('Bearer tok');
    expect(JSON.parse(options.body)).toEqual({ firstName: 'María', lastName: 'García' });
    expect(result).toEqual(updated);
  });

  it('sends null values when firstName and lastName are null', async () => {
    const updated = { id: 'u1', email: 'a@b.com', firstName: null, lastName: null };
    mockFetch.mockResolvedValue(makeResponse(updated));

    await updateMyProfile('tok', { firstName: null, lastName: null });

    const [, options] = mockFetch.mock.calls[0];
    expect(JSON.parse(options.body)).toEqual({ firstName: null, lastName: null });
  });

  it('throws api_error on non-ok response', async () => {
    mockFetch.mockResolvedValue(makeResponse({ message: 'Bad Request' }, 400, false));

    await expect(updateMyProfile('tok', { firstName: null, lastName: null })).rejects.toMatchObject({
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

    const result = await apiAuthJson<void>('http://localhost/test', 'token');
    expect(result).toBeUndefined();
  });

  it('returns undefined when response body is empty', async () => {
    mockFetch.mockResolvedValue({ ok: true, status: 200, text: () => Promise.resolve(''), json: () => Promise.resolve(undefined) } as unknown as Response);

    const result = await apiAuthJson<void>('http://localhost/test', 'token');
    expect(result).toBeUndefined();
  });

  it('rethrows non-AbortError errors', async () => {
    mockFetch.mockRejectedValue(new Error('network failure'));

    await expect(apiAuthJson('http://localhost/test', 'token')).rejects.toThrow('network failure');
  });

  it('converts AbortError to timeout error', async () => {
    const abortErr = Object.assign(new Error('aborted'), { name: 'AbortError' });
    mockFetch.mockRejectedValue(abortErr);

    await expect(apiAuthJson('http://localhost/test', 'token')).rejects.toMatchObject({
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
