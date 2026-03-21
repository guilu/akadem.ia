import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRightToBracket } from 'flowbite-react-icons/outline';
import { apiBase } from '../api';
import { ROUTES } from '../constants/routes';

const inputClass =
  'w-full bg-white/50 dark:bg-[#24394c] border border-secondary/30 rounded-xl px-4 py-3 text-sm text-text placeholder:text-text/35 focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary/50 transition-colors';

export default function Login({ onToken }: { onToken: (t: string) => void }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [err, setErr] = useState<string>('');
  const [loading, setLoading] = useState(false);

  function validate() {
    const emailRegex = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
    if (!email.trim()) return 'El email es obligatorio';
    if (!emailRegex.test(email)) return 'El email no es válido';
    if (!password.trim()) return 'La contraseña es obligatoria';
    return '';
  }

  async function login() {
    const validation = validate();
    if (validation) { setErr(validation); return; }
    setErr('');
    setLoading(true);
    try {
      const res = await fetch(`${apiBase}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });
      const data = await res.json().catch(() => ({}));
      if (res.ok && data.accessToken) {
        onToken(data.accessToken);
      } else {
        setErr(data.error || 'Credenciales inválidas');
      }
    } catch {
      setErr('No se pudo conectar con el servidor');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="relative overflow-hidden min-h-[calc(100vh-4rem)] flex items-top justify-center px-6 py-16">
      <div className="pointer-events-none absolute inset-0 bg-gradient-to-br from-primary/10 via-transparent to-accent/15" />
      <div className="pointer-events-none absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[500px] h-[500px] rounded-full bg-primary/5 blur-3xl" />

      <div className="relative w-full max-w-md">
        <div className="border border-secondary/25 rounded-2xl p-8 bg-bg/60 backdrop-blur-sm">
          <div className="text-center mb-8">
            <h1 className="text-3xl font-extrabold tracking-tight mb-2">
              Bienvenido de{' '}
              <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">
                vuelta
              </span>
            </h1>
            <p className="text-sm text-text/55">Accede a tu cuenta para continuar preparándote</p>
          </div>

          <div className="space-y-4">
            <a
              href={`${apiBase}/api/oauth2/authorization/google`}
              className="flex items-center justify-center gap-3 w-full py-3 px-4 rounded-full border border-secondary/30 bg-bg hover:bg-secondary/10 transition-colors text-sm font-medium text-text/80"
            >
              <svg className="w-4 h-4 shrink-0" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
                <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
                <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z" fill="#FBBC05"/>
                <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
              </svg>
              Continuar con Google
            </a>

            <div className="flex items-center gap-3">
              <div className="flex-1 h-px bg-secondary/20" />
              <span className="text-xs text-text/40">o</span>
              <div className="flex-1 h-px bg-secondary/20" />
            </div>

            <div>
              <label className="block text-sm font-medium text-text/70 mb-1.5">Email</label>
              <input
                type="email"
                placeholder="tu@email.com"
                value={email}
                onChange={e => setEmail(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && login()}
                className={inputClass}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-text/70 mb-1.5">Contraseña</label>
              <input
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={e => setPassword(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && login()}
                className={inputClass}
              />
            </div>

            {err && (
              <div className="rounded-xl border border-primary/30 bg-primary/10 px-4 py-3 text-sm text-primary">
                {err}
              </div>
            )}

            <button
              onClick={login}
              disabled={loading}
              className="btn btn-primary rounded-full w-full py-3 text-base shadow-lg shadow-primary/20 flex items-center justify-center gap-2 disabled:opacity-60"
            >
              <ArrowRightToBracket className="w-4 h-4" />
              {loading ? 'Entrando...' : 'Entrar'}
            </button>
          </div>

          <p className="text-center text-sm text-text/50 mt-6">
            ¿No tienes cuenta?{' '}
            <Link to={ROUTES.register} className="text-primary font-semibold hover:underline">
              Regístrate gratis
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
