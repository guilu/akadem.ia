// Google Analytics 4 — property "Akadem.ia" (accounts/381710780).
//
// Two rules drive this module:
//   1. The gtag script is only injected AFTER the user grants consent (RGPD).
//   2. Every path is sanitized before it leaves the browser. Two routes carry
//      secrets in the URL — /descarga/:token (purchase download token) and the
//      OAuth2 callback (?code=...) — so GA must never see the raw location.
//      This is also why "page changes based on browser history events" is
//      disabled on the data stream: page_view is sent manually from here.

export type ConsentState = 'granted' | 'denied';

export const CONSENT_STORAGE_KEY = 'akdmia.analytics.consent';

/** Ordered: first match wins. Keep in sync with the dynamic routes in App.tsx. */
const DYNAMIC_ROUTES: ReadonlyArray<readonly [RegExp, string]> = [
  [/^\/descarga\/[^/]+\/?$/, '/descarga/:token'],
  [/^\/exams\/attempts\/[^/]+\/?$/, '/exams/attempts/:attemptId'],
  [/^\/syllabuses\/[^/]+\/subjects\/?$/, '/syllabuses/:syllabusId/subjects'],
  [/^\/syllabuses\/[^/]+\/exam-builder\/?$/, '/syllabuses/:syllabusId/exam-builder'],
  [/^\/subjects\/[^/]+\/builder\/?$/, '/subjects/:subjectId/builder'],
];

declare global {
  interface Window {
    dataLayer?: unknown[];
    gtag?: (...args: unknown[]) => void;
  }
}

let initialised = false;
/** Guards against double-counting: React StrictMode remounts effects, and
 *  initAnalytics already emits the landing page_view. */
let lastTrackedPath: string | null = null;

function measurementId(): string {
  return import.meta.env.VITE_GA_MEASUREMENT_ID ?? '';
}

/**
 * Strips query string, hash and dynamic segments so no token, OAuth2 code or
 * internal id is ever sent to Google Analytics.
 */
export function sanitizePath(pathname: string): string {
  const path = pathname.split('?')[0].split('#')[0] || '/';
  const match = DYNAMIC_ROUTES.find(([pattern]) => pattern.test(path));
  return match ? match[1] : path;
}

export function getStoredConsent(): ConsentState | null {
  const stored = localStorage.getItem(CONSENT_STORAGE_KEY);
  return stored === 'granted' || stored === 'denied' ? stored : null;
}

export function setConsent(state: ConsentState): void {
  localStorage.setItem(CONSENT_STORAGE_KEY, state);
  if (state === 'granted') initAnalytics();
}

function safeLocation(path: string): string {
  return `${window.location.origin}${sanitizePath(path)}`;
}

/**
 * Injects gtag.js. No-op unless a measurement id is configured AND the user
 * granted consent. Safe to call on every mount.
 */
export function initAnalytics(): void {
  const id = measurementId();
  if (initialised || !id || getStoredConsent() !== 'granted') return;

  window.dataLayer = window.dataLayer ?? [];
  // Must push the native `arguments` object, NOT a rest-args array: gtag.js
  // runs Array.isArray() on every dataLayer entry and treats a real array as a
  // GTM-style push, so `config` never registers and no hit is ever sent.
  function gtagShim() {
    // eslint-disable-next-line prefer-rest-params
    window.dataLayer!.push(arguments);
  }
  window.gtag = gtagShim as (...args: unknown[]) => void;

  const script = document.createElement('script');
  script.async = true;
  script.src = `https://www.googletagmanager.com/gtag/js?id=${encodeURIComponent(id)}`;
  document.head.appendChild(script);

  window.gtag('js', new Date());
  window.gtag('config', id, {
    // Route tracking is manual — see trackPageView.
    send_page_view: false,
    page_location: safeLocation(window.location.pathname),
  });

  initialised = true;
  trackPageView(window.location.pathname);
}

export function trackPageView(pathname: string): void {
  if (!initialised || !window.gtag) return;
  const path = sanitizePath(pathname);
  if (path === lastTrackedPath) return;
  lastTrackedPath = path;
  window.gtag('event', 'page_view', {
    page_path: path,
    page_location: `${window.location.origin}${path}`,
    page_title: document.title,
  });
}

export function trackEvent(name: string, params: Record<string, unknown> = {}): void {
  if (!initialised || !window.gtag) return;
  window.gtag('event', name, params);
}

/** Test seam: clears the module-level init latch between test cases. */
export function resetAnalyticsForTests(): void {
  initialised = false;
  lastTrackedPath = null;
}
