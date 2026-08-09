import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import ConsentBanner from '../components/ConsentBanner';
import { CONSENT_STORAGE_KEY, resetAnalyticsForTests } from '../lib/analytics';

beforeEach(() => {
  localStorage.clear();
  document.head.innerHTML = '';
  delete (window as unknown as { dataLayer?: unknown[] }).dataLayer;
  delete (window as unknown as { gtag?: unknown }).gtag;
  resetAnalyticsForTests();
  vi.stubEnv('VITE_GA_MEASUREMENT_ID', 'G-TESTID0001');
});

afterEach(() => {
  vi.unstubAllEnvs();
});

function gaScript() {
  return document.querySelector('script[src*="googletagmanager.com/gtag/js"]');
}

describe('ConsentBanner', () => {
  it('is shown when the user has not decided yet', () => {
    render(<ConsentBanner />);
    expect(screen.getByRole('button', { name: 'Aceptar' })).toBeDefined();
    expect(screen.getByRole('button', { name: 'Rechazar' })).toBeDefined();
  });

  it('is hidden once a decision is stored', () => {
    localStorage.setItem(CONSENT_STORAGE_KEY, 'denied');
    render(<ConsentBanner />);
    expect(screen.queryByRole('button', { name: 'Aceptar' })).toBeNull();
  });

  it('is hidden when no measurement id is configured', () => {
    vi.stubEnv('VITE_GA_MEASUREMENT_ID', '');
    render(<ConsentBanner />);
    expect(screen.queryByRole('button', { name: 'Aceptar' })).toBeNull();
  });

  it('stores consent and loads GA when the user accepts', () => {
    render(<ConsentBanner />);
    fireEvent.click(screen.getByRole('button', { name: 'Aceptar' }));

    expect(localStorage.getItem(CONSENT_STORAGE_KEY)).toBe('granted');
    expect(gaScript()).not.toBeNull();
    expect(screen.queryByRole('button', { name: 'Aceptar' })).toBeNull();
  });

  it('stores the refusal and never loads GA when the user declines', () => {
    render(<ConsentBanner />);
    fireEvent.click(screen.getByRole('button', { name: 'Rechazar' }));

    expect(localStorage.getItem(CONSENT_STORAGE_KEY)).toBe('denied');
    expect(gaScript()).toBeNull();
    expect(screen.queryByRole('button', { name: 'Rechazar' })).toBeNull();
  });
});
