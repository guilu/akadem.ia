import { useState } from 'react';
import { Button, Select, TextInput } from 'flowbite-react';
import { UserAdd } from 'flowbite-react-icons/outline';
import { apiBase } from '../api';

export default function Register({ onToken }:{ onToken: (t:string)=>void }){
  const [email, setEmail] = useState('demo@akdemya.com');
  const [password, setPassword] = useState('demo1234');
  const [confirmPassword, setConfirmPassword] = useState('demo1234');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [occupation, setOccupation] = useState('');
  const [err, setErr] = useState<string>('');
  const [loading, setLoading] = useState(false);

  function validate(){
    const emailRegex = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
    if (!email.trim()) return 'El email es obligatorio';
    if (!emailRegex.test(email)) return 'El email no es válido';
    if (!password.trim()) return 'La contraseña es obligatoria';
    if (password.length < 8) return 'La contraseña debe tener al menos 8 caracteres';
    if (!confirmPassword.trim()) return 'La confirmación de contraseña es obligatoria';
    if (password !== confirmPassword) return 'Las contraseñas no coinciden';
    return '';
  }

  async function register(){
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
      if(res.ok && data.accessToken){
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
    <div className="max-w-sm mx-auto p-4 border border-slate-700 rounded-xl">
      <h2 className="text-xl font-semibold mb-3">Registro</h2>
      <div className="grid gap-2">
        <TextInput placeholder="email" value={email} onChange={e=>setEmail(e.target.value)} />
        <TextInput placeholder="password" type="password" value={password} onChange={e=>setPassword(e.target.value)} />
        <TextInput placeholder="confirmar password" type="password" value={confirmPassword} onChange={e=>setConfirmPassword(e.target.value)} />
        <TextInput placeholder="nombre (opcional)" value={firstName} onChange={e=>setFirstName(e.target.value)} />
        <TextInput placeholder="apellidos (opcional)" value={lastName} onChange={e=>setLastName(e.target.value)} />
        <Select value={occupation} onChange={e=>setOccupation(e.target.value)}>
          <option value="">ocupación (opcional)</option>
          <option value="STUDENT">Estudiante</option>
          <option value="TEACHER">Profesor</option>
          <option value="OPOSITOR">Opositor</option>
          <option value="OTHER">Otro</option>
        </Select>
        {err && <div className="text-red-400 text-sm">{err}</div>}
        <div className="flex justify-end">
          <Button onClick={register} disabled={loading} className="btn btn-primary flex items-center gap-2">
            <UserAdd className="w-4 h-4" />
            {loading ? 'Creando...' : 'Crear cuenta'}
          </Button>
        </div>
        <div className="text-xs text-slate-400">Email obligatorio. Password mínimo 8 caracteres.</div>
      </div>
    </div>
  );
}
