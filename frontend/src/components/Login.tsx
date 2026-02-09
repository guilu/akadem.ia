import { useState } from 'react';

export default function Login({ onToken }:{ onToken: (t:string)=>void }){
  const [email, setEmail] = useState('demo@akdemya');
  const [password, setPassword] = useState('demo1234');
  const [err, setErr] = useState<string>('');

  const apiBase = import.meta.env.VITE_API_URL || `http://${window.location.hostname}:8080`;

  async function login(){
    setErr('');
    const res = await fetch(`${apiBase}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });
    const data = await res.json();
    if(res.ok){
      onToken(data.accessToken);
    } else {
      setErr(data.error || 'error');
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
          <button onClick={login} className="px-3 py-2 rounded bg-indigo-600">Entrar</button>
        </div>
        <div className="text-xs text-slate-400">Puedes usar cualquier email y contraseña (demo).</div>
      </div>
    </div>
  );
}
