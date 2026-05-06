import { useEffect, useState, FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { Elements, PaymentElement, useStripe, useElements } from '@stripe/react-stripe-js';
import mailcheck from 'mailcheck';
import { stripePromise } from '../lib/stripe';
import { createPaymentIntent } from '../api/paymentApi';
import { ROUTES } from '../constants/routes';
import MailcheckHint from './MailcheckHint';

type Step = 'email-entry' | 'loading-intent' | 'payment-confirming' | 'success';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

interface PaymentModalProps {
  isOpen: boolean;
  onClose: () => void;
  productId: string;
}

export default function PaymentModal({ isOpen, onClose, productId }: PaymentModalProps) {
  const navigate = useNavigate();

  const [step, setStep] = useState<Step>('email-entry');
  const [email, setEmail] = useState('');
  const [emailSuggestion, setEmailSuggestion] = useState<string | null>(null);
  const [clientSecret, setClientSecret] = useState<string | null>(null);
  const [downloadToken, setDownloadToken] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isOpen) {
      document.body.style.overflow = '';
      return;
    }
    document.body.style.overflow = 'hidden';
    setStep('email-entry');
    setEmail('');
    setEmailSuggestion(null);
    setClientSecret(null);
    setDownloadToken(null);
    setError(null);
    return () => { document.body.style.overflow = ''; };
  }, [isOpen]);

  if (!isOpen) return null;

  function handleEmailBlur() {
    if (!email) {
      setEmailSuggestion(null);
      return;
    }
    mailcheck.run({
      email,
      suggested: (s: { full: string }) => setEmailSuggestion(s.full),
      empty: () => setEmailSuggestion(null),
    });
  }

  async function handleEmailContinue(e: FormEvent) {
    e.preventDefault();
    if (!EMAIL_PATTERN.test(email)) {
      setError('Introduce un email válido.');
      return;
    }
    setError(null);
    setStep('loading-intent');
    try {
      const { clientSecret: cs, downloadToken: dt } = await createPaymentIntent(email, productId);
      setClientSecret(cs);
      setDownloadToken(dt);
      setStep('payment-confirming');
    } catch {
      setError('No se pudo iniciar el pago. Inténtalo de nuevo.');
      setStep('email-entry');
    }
  }

  function backToEmailStep() {
    setStep('email-entry');
    setClientSecret(null);
    setDownloadToken(null);
    setError(null);
  }

  function onPaymentSuccess() {
    setStep('success');
    if (downloadToken) {
      setTimeout(() => {
        onClose();
        navigate(ROUTES.download(downloadToken));
      }, 1200);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
      <div className="absolute inset-0" onClick={onClose} />
      <div className="relative z-10 bg-card w-full max-w-md rounded-3xl shadow-2xl p-8 border border-secondary/20 max-h-[90vh] overflow-y-auto">
        <button
          onClick={onClose}
          className="absolute top-5 right-5 text-secondary hover:text-text transition-colors"
          aria-label="Cerrar"
        >
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-6 h-6">
            <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>

        <div className="mb-8">
          <h2 className="text-2xl font-bold tracking-tight text-text">Completa tu compra</h2>
          <p className="text-sm text-secondary mt-1">Temario Subalterno GVA — Pago único 15€</p>
        </div>

        {step === 'email-entry' && (
          <form onSubmit={handleEmailContinue} className="space-y-4">
            <p className="text-sm text-text/80 leading-relaxed">
              Danos tu mejor e-mail para que podamos contactar contigo, además de descargarlo ahora mismo, te enviaremos un enlace con la descarga disponible.
            </p>
            <div className="relative">
              <span className="absolute left-4 top-1/2 -translate-y-1/2 text-secondary pointer-events-none">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5 shrink-0">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M21.75 6.75v10.5a2.25 2.25 0 01-2.25 2.25h-15a2.25 2.25 0 01-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25m19.5 0v.243a2.25 2.25 0 01-1.07 1.916l-7.5 4.615a2.25 2.25 0 01-2.36 0L3.32 8.91a2.25 2.25 0 01-1.07-1.916V6.75" />
                </svg>
              </span>
              <input
                type="email"
                value={email}
                onChange={(e) => { setEmail(e.target.value); setEmailSuggestion(null); }}
                onBlur={handleEmailBlur}
                placeholder="Introduce tu email"
                required
                aria-label="Tu email"
                className="w-full bg-white/50 dark:bg-[#24394c] border border-transparent rounded-xl pl-11 pr-4 py-4 text-sm text-text placeholder:text-text/35 focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary/50 transition-colors"
              />
            </div>
            <MailcheckHint
              suggestion={emailSuggestion}
              onAccept={(s) => { setEmail(s); setEmailSuggestion(null); }}
            />
            {error && (
              <p className="text-sm text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20 rounded-xl px-4 py-3">{error}</p>
            )}
            <button
              type="submit"
              disabled={!EMAIL_PATTERN.test(email)}
              className="w-full btn btn-primary py-4 rounded-full font-bold text-base disabled:opacity-60 disabled:cursor-not-allowed"
            >
              Continuar
            </button>
          </form>
        )}

        {step === 'loading-intent' && (
          <div className="flex flex-col items-center gap-4 py-12" role="status" aria-live="polite">
            <svg className="animate-spin w-8 h-8 text-primary" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
            </svg>
            <p className="text-sm text-secondary">Preparando el pago…</p>
          </div>
        )}

        {step === 'payment-confirming' && clientSecret && (
          <Elements stripe={stripePromise} options={{ clientSecret }}>
            <PaymentForm onSuccess={onPaymentSuccess} onBack={backToEmailStep} />
          </Elements>
        )}

        {step === 'success' && (
          <div className="flex flex-col items-center gap-4 py-8">
            <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2.5} stroke="currentColor" className="w-8 h-8 text-primary">
                <path strokeLinecap="round" strokeLinejoin="round" d="M4.5 12.75l6 6 9-13.5" />
              </svg>
            </div>
            <p className="text-lg font-bold text-text">Pago completado</p>
            <p className="text-sm text-secondary text-center">Te llevamos a tu descarga…</p>
          </div>
        )}
      </div>
    </div>
  );
}

function PaymentForm({ onSuccess, onBack }: { onSuccess: () => void; onBack: () => void }) {
  const stripe = useStripe();
  const elements = useElements();
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!stripe || !elements) return;
    setLoading(true);
    setError(null);
    const { error: confirmError } = await stripe.confirmPayment({
      elements,
      confirmParams: { return_url: window.location.href },
      redirect: 'if_required',
    });
    if (confirmError) {
      setError(confirmError.message ?? 'Error al procesar el pago.');
      setLoading(false);
      return;
    }
    setLoading(false);
    onSuccess();
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <PaymentElement />
      {error && (
        <p className="text-sm text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-900/20 rounded-xl px-4 py-3">{error}</p>
      )}
      <div className="flex gap-3">
        <button
          type="button"
          onClick={onBack}
          disabled={loading}
          className="flex-1 btn py-4 rounded-full font-semibold text-base border border-secondary/30 text-text hover:bg-secondary/10 disabled:opacity-60 disabled:cursor-not-allowed"
        >
          Volver
        </button>
        <button
          type="submit"
          disabled={!stripe || loading}
          className="flex-1 btn btn-primary py-4 rounded-full font-bold text-base disabled:opacity-60 disabled:cursor-not-allowed inline-flex items-center justify-center gap-2"
        >
          {loading ? (
            <>
              <svg className="animate-spin w-5 h-5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
              </svg>
              Procesando…
            </>
          ) : (
            'Pagar 15€'
          )}
        </button>
      </div>
    </form>
  );
}
