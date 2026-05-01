import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ROUTES } from '../constants/routes';
import {
  fetchPurchaseInfo,
  downloadUrl,
  PurchaseInfoResponse,
} from '../api/downloadApi';

type LoadState =
  | { kind: 'loading' }
  | { kind: 'ready'; info: PurchaseInfoResponse }
  | { kind: 'not-found' }
  | { kind: 'error' };

export default function DownloadPage() {
  const { token = '' } = useParams<{ token: string }>();
  const [state, setState] = useState<LoadState>({ kind: 'loading' });

  useEffect(() => {
    if (!token) {
      setState({ kind: 'not-found' });
      return;
    }
    let cancelled = false;
    fetchPurchaseInfo(token)
      .then((info) => { if (!cancelled) setState({ kind: 'ready', info }); })
      .catch((err: unknown) => {
        if (cancelled) return;
        const status = (err as { status?: number })?.status;
        setState({ kind: status === 404 ? 'not-found' : 'error' });
      });
    return () => { cancelled = true; };
  }, [token]);

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <div className="bg-card w-full max-w-lg rounded-3xl shadow-2xl p-8 border border-secondary/20">
        {state.kind === 'loading' && <LoadingView />}
        {state.kind === 'ready' && <StatusView info={state.info} token={token} />}
        {state.kind === 'not-found' && <NotFoundView />}
        {state.kind === 'error' && <GenericErrorView />}
      </div>
    </div>
  );
}

function LoadingView() {
  return (
    <div className="flex flex-col items-center gap-4 py-12" role="status" aria-live="polite">
      <svg className="animate-spin w-8 h-8 text-primary" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
      </svg>
      <p className="text-sm text-secondary">Cargando tu compra…</p>
    </div>
  );
}

function StatusView({ info, token }: { info: PurchaseInfoResponse; token: string }) {
  const registerHref = `${ROUTES.register}?email=${encodeURIComponent(info.email)}`;

  if (info.status === 'PAID') {
    return (
      <div className="space-y-6">
        <header>
          <h1 className="text-2xl font-bold tracking-tight text-text">Tu descarga está lista</h1>
          <p className="text-sm text-secondary mt-1">
            Compra confirmada para <span className="font-medium text-text">{info.email}</span>.
          </p>
        </header>
        <a
          href={downloadUrl(token)}
          target="_blank"
          rel="noopener noreferrer"
          className="w-full btn btn-primary py-4 rounded-full font-bold text-base inline-flex items-center justify-center gap-3"
        >
          Descargar {info.productName}
        </a>
        <div className="text-sm text-secondary border-t border-secondary/20 pt-6">
          <p className="mb-2 font-medium text-text">¿Quieres seguir estudiando?</p>
          <p>
            Crea una cuenta gratuita con tu email para acceder a flashcards, exámenes y seguimiento de progreso.
          </p>
          <Link
            to={registerHref}
            className="inline-block mt-3 text-primary font-semibold hover:underline"
          >
            Crear cuenta con {info.email}
          </Link>
        </div>
      </div>
    );
  }

  if (info.status === 'PENDING') {
    return (
      <div className="space-y-3">
        <h1 className="text-2xl font-bold tracking-tight text-text">Procesando tu pago</h1>
        <p className="text-sm text-secondary">
          Estamos esperando la confirmación de Stripe. En cuanto se confirme, recibirás un email con el enlace de descarga. Esta página se actualizará automáticamente al recargar.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <h1 className="text-2xl font-bold tracking-tight text-text">Pago fallido</h1>
      <p className="text-sm text-secondary">
        El pago no se completó. Si tu tarjeta fue rechazada, puedes volver a intentarlo desde la página del temario.
      </p>
    </div>
  );
}

function NotFoundView() {
  return (
    <div className="space-y-3">
      <h1 className="text-2xl font-bold tracking-tight text-text">Enlace no válido</h1>
      <p className="text-sm text-secondary">
        Este enlace de descarga no existe o ha expirado. Comprueba el correo que te enviamos o vuelve a la página del temario.
      </p>
    </div>
  );
}

function GenericErrorView() {
  return (
    <div className="space-y-3">
      <h1 className="text-2xl font-bold tracking-tight text-text">Algo ha ido mal</h1>
      <p className="text-sm text-secondary">
        No hemos podido cargar la información de tu compra. Inténtalo de nuevo en unos minutos.
      </p>
    </div>
  );
}
