import { useEffect, useState, FormEvent } from 'react';
import { Elements, PaymentElement, useStripe, useElements } from '@stripe/react-stripe-js';
import { stripePromise } from '../lib/stripe';
import { createPaymentIntent } from '../api/paymentApi';

function PaymentForm({ onSuccess }: { onSuccess: () => void }) {
  const stripe = useStripe();
  const elements = useElements();
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [succeeded, setSucceeded] = useState(false);

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

    setSucceeded(true);
    setLoading(false);
    setTimeout(onSuccess, 1500);
  }

  if (succeeded) {
    return (
      <div className="flex flex-col items-center gap-4 py-8">
        <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2.5} stroke="currentColor" className="w-8 h-8 text-primary">
            <path strokeLinecap="round" strokeLinejoin="round" d="M4.5 12.75l6 6 9-13.5" />
          </svg>
        </div>
        <p className="text-lg font-bold text-on-surface">Pago completado</p>
        <p className="text-sm text-on-surface-variant text-center">Acceso activado. ¡Bienvenido a akadem.ia!</p>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <PaymentElement />
      {error && (
        <p className="text-sm text-error bg-error/10 rounded-xl px-4 py-3">{error}</p>
      )}
      <button
        type="submit"
        disabled={!stripe || loading}
        className="w-full btn btn-primary py-4 rounded-xl font-bold text-base disabled:opacity-60 disabled:cursor-not-allowed"
      >
        {loading ? (
          <span className="flex items-center justify-center gap-2">
            <svg className="animate-spin w-5 h-5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
            </svg>
            Procesando...
          </span>
        ) : (
          'Pagar 15€'
        )}
      </button>
    </form>
  );
}

interface PaymentModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function PaymentModal({ isOpen, onClose }: PaymentModalProps) {
  const [clientSecret, setClientSecret] = useState<string | null>(null);
  const [fetchError, setFetchError] = useState<string | null>(null);

  useEffect(() => {
    if (!isOpen) {
      setClientSecret(null);
      setFetchError(null);
      return;
    }

    createPaymentIntent()
      .then(({ clientSecret: cs }) => setClientSecret(cs))
      .catch(() => setFetchError('No se pudo iniciar el pago. Inténtalo de nuevo.'));
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div
        className="absolute inset-0 bg-black/60 backdrop-blur-sm"
        onClick={onClose}
      />
      <div className="relative z-10 bg-surface w-full max-w-md rounded-3xl shadow-2xl p-8 border border-outline-variant/10">
        <button
          onClick={onClose}
          className="absolute top-5 right-5 text-on-surface-variant hover:text-on-surface transition-colors"
          aria-label="Cerrar"
        >
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-5 h-5">
            <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>

        <div className="mb-8">
          <h2 className="text-2xl font-bold tracking-tight text-on-surface">Completa tu compra</h2>
          <p className="text-sm text-on-surface-variant mt-1">Temario Subalterno GVA — Pago único 15€</p>
        </div>

        {fetchError ? (
          <div className="text-sm text-error bg-error/10 rounded-xl px-4 py-3">{fetchError}</div>
        ) : !clientSecret ? (
          <div className="flex items-center justify-center py-12">
            <svg className="animate-spin w-8 h-8 text-primary" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
            </svg>
          </div>
        ) : (
          <Elements stripe={stripePromise} options={{ clientSecret }}>
            <PaymentForm onSuccess={onClose} />
          </Elements>
        )}
      </div>
    </div>
  );
}
