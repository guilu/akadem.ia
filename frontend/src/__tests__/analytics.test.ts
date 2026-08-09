import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import {
  CONSENT_STORAGE_KEY,
  getStoredConsent,
  initAnalytics,
  resetAnalyticsForTests,
  sanitizePath,
  setConsent,
  trackEvent,
  trackPageView,
} from '../lib/analytics';

const GA_ID = 'G-TESTID0001';

function gaScript() {
  return document.querySelector('script[src*="googletagmanager.com/gtag/js"]');
}

function dataLayer() {
  return (window as unknown as { dataLayer?: unknown[][] }).dataLayer ?? [];
}

beforeEach(() => {
  localStorage.clear();
  document.head.innerHTML = '';
  delete (window as unknown as { dataLayer?: unknown[] }).dataLayer;
  delete (window as unknown as { gtag?: unknown }).gtag;
  resetAnalyticsForTests();
  vi.stubEnv('VITE_GA_MEASUREMENT_ID', GA_ID);
});

afterEach(() => {
  vi.unstubAllEnvs();
});

describe('sanitizePath — secrets must never reach Google Analytics', () => {
  it('replaces the purchase download token with a placeholder', () => {
    expect(sanitizePath('/descarga/eyJhbGciOiJIUzI1NiJ9.secret-token')).toBe('/descarga/:token');
  });

  it('drops the query string so the OAuth2 code is never sent', () => {
    expect(sanitizePath('/oauth2/callback?code=4/0AX4XfWh-secret')).toBe('/oauth2/callback');
  });

  it('drops the hash fragment', () => {
    expect(sanitizePath('/settings#token=abc')).toBe('/settings');
  });

  it('replaces the exam attempt id', () => {
    expect(sanitizePath('/exams/attempts/7f3a-91bc')).toBe('/exams/attempts/:attemptId');
  });

  it('replaces the syllabus id in both syllabus routes', () => {
    expect(sanitizePath('/syllabuses/42/subjects')).toBe('/syllabuses/:syllabusId/subjects');
    expect(sanitizePath('/syllabuses/42/exam-builder')).toBe('/syllabuses/:syllabusId/exam-builder');
  });

  it('replaces the subject id in the builder route', () => {
    expect(sanitizePath('/subjects/99/builder')).toBe('/subjects/:subjectId/builder');
  });

  it('leaves static routes untouched', () => {
    expect(sanitizePath('/')).toBe('/');
    expect(sanitizePath('/flashcards/study')).toBe('/flashcards/study');
    expect(sanitizePath('/temario/subalterno-gva')).toBe('/temario/subalterno-gva');
  });
});

describe('consent gating', () => {
  it('returns null when the user has not decided yet', () => {
    expect(getStoredConsent()).toBeNull();
  });

  it('persists the decision', () => {
    setConsent('denied');
    expect(localStorage.getItem(CONSENT_STORAGE_KEY)).toBe('denied');
    expect(getStoredConsent()).toBe('denied');
  });

  it('does NOT load the GA script when consent is missing', () => {
    initAnalytics();
    expect(gaScript()).toBeNull();
  });

  it('does NOT load the GA script when consent is denied', () => {
    setConsent('denied');
    initAnalytics();
    expect(gaScript()).toBeNull();
  });

  it('does NOT load the GA script when the measurement id is not configured', () => {
    vi.stubEnv('VITE_GA_MEASUREMENT_ID', '');
    setConsent('granted');
    initAnalytics();
    expect(gaScript()).toBeNull();
  });

  it('loads the GA script once consent is granted', () => {
    setConsent('granted');
    initAnalytics();
    expect(gaScript()?.getAttribute('src')).toContain(GA_ID);
  });

  it('does not inject the script twice', () => {
    setConsent('granted');
    initAnalytics();
    initAnalytics();
    expect(document.querySelectorAll('script[src*="googletagmanager.com/gtag/js"]').length).toBe(1);
  });

  it('pushes the native arguments object, not an array', () => {
    // gtag.js checks Array.isArray() on each dataLayer entry: a real array is
    // treated as a GTM-style push, so `config` never registers and no hit is
    // ever sent. Pushing `arguments` is the only form gtag.js acts on.
    setConsent('granted');
    initAnalytics();

    const raw = (window as unknown as { dataLayer: unknown[] }).dataLayer;
    expect(raw.length).toBeGreaterThan(0);
    expect(Array.isArray(raw[0])).toBe(false);
    expect(Object.prototype.toString.call(raw[0])).toBe('[object Arguments]');
  });

  it('configures GA with send_page_view disabled so route tracking stays manual', () => {
    setConsent('granted');
    initAnalytics();
    const config = dataLayer().find((entry) => entry[0] === 'config');
    expect(config?.[1]).toBe(GA_ID);
    expect((config?.[2] as { send_page_view?: boolean })?.send_page_view).toBe(false);
  });
});

describe('trackPageView', () => {
  it('is a no-op before consent is granted', () => {
    trackPageView('/flashcards');
    expect(dataLayer().some((entry) => entry[1] === 'page_view')).toBe(false);
  });

  it('sends a sanitized page_path and page_location', () => {
    setConsent('granted');
    initAnalytics();
    trackPageView('/descarga/super-secret-token');

    // initAnalytics already emits the landing page_view — assert on the last one.
    const pageView = dataLayer().filter((entry) => entry[1] === 'page_view').pop();
    const params = pageView?.[2] as { page_path: string; page_location: string };
    expect(params.page_path).toBe('/descarga/:token');
    expect(params.page_location).toBe(`${window.location.origin}/descarga/:token`);
    expect(params.page_location).not.toContain('super-secret-token');
  });
});

describe('trackPageView deduplication', () => {
  function pageViews() {
    return dataLayer().filter((entry) => entry[1] === 'page_view');
  }

  it('does not re-send the same path twice in a row (StrictMode / init overlap)', () => {
    setConsent('granted');
    initAnalytics();
    const afterInit = pageViews().length;

    trackPageView(window.location.pathname);
    expect(pageViews().length).toBe(afterInit);
  });

  it('sends again when the path actually changes, and on returning to a previous path', () => {
    setConsent('granted');
    initAnalytics();
    const afterInit = pageViews().length;

    trackPageView('/flashcards');
    trackPageView('/settings');
    trackPageView('/flashcards');
    expect(pageViews().length).toBe(afterInit + 3);
  });
});

describe('trackEvent', () => {
  it('is a no-op before consent is granted', () => {
    trackEvent('purchase_started');
    expect(dataLayer().some((entry) => entry[1] === 'purchase_started')).toBe(false);
  });

  it('forwards the event name and params once initialised', () => {
    setConsent('granted');
    initAnalytics();
    trackEvent('purchase_started', { value: 15 });

    const event = dataLayer().find((entry) => entry[1] === 'purchase_started');
    expect(event?.[2]).toEqual({ value: 15 });
  });
});
