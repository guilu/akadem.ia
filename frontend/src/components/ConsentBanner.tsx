import { useState } from 'react';
import { getStoredConsent, setConsent } from '../lib/analytics';

/**
 * RGPD consent gate for Google Analytics. The gtag script is not injected
 * until the user accepts, so declining leaves zero analytics cookies behind.
 * Renders nothing when analytics is not configured for this build.
 */
export default function ConsentBanner() {
  const configured = Boolean(import.meta.env.VITE_GA_MEASUREMENT_ID);
  const [decided, setDecided] = useState(() => getStoredConsent() !== null);

  if (!configured || decided) return null;

  function decide(state: 'granted' | 'denied') {
    setConsent(state);
    setDecided(true);
  }

  return (
    <div
      role="dialog"
      aria-label="Consentimiento de cookies"
      className="fixed bottom-0 inset-x-0 z-50 p-4 sm:p-6"
    >
      <div className="mx-auto max-w-3xl rounded-2xl border border-secondary/25 bg-bg shadow-lg p-5 flex flex-col sm:flex-row sm:items-center gap-4">
        <p className="text-sm text-text/75 flex-1">
          Usamos cookies de analítica para entender cómo se usa Akadem.ia y mejorarla. Solo se
          activan si las aceptas.
        </p>
        <div className="flex gap-3 shrink-0">
          <button
            type="button"
            onClick={() => decide('denied')}
            className="px-5 py-2 rounded-full text-sm font-semibold border border-secondary/30 text-text hover:bg-secondary/10 transition-colors"
          >
            Rechazar
          </button>
          <button
            type="button"
            onClick={() => decide('granted')}
            className="px-5 py-2 rounded-full text-sm font-semibold bg-primary text-bg hover:opacity-90 transition-opacity"
          >
            Aceptar
          </button>
        </div>
      </div>
    </div>
  );
}
