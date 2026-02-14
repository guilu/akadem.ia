import { useEffect, useMemo, useState } from 'react';
import { apiBase, apiAuthJson } from '../api';

export type AdminUser = {
  id: string;
  email: string;
  firstName?: string | null;
  lastName?: string | null;
  occupation?: string | null;
  role: 'ADMIN' | 'TEACHER' | 'STUDENT';
};

type Tab = 'users' | 'subjects' | 'units' | 'questions';

export default function Settings({ token }: { token: string }) {
  const [tab, setTab] = useState<Tab>('users');
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [form, setForm] = useState<AdminUser>({ id: '', email: '', role: 'STUDENT' });
  const [loading, setLoading] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState<AdminUser | null>(null);
  const isEditing = useMemo(() => Boolean(form.id), [form.id]);

  function resetForm() {
    setForm({ id: '', email: '', role: 'STUDENT', firstName: '', lastName: '', occupation: '' });
  }

  async function loadUsers() {
    const data = await apiAuthJson<AdminUser[]>(`${apiBase}/api/admin/users`, token);
    setUsers(data);
  }

  useEffect(() => {
    loadUsers().catch(() => setUsers([]));
  }, []);

  async function saveUser() {
    if (!form.email.trim()) return;
    setLoading(true);
    try {
      if (isEditing) {
        await apiAuthJson(`${apiBase}/api/admin/users/${form.id}`, token, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            email: form.email,
            firstName: form.firstName || null,
            lastName: form.lastName || null,
            occupation: form.occupation || null,
            role: form.role
          })
        });
      } else {
        await apiAuthJson(`${apiBase}/api/admin/users`, token, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            email: form.email,
            firstName: form.firstName || null,
            lastName: form.lastName || null,
            occupation: form.occupation || null,
            role: form.role
          })
        });
      }
      resetForm();
      await loadUsers();
    } finally {
      setLoading(false);
    }
  }

  async function removeUser(id: string) {
    await apiAuthJson(`${apiBase}/api/admin/users/${id}`, token, { method: 'DELETE' });
    await loadUsers();
  }

  return (
    <div className="grid md:grid-cols-[220px_1fr] gap-6">
      <aside className="border border-slate-800 rounded-xl p-3 h-fit">
        <div className="text-xs text-slate-400 mb-2">Configuración</div>
        <button className={`w-full text-left px-3 py-2 rounded ${tab === 'users' ? 'bg-slate-800' : ''}`} onClick={()=>setTab('users')}>Configurar usuarios</button>
        <button className={`w-full text-left px-3 py-2 rounded ${tab === 'subjects' ? 'bg-slate-800' : ''}`} onClick={()=>setTab('subjects')}>Configurar materias</button>
        <button className={`w-full text-left px-3 py-2 rounded ${tab === 'units' ? 'bg-slate-800' : ''}`} onClick={()=>setTab('units')}>Configurar unidades</button>
        <button className={`w-full text-left px-3 py-2 rounded ${tab === 'questions' ? 'bg-slate-800' : ''}`} onClick={()=>setTab('questions')}>Configurar preguntas</button>
      </aside>

      <section className="border border-slate-800 rounded-xl p-4">
        {tab !== 'users' && (
          <div className="text-slate-400">Próximamente…</div>
        )}

        {tab === 'users' && (
          <div className="grid gap-4">
            <h2 className="text-xl font-semibold">Usuarios</h2>
            <div className="grid gap-2 sm:grid-cols-2">
              <input className="bg-slate-900 border border-slate-700 rounded px-3 py-2" placeholder="email" value={form.email} onChange={e=>setForm(f=>({ ...f, email: e.target.value }))} />
              <select className="bg-slate-900 border border-slate-700 rounded px-3 py-2" value={form.role} onChange={e=>setForm(f=>({ ...f, role: e.target.value as AdminUser['role'] }))}>
                <option value="ADMIN">ADMIN</option>
                <option value="TEACHER">TEACHER</option>
                <option value="STUDENT">STUDENT</option>
              </select>
              <input className="bg-slate-900 border border-slate-700 rounded px-3 py-2" placeholder="nombre" value={form.firstName || ''} onChange={e=>setForm(f=>({ ...f, firstName: e.target.value }))} />
              <input className="bg-slate-900 border border-slate-700 rounded px-3 py-2" placeholder="apellidos" value={form.lastName || ''} onChange={e=>setForm(f=>({ ...f, lastName: e.target.value }))} />
              <select className="bg-slate-900 border border-slate-700 rounded px-3 py-2" value={form.occupation || ''} onChange={e=>setForm(f=>({ ...f, occupation: e.target.value }))}>
                <option value="">ocupación (opcional)</option>
                <option value="STUDENT">Estudiante</option>
                <option value="TEACHER">Profesor</option>
                <option value="OPOSITOR">Opositor</option>
                <option value="OTHER">Otro</option>
              </select>
            </div>
            <div className="flex gap-2">
              <button onClick={saveUser} disabled={loading} className="px-3 py-2 rounded bg-indigo-600 disabled:opacity-60">
                {isEditing ? 'Guardar cambios' : 'Crear usuario'}
              </button>
              {isEditing && (
                <button onClick={resetForm} className="px-3 py-2 rounded border border-slate-600">Cancelar</button>
              )}
            </div>

            <div className="border border-slate-700 rounded-xl">
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                <thead className="bg-slate-900">
                  <tr>
                    <th className="text-left p-2">Email</th>
                    <th className="text-left p-2 hidden sm:table-cell">Rol</th>
                    <th className="text-left p-2 hidden sm:table-cell">Nombre</th>
                    <th className="text-left p-2 w-12"><span className="sr-only">Acciones</span></th>
                  </tr>
                </thead>
                <tbody>
                  {users.map(u => (
                    <tr key={u.id} className="border-t border-slate-800">
                      <td className="p-2">{u.email}</td>
                      <td className="p-2 hidden sm:table-cell">{u.role}</td>
                      <td className="p-2 hidden sm:table-cell">{[u.firstName, u.lastName].filter(Boolean).join(' ')}</td>
                      <td className="p-2">
                        <div className="flex gap-2">
                          <button className="text-cyan-400" onClick={()=>setForm(u)} aria-label="Editar" title="Editar">✏️</button>
                          <button className="text-red-400" onClick={()=>setConfirmDelete(u)} aria-label="Eliminar" title="Eliminar">🗑️</button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
                </table>
              </div>
            </div>
          </div>
        )}
      </section>

      {confirmDelete && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
          <div className="bg-slate-950 border border-slate-800 rounded-xl p-5 w-full max-w-sm">
            <h3 className="text-lg font-semibold mb-2">Eliminar usuario</h3>
            <p className="text-sm text-slate-400 mb-4">¿Seguro que quieres eliminar <strong>{confirmDelete.email}</strong>?</p>
            <div className="flex gap-2 justify-end">
              <button className="px-3 py-2 rounded border border-slate-600" onClick={() => setConfirmDelete(null)}>Cancelar</button>
              <button
                className="px-3 py-2 rounded bg-red-600"
                onClick={async () => {
                  await removeUser(confirmDelete.id);
                  setConfirmDelete(null);
                }}
              >
                Eliminar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
