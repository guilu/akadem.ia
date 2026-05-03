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
    let timer: ReturnType<typeof setTimeout> | undefined;
    let pollAttempts = 0;
    const MAX_POLL_ATTEMPTS = 15;
    const POLL_INTERVAL_MS = 2000;

    async function load(isPoll: boolean) {
      try {
        const info = await fetchPurchaseInfo(token);
        if (cancelled) return;
        setState({ kind: 'ready', info });
        if (info.status === 'PENDING' && pollAttempts < MAX_POLL_ATTEMPTS) {
          pollAttempts += 1;
          timer = setTimeout(() => load(true), POLL_INTERVAL_MS);
        }
      } catch (err: unknown) {
        if (cancelled) return;
        if (isPoll && pollAttempts < MAX_POLL_ATTEMPTS) {
          pollAttempts += 1;
          timer = setTimeout(() => load(true), POLL_INTERVAL_MS);
          return;
        }
        const status = (err as { status?: number })?.status;
        setState({ kind: status === 404 ? 'not-found' : 'error' });
      }
    }

    load(false);

    return () => {
      cancelled = true;
      if (timer) clearTimeout(timer);
    };
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
      {state.kind === 'not-found' && <NotFoundView />}
      {state.kind === 'error' && <GenericErrorView />}
    </div>
  );
}

function CenteredCard({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex justify-center px-4 py-10">
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
    <main className="max-w-4xl mx-auto flex flex-col items-center px-6 py-10">
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
    <main className="max-w-4xl mx-auto px-6 py-10 flex flex-col items-center text-center">
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
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-8 h-8 shrink-0">
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
          className="btn btn-secondary w-full sm:w-auto sm:inline-flex max-w-full px-8 py-4 rounded-xl font-bold text-center break-words"
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
    <main className="relative overflow-hidden flex justify-center px-4 py-10">
      <div className="pointer-events-none absolute top-0 right-0 -mr-24 -mt-24 w-96 h-96 bg-primary/5 rounded-full blur-3xl" />
      <div className="pointer-events-none absolute bottom-0 left-0 -ml-24 -mb-24 w-80 h-80 bg-accent/5 rounded-full blur-3xl" />
      <section className="max-w-xl w-full text-center relative z-10">
        <div className="flex justify-center mb-10">
          <div className="relative">
            <div className="absolute inset-0 bg-primary/10 rounded-full scale-125" />
            <div className="w-24 h-24 bg-primary text-bg rounded-full flex items-center justify-center shadow-2xl shadow-primary/30 relative z-10">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-12 h-12">
                <path fillRule="evenodd" d="M9.401 3.003c1.155-2 4.043-2 5.197 0l7.355 12.748c1.154 2-.29 4.5-2.599 4.5H4.645c-2.309 0-3.752-2.5-2.598-4.5L9.4 3.003zM12 8.25a.75.75 0 01.75.75v3.75a.75.75 0 01-1.5 0V9a.75.75 0 01.75-.75zm0 8.25a.75.75 0 100-1.5.75.75 0 000 1.5z" clipRule="evenodd" />
              </svg>
            </div>
          </div>
        </div>
        <h1 className="text-4xl md:text-5xl font-extrabold text-text tracking-tight leading-tight mb-6">
          Enlace no válido
        </h1>
        <p className="text-lg text-secondary font-medium leading-relaxed mb-10 px-4 md:px-8">
          Este enlace de descarga no existe o ha expirado. Comprueba el correo que te enviamos o vuelve a la página del temario.
        </p>
        <div className="flex flex-col items-center gap-6">
          <Link
            to={ROUTES.subalternoGva}
            className="inline-flex items-center justify-center bg-primary text-bg px-10 py-4 rounded-xl text-lg font-bold shadow-lg shadow-primary/20 hover:opacity-95 active:scale-[0.98] transition-all w-full sm:w-auto"
          >
            Volver al temario
          </Link>
          <p className="text-secondary text-sm font-medium">
            ¿Tienes problemas con tu compra?{' '}
            <a className="text-primary font-bold hover:underline ml-1" href={`mailto:soporte@akadem.ia?subject=${encodeURIComponent('Enlace de descarga no válido')}`}>
              Contactar con soporte
            </a>
          </p>
        </div>
      </section>
    </main>
  );
}

function GenericErrorView() {
  return (
    <main className="flex justify-center px-6 py-10">
      <div className="max-w-2xl w-full text-center space-y-8">
        <div className="relative inline-flex items-center justify-center">
          <div className="absolute inset-0 bg-primary/10 blur-3xl rounded-full" />
          <div className="relative bg-card p-8 rounded-full border border-primary/20 shadow-xl shadow-primary/5">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-16 h-16 text-primary">
              <path fillRule="evenodd" d="M12 2.25c-5.385 0-9.75 4.365-9.75 9.75s4.365 9.75 9.75 9.75 9.75-4.365 9.75-9.75S17.385 2.25 12 2.25zm-1.72 6.97a.75.75 0 10-1.06 1.06L10.94 12l-1.72 1.72a.75.75 0 101.06 1.06L12 13.06l1.72 1.72a.75.75 0 101.06-1.06L13.06 12l1.72-1.72a.75.75 0 10-1.06-1.06L12 10.94l-1.72-1.72z" clipRule="evenodd" />
            </svg>
          </div>
        </div>
        <div className="space-y-4">
          <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight text-text leading-tight">
            Algo ha ido mal
          </h1>
          <p className="text-lg text-secondary max-w-xl mx-auto leading-relaxed">
            No hemos podido cargar la información de tu compra. No te preocupes, no se ha realizado ningún cargo en tu cuenta. Inténtalo de nuevo en unos minutos.
          </p>
        </div>
        <div className="flex flex-col items-center gap-6 pt-2">
          <button
            type="button"
            onClick={() => window.location.reload()}
            className="bg-primary text-bg px-10 py-4 rounded-xl text-lg font-bold shadow-lg shadow-primary/20 hover:opacity-95 active:scale-[0.98] transition-all w-full sm:w-auto"
          >
            Intentar de nuevo
          </button>
          <p className="text-sm text-secondary">
            ¿Has intentado varias veces y sigue fallando?{' '}
            <a className="text-primary font-semibold hover:underline" href={`mailto:soporte@akadem.ia?subject=${encodeURIComponent('Error al cargar la información de compra')}`}>
              Contactar con soporte
            </a>
          </p>
        </div>
        <div className="mt-8 bg-card rounded-2xl p-6 border border-secondary/20 max-w-md mx-auto">
          <div className="flex items-start gap-4 text-left">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5 text-secondary mt-1 shrink-0">
              <path fillRule="evenodd" d="M2.25 12c0-5.385 4.365-9.75 9.75-9.75s9.75 4.365 9.75 9.75-4.365 9.75-9.75 9.75S2.25 17.385 2.25 12zM12 8.25a.75.75 0 01.75.75v3.75a.75.75 0 01-1.5 0V9a.75.75 0 01.75-.75zm0 8.25a.75.75 0 100-1.5.75.75 0 000 1.5z" clipRule="evenodd" />
            </svg>
            <div>
              <h4 className="text-xs uppercase tracking-widest text-secondary font-bold mb-1">Información de seguridad</h4>
              <p className="text-xs text-secondary leading-relaxed">
                Nuestras pasarelas de pago están cifradas y protegidas. Si el problema persiste, es posible que tu entidad bancaria haya bloqueado el movimiento por seguridad.
              </p>
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}
