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
    <div className="min-h-screen bg-bg text-text">
      {state.kind === 'loading' && <CenteredCard><LoadingView /></CenteredCard>}
      {state.kind === 'ready' && state.info.status === 'PAID' && (
        <ReadyView info={state.info} token={token} />
      )}
      {state.kind === 'ready' && state.info.status === 'PENDING' && <ProcessingView />}
      {state.kind === 'ready' && state.info.status === 'FAILED' && (
        <CenteredCard><FailedView /></CenteredCard>
      )}
      {state.kind === 'not-found' && <CenteredCard><NotFoundView /></CenteredCard>}
      {state.kind === 'error' && <CenteredCard><GenericErrorView /></CenteredCard>}
    </div>
  );
}

function CenteredCard({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <div className="bg-card w-full max-w-lg rounded-3xl shadow-2xl p-8 border border-secondary/20">
        {children}
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

function ProcessingView() {
  return (
    <main className="min-h-screen flex flex-col items-center justify-center px-6 py-12">
      <div className="max-w-md w-full text-center space-y-8">
        <div className="relative w-24 h-24 mx-auto" role="status" aria-live="polite">
          <div className="absolute inset-0 rounded-full border-4 border-primary/10" />
          <div className="absolute inset-0 rounded-full border-4 border-transparent border-t-primary animate-spin" />
          <div className="absolute inset-0 flex items-center justify-center">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-9 h-9 text-primary">
              <path fillRule="evenodd" d="M2.25 8.25A2.25 2.25 0 014.5 6h15a2.25 2.25 0 012.25 2.25v.75H2.25v-.75zm0 3.75v6A2.25 2.25 0 004.5 20.25h15A2.25 2.25 0 0021.75 18v-6H2.25zm3 4.5a.75.75 0 01.75-.75h3a.75.75 0 010 1.5H6a.75.75 0 01-.75-.75z" clipRule="evenodd" />
            </svg>
          </div>
        </div>
        <div className="space-y-4">
          <h1 className="text-3xl font-extrabold tracking-tight text-text">Procesando tu pago…</h1>
          <p className="text-lg text-secondary leading-relaxed">
            Estamos esperando la confirmación de Stripe. En cuanto se confirme, recibirás un email con el enlace de descarga. Esta página se actualizará automáticamente al recargar.
          </p>
        </div>
        <div className="w-full bg-card h-1.5 rounded-full overflow-hidden border border-secondary/10">
          <div className="bg-primary h-full w-2/3 rounded-full animate-pulse" />
        </div>
        <div className="pt-4 flex items-center justify-center space-x-2 text-sm font-medium text-secondary/70">
          <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm4.59-12.42L10 14.17l-2.59-2.58L6 13l4 4 8-8z" />
          </svg>
          <span>Conexión segura cifrada</span>
        </div>
      </div>
    </main>
  );
}

function ReadyView({ info, token }: { info: PurchaseInfoResponse; token: string }) {
  const registerHref = `${ROUTES.register}?email=${encodeURIComponent(info.email)}`;

  return (
    <main className="max-w-4xl mx-auto px-6 py-20 flex flex-col items-center text-center">
      <div className="mb-12">
        <div className="w-24 h-24 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-8 animate-bounce">
          <svg className="w-12 h-12 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
            <path d="M5 13l4 4L19 7" strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" />
          </svg>
        </div>
        <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight mb-4 text-text">
          ¡Tu descarga está lista!
        </h1>
        <p className="text-xl text-secondary max-w-lg mx-auto leading-relaxed">
          Compra confirmada para <span className="font-medium text-text">{info.email}</span>.
          Hemos enviado el enlace también a tu correo.
        </p>
      </div>

      <div className="w-full max-w-md space-y-6">
        <a
          href={downloadUrl(token)}
          target="_blank"
          rel="noopener noreferrer"
          className="w-full bg-primary text-bg px-10 py-5 rounded-2xl font-bold text-lg hover:opacity-95 transition-all active:scale-[0.98] shadow-xl shadow-primary/20 flex items-center justify-center gap-3"
        >
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-6 h-6">
            <path strokeLinecap="round" strokeLinejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3" />
          </svg>
          Descargar {info.productName}
        </a>
      </div>

      <div className="mt-16 p-10 bg-card rounded-[2.5rem] w-full max-w-2xl border border-secondary/20">
        <h2 className="text-2xl font-bold mb-4 text-text">¿Quieres seguir estudiando?</h2>
        <p className="text-secondary mb-8 leading-relaxed">
          Crea una cuenta gratuita con tu email para acceder a flashcards, exámenes y seguimiento de progreso.
        </p>
        <Link
          to={registerHref}
          className="inline-flex items-center justify-center bg-card text-text border border-secondary/40 px-8 py-4 rounded-xl font-bold hover:bg-secondary/10 transition-colors"
        >
          Crear cuenta con {info.email}
        </Link>
      </div>

      <p className="mt-12 text-sm text-secondary">
        ¿Tienes problemas con la descarga?{' '}
        <a className="text-primary font-semibold hover:underline" href={`mailto:soporte@akadem.ia?subject=${encodeURIComponent('Problema con descarga del temario')}`}>
          Contactar con soporte
        </a>
      </p>
    </main>
  );
}

function FailedView() {
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
