import { useState } from 'react';
import { Button, TextInput } from 'flowbite-react';
import { apiBase } from '../api';

export default function Login({ onToken }:{ onToken: (t:string)=>void }){
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
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
    <div className="max-w-sm mx-auto p-4 border border-gray-400 rounded-xl">
      <h2 className="text-xl font-semibold mb-3">Acceso</h2>
      <div className="grid gap-2">
        <TextInput placeholder="email" value={email} onChange={e=>setEmail(e.target.value)} />
        <TextInput placeholder="password" type="password" value={password} onChange={e=>setPassword(e.target.value)} />
        {err && <div className="text-red-400 text-sm">{err}</div>}
        <div className="flex justify-end">
          <Button onClick={login} disabled={loading} className="btn btn-primary">
            {loading ? 'Entrando...' : 'Entrar'}
          </Button>
        </div>
        <div className="text-xs text-slate-400">Introduce tus credenciales para acceder.</div>
      </div>
    </div>
  );
}
