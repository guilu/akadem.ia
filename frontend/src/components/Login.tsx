import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRightToBracket } from 'flowbite-react-icons/outline';
import { apiBase } from '../api';
import { ROUTES } from '../constants/routes';

const inputClass =
  'w-full bg-bg border border-secondary/30 rounded-xl px-4 py-3 text-sm text-text placeholder:text-text/35 focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary/50 transition-colors';

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
