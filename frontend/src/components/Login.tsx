import { useState } from 'react';
import { apiBase } from '../api';

export default function Login({ onToken }:{ onToken: (t:string)=>void }){
  const [email, setEmail] = useState('demo@akdemya');
  const [password, setPassword] = useState('demo1234');
  const [err, setErr] = useState<string>('');
  const [loading, setLoading] = useState(false);

  function validate(){
    const emailRegex = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
    if (!email.trim()) return 'El email es obligatorio';
    if (!emailRegex.test(email)) return 'El email no es válido';
    if (!password.trim()) return 'La contraseña es obligatoria';
    return '';
  }

  async function login(){
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
      if(res.ok && data.accessToken){
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
    <div className="max-w-sm mx-auto p-4 border border-slate-700 rounded-xl">
      <h2 className="text-xl font-semibold mb-3">Acceso</h2>
      <div className="grid gap-2">
        <input className="bg-slate-900 border border-slate-700 rounded px-3 py-2" placeholder="email"
               value={email} onChange={e=>setEmail(e.target.value)} />
        <input className="bg-slate-900 border border-slate-700 rounded px-3 py-2" placeholder="password" type="password"
               value={password} onChange={e=>setPassword(e.target.value)} />
        {err && <div className="text-red-400 text-sm">{err}</div>}
        <div className="flex gap-2">
          <button onClick={login} disabled={loading} className="px-3 py-2 rounded bg-indigo-600 disabled:opacity-60">
            {loading ? 'Entrando...' : 'Entrar'}
          </button>
        </div>
        <div className="text-xs text-slate-400">Introduce tus credenciales para acceder.</div>
      </div>
    </div>
  );
}
