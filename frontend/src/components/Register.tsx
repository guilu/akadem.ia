import { useState } from 'react';
import { Link } from 'react-router-dom';
import { UserAdd } from 'flowbite-react-icons/outline';
import { apiBase } from '../api';
import { ROUTES } from '../constants/routes';

const inputClass =
  'w-full bg-white/50 dark:bg-[#24394c] border border-secondary/30 rounded-xl px-4 py-3 text-sm text-text placeholder:text-text/35 focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary/50 transition-colors';

export default function Register({ onToken }: { onToken: (t: string) => void }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [occupation, setOccupation] = useState('');
  const [err, setErr] = useState<string>('');
  const [loading, setLoading] = useState(false);

  function validate() {
    const emailRegex = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
    if (!email.trim()) return 'El email es obligatorio';
    if (!emailRegex.test(email)) return 'El email no es válido';
    if (!password.trim()) return 'La contraseña es obligatoria';
    if (password.length < 8) return 'La contraseña debe tener al menos 8 caracteres';
    if (!confirmPassword.trim()) return 'La confirmación de contraseña es obligatoria';
    if (password !== confirmPassword) return 'Las contraseñas no coinciden';
    return '';
  }

  async function register() {
    const validation = validate();
    if (validation) { setErr(validation); return; }
    setErr('');
    setLoading(true);
    try {
      const res = await fetch(`${apiBase}/api/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email,
          password,
          confirmPassword,
          firstName: firstName.trim() || null,
          lastName: lastName.trim() || null,
          occupation: occupation || null
        })
      });
      const data = await res.json().catch(() => ({}));
      if (res.ok && data.accessToken) {
        onToken(data.accessToken);
      } else {
        setErr(data.error || 'No se pudo registrar');
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
              Empieza a{' '}
              <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">
                prepararte
              </span>
            </h1>
            <p className="text-sm text-text/55">Crea tu cuenta gratis y accede a todos los simulacros</p>
          </div>

          <div className="space-y-4">
            <a
              href={`${apiBase}/oauth2/authorization/google`}
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

            <div className="flex items-center gap-3 my-1">
              <div className="flex-1 h-px bg-secondary/20" />
              <span className="text-xs text-text/40">o</span>
              <div className="flex-1 h-px bg-secondary/20" />
            </div>

            <input
              type="email"
              placeholder="Email *"
              value={email}
              onChange={e => setEmail(e.target.value)}
              className={inputClass}
            />
            <div className="grid grid-cols-2 gap-3">
              <input
                type="password"
                placeholder="Contraseña *"
                value={password}
                onChange={e => setPassword(e.target.value)}
                className={inputClass}
              />
              <input
                type="password"
                placeholder="Confirmar *"
                value={confirmPassword}
                onChange={e => setConfirmPassword(e.target.value)}
                className={inputClass}
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <input
                placeholder="Nombre"
                value={firstName}
                onChange={e => setFirstName(e.target.value)}
                className={inputClass}
              />
              <input
                placeholder="Apellidos"
                value={lastName}
                onChange={e => setLastName(e.target.value)}
                className={inputClass}
              />
            </div>
            <select
              value={occupation}
              onChange={e => setOccupation(e.target.value)}
              className={inputClass}
            >
              <option value="">Ocupación (opcional)</option>
              <option value="STUDENT">Estudiante</option>
              <option value="TEACHER">Profesor</option>
              <option value="OPOSITOR">Opositor</option>
              <option value="OTHER">Otro</option>
            </select>

            {err && (
              <div className="rounded-xl border border-primary/30 bg-primary/10 px-4 py-3 text-sm text-primary">
                {err}
              </div>
            )}

            <button
              onClick={register}
              disabled={loading}
              className="btn btn-primary rounded-full w-full py-3 text-base shadow-lg shadow-primary/20 flex items-center justify-center gap-2 disabled:opacity-60"
            >
              <UserAdd className="w-4 h-4" />
              {loading ? 'Creando cuenta...' : 'Crear cuenta gratis'}
            </button>

            <p className="text-xs text-center text-text/40">
              Email obligatorio · Contraseña mínimo 8 caracteres
            </p>
          </div>

          <p className="text-center text-sm text-text/50 mt-6">
            ¿Ya tienes cuenta?{' '}
            <Link to={ROUTES.login} className="text-primary font-semibold hover:underline">
              Inicia sesión
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
