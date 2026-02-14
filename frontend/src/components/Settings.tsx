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

export type AdminSubject = {
  id: string;
  name: string;
  description?: string | null;
  unitCount?: number;
};

export type AdminUnit = {
  id: string;
  subjectId: string;
  name: string;
  description?: string | null;
  orderIndex: number;
  questionCount?: number;
};

type Tab = 'users' | 'subjects' | 'units' | 'questions';

export default function Settings({ token }: { token: string }) {
  const [tab, setTab] = useState<Tab>('users');
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [form, setForm] = useState<AdminUser>({ id: '', email: '', role: 'STUDENT' });
  const [loading, setLoading] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState<AdminUser | null>(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [deleteError, setDeleteError] = useState('');
  const isEditing = useMemo(() => Boolean(form.id), [form.id]);

  const [subjects, setSubjects] = useState<AdminSubject[]>([]);
  const [subjectForm, setSubjectForm] = useState<AdminSubject>({ id: '', name: '', description: '' });
  const [subjectLoading, setSubjectLoading] = useState(false);
  const [confirmSubjectDelete, setConfirmSubjectDelete] = useState<AdminSubject | null>(null);
  const [subjectDeleteLoading, setSubjectDeleteLoading] = useState(false);
  const [subjectDeleteError, setSubjectDeleteError] = useState('');
  const [subjectDeleteText, setSubjectDeleteText] = useState('');
  const isSubjectEditing = useMemo(() => Boolean(subjectForm.id), [subjectForm.id]);

  const [units, setUnits] = useState<AdminUnit[]>([]);
  const [unitForm, setUnitForm] = useState<AdminUnit>({ id: '', subjectId: '', name: '', description: '', orderIndex: 1 });
  const [unitLoading, setUnitLoading] = useState(false);
  const [confirmUnitDelete, setConfirmUnitDelete] = useState<AdminUnit | null>(null);
  const [unitDeleteLoading, setUnitDeleteLoading] = useState(false);
  const [unitDeleteError, setUnitDeleteError] = useState('');
  const [unitDeleteText, setUnitDeleteText] = useState('');
  const isUnitEditing = useMemo(() => Boolean(unitForm.id), [unitForm.id]);

  function resetForm() {
    setForm({ id: '', email: '', role: 'STUDENT', firstName: '', lastName: '', occupation: '' });
  }

  function resetSubjectForm() {
    setSubjectForm({ id: '', name: '', description: '' });
  }

  function resetUnitForm() {
    setUnitForm({ id: '', subjectId: '', name: '', description: '', orderIndex: 1 });
  }

  async function loadUsers() {
    const data = await apiAuthJson<AdminUser[]>(`${apiBase}/api/admin/users`, token);
    setUsers(data);
  }

  async function loadSubjects() {
    const data = await apiAuthJson<AdminSubject[]>(`${apiBase}/api/admin/subjects`, token);
    setSubjects(data);
  }

  async function loadUnits(subjectId: string) {
    if (!subjectId) {
      setUnits([]);
      return;
    }
    const data = await apiAuthJson<AdminUnit[]>(`${apiBase}/api/admin/units?subjectId=${subjectId}`, token);
    setUnits(data);
  }

  useEffect(() => {
    loadUsers().catch(() => setUsers([]));
    loadSubjects().catch(() => setSubjects([]));
  }, []);

  useEffect(() => {
    if (tab === 'units') {
      loadUnits(unitForm.subjectId).catch(() => setUnits([]));
    }
  }, [tab, unitForm.subjectId]);

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

  async function saveSubject() {
    if (!subjectForm.name.trim()) return;
    setSubjectLoading(true);
    try {
      if (isSubjectEditing) {
        await apiAuthJson(`${apiBase}/api/admin/subjects/${subjectForm.id}`, token, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            name: subjectForm.name,
            description: subjectForm.description || null
          })
        });
      } else {
        await apiAuthJson(`${apiBase}/api/admin/subjects`, token, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            name: subjectForm.name,
            description: subjectForm.description || null
          })
        });
      }
      resetSubjectForm();
      await loadSubjects();
    } finally {
      setSubjectLoading(false);
    }
  }

  async function removeUser(id: string) {
    await apiAuthJson(`${apiBase}/api/admin/users/${id}`, token, { method: 'DELETE' });
    await loadUsers();
  }

  async function removeSubject(id: string) {
    await apiAuthJson(`${apiBase}/api/admin/subjects/${id}`, token, { method: 'DELETE' });
    await loadSubjects();
  }

  async function saveUnit() {
    if (!unitForm.subjectId || !unitForm.name.trim()) return;
    setUnitLoading(true);
    try {
      const payload = {
        subjectId: unitForm.subjectId,
        name: unitForm.name,
        description: unitForm.description || null,
        orderIndex: unitForm.orderIndex || 0
      };
      if (isUnitEditing) {
        await apiAuthJson(`${apiBase}/api/admin/units/${unitForm.id}`, token, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
      } else {
        await apiAuthJson(`${apiBase}/api/admin/units`, token, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
      }
      resetUnitForm();
      await loadUnits(unitForm.subjectId);
      await loadSubjects();
    } finally {
      setUnitLoading(false);
    }
  }

  async function removeUnit(id: string) {
    await apiAuthJson(`${apiBase}/api/admin/units/${id}`, token, { method: 'DELETE' });
    await loadUnits(unitForm.subjectId);
    await loadSubjects();
  }

  return (
    <div className="grid md:grid-cols-[220px_1fr] gap-6">
      <aside className="border border-slate-800 rounded-xl p-3 h-fit">
        <div className="text-xs text-slate-400 mb-2">Administración</div>
        <button className={`w-full text-left px-3 py-2 rounded ${tab === 'users' ? 'bg-slate-800' : ''}`} onClick={()=>setTab('users')}>Usuarios</button>
        <button className={`w-full text-left px-3 py-2 rounded ${tab === 'subjects' ? 'bg-slate-800' : ''}`} onClick={()=>setTab('subjects')}>Materias</button>
        <button className={`w-full text-left px-3 py-2 rounded ${tab === 'units' ? 'bg-slate-800' : ''}`} onClick={()=>setTab('units')}>Unidades</button>
        <button className={`w-full text-left px-3 py-2 rounded ${tab === 'questions' ? 'bg-slate-800' : ''}`} onClick={()=>setTab('questions')}>Preguntas</button>
      </aside>

      <section className="border border-slate-800 rounded-xl p-4">
        {tab === 'questions' && (
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
                          <button type="button" className="text-cyan-400 cursor-pointer" onClick={()=>setForm(u)} aria-label="Editar" title="Editar">✏️</button>
                          <button type="button" className="text-red-400 cursor-pointer" onClick={()=>setConfirmDelete(u)} aria-label="Eliminar" title="Eliminar">🗑️</button>
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

        {tab === 'subjects' && (
          <div className="grid gap-4">
            <h2 className="text-xl font-semibold">Materias</h2>
            <div className="grid gap-2 sm:grid-cols-2">
              <input className="bg-slate-900 border border-slate-700 rounded px-3 py-2" placeholder="nombre" value={subjectForm.name} onChange={e=>setSubjectForm(f=>({ ...f, name: e.target.value }))} />
              <input className="bg-slate-900 border border-slate-700 rounded px-3 py-2" placeholder="descripción (opcional)" value={subjectForm.description || ''} onChange={e=>setSubjectForm(f=>({ ...f, description: e.target.value }))} />
            </div>
            <div className="flex gap-2">
              <button onClick={saveSubject} disabled={subjectLoading} className="px-3 py-2 rounded bg-indigo-600 disabled:opacity-60">
                {isSubjectEditing ? 'Guardar cambios' : 'Crear materia'}
              </button>
              {isSubjectEditing && (
                <button onClick={resetSubjectForm} className="px-3 py-2 rounded border border-slate-600">Cancelar</button>
              )}
            </div>

            <div className="border border-slate-700 rounded-xl">
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="bg-slate-900">
                    <tr>
                      <th className="text-left p-2">Materia</th>
                      <th className="text-left p-2 hidden sm:table-cell">Descripción</th>
                      <th className="text-left p-2 w-12"><span className="sr-only">Acciones</span></th>
                    </tr>
                  </thead>
                  <tbody>
                    {subjects.map(s => (
                      <tr key={s.id} className="border-t border-slate-800">
                        <td className="p-2">{s.name}</td>
                        <td className="p-2 hidden sm:table-cell">{s.description || '-'}</td>
                        <td className="p-2">
                          <div className="flex gap-2">
                            <button type="button" className="text-cyan-400 cursor-pointer" onClick={()=>setSubjectForm(s)} aria-label="Editar" title="Editar">✏️</button>
                            <button
                              type="button"
                              className="text-red-400 cursor-pointer"
                              onClick={()=>{ setConfirmSubjectDelete(s); setSubjectDeleteText(''); setSubjectDeleteError(''); }}
                              aria-label="Eliminar"
                              title="Eliminar"
                            >🗑️</button>
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

        {tab === 'units' && (
          <div className="grid gap-4">
            <h2 className="text-xl font-semibold">Unidades</h2>
            <div className="grid gap-2 sm:grid-cols-2">
              <select className="bg-slate-900 border border-slate-700 rounded px-3 py-2" value={unitForm.subjectId} onChange={e=>setUnitForm(f=>({ ...f, subjectId: e.target.value }))}>
                <option value="">Selecciona materia</option>
                {subjects.map(s => (
                  <option key={s.id} value={s.id}>{s.name}</option>
                ))}
              </select>
              <input className="bg-slate-900 border border-slate-700 rounded px-3 py-2" placeholder="nombre" value={unitForm.name} onChange={e=>setUnitForm(f=>({ ...f, name: e.target.value }))} />
              <input className="bg-slate-900 border border-slate-700 rounded px-3 py-2" placeholder="descripción (opcional)" value={unitForm.description || ''} onChange={e=>setUnitForm(f=>({ ...f, description: e.target.value }))} />
              <input className="bg-slate-900 border border-slate-700 rounded px-3 py-2" type="number" placeholder="orden" value={unitForm.orderIndex} onChange={e=>setUnitForm(f=>({ ...f, orderIndex: Number(e.target.value) }))} />
            </div>
            <div className="flex gap-2">
              <button onClick={saveUnit} disabled={unitLoading} className="px-3 py-2 rounded bg-indigo-600 disabled:opacity-60">
                {isUnitEditing ? 'Guardar cambios' : 'Crear unidad'}
              </button>
              {isUnitEditing && (
                <button onClick={resetUnitForm} className="px-3 py-2 rounded border border-slate-600">Cancelar</button>
              )}
            </div>

            <div className="border border-slate-700 rounded-xl">
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="bg-slate-900">
                    <tr>
                      <th className="text-left p-2">Materia</th>
                      <th className="text-left p-2">Unidad</th>
                      <th className="text-left p-2 hidden sm:table-cell">Descripción</th>
                      <th className="text-left p-2 hidden sm:table-cell">Orden</th>
                      <th className="text-left p-2 w-12"><span className="sr-only">Acciones</span></th>
                    </tr>
                  </thead>
                  <tbody>
                    {units.map(u => (
                      <tr key={u.id} className="border-t border-slate-800">
                        <td className="p-2">{subjects.find(s => s.id === u.subjectId)?.name || '-'}</td>
                        <td className="p-2">{u.name}</td>
                        <td className="p-2 hidden sm:table-cell">{u.description || '-'}</td>
                        <td className="p-2 hidden sm:table-cell">{u.orderIndex}</td>
                        <td className="p-2">
                          <div className="flex gap-2">
                            <button type="button" className="text-cyan-400 cursor-pointer" onClick={()=>setUnitForm(u)} aria-label="Editar" title="Editar">✏️</button>
                            <button type="button" className="text-red-400 cursor-pointer" onClick={()=>{ setConfirmUnitDelete(u); setUnitDeleteError(''); setUnitDeleteText(''); }} aria-label="Eliminar" title="Eliminar">🗑️</button>
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
            {deleteError && <div className="text-sm text-red-400 mb-2">{deleteError}</div>}
            <div className="flex gap-2 justify-end">
              <button type="button" className="px-3 py-2 rounded border border-slate-600" onClick={() => setConfirmDelete(null)}>Cancelar</button>
              <button
                type="button"
                className="px-3 py-2 rounded bg-red-600 disabled:opacity-60"
                disabled={deleteLoading}
                onClick={async () => {
                  setDeleteError('');
                  setDeleteLoading(true);
                  try {
                    await removeUser(confirmDelete.id);
                    setConfirmDelete(null);
                  } catch {
                    setDeleteError('No se pudo eliminar');
                  } finally {
                    setDeleteLoading(false);
                  }
                }}
              >
                {deleteLoading ? 'Eliminando...' : 'Eliminar'}
              </button>
            </div>
          </div>
        </div>
      )}

      {confirmSubjectDelete && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
          <div className="bg-slate-950 border border-slate-800 rounded-xl p-5 w-full max-w-sm">
            <h3 className="text-lg font-semibold mb-2">Eliminar materia</h3>
            <p className="text-sm text-slate-400 mb-3">¿Seguro que quieres eliminar <strong>{confirmSubjectDelete.name}</strong>?</p>
            {confirmSubjectDelete.unitCount !== undefined && confirmSubjectDelete.unitCount > 0 && (
              <div className="text-sm text-red-400 mb-3">
                Esta materia tiene unidades asociadas, si la eliminas se eliminarán todas sus unidades y preguntas asociadas. Esta acción es irreversible.
              </div>
            )}
            {confirmSubjectDelete.unitCount !== undefined && confirmSubjectDelete.unitCount > 0 && (
              <input
                className="w-full bg-slate-900 border border-slate-700 rounded px-3 py-2 mb-3"
                placeholder='Escribe "eliminar" para confirmar'
                value={subjectDeleteText}
                onChange={e=>setSubjectDeleteText(e.target.value)}
              />
            )}
            {subjectDeleteError && <div className="text-sm text-red-400 mb-2">{subjectDeleteError}</div>}
            <div className="flex gap-2 justify-end">
              <button type="button" className="px-3 py-2 rounded border border-slate-600" onClick={() => setConfirmSubjectDelete(null)}>Cancelar</button>
              <button
                type="button"
                className="px-3 py-2 rounded bg-red-600 disabled:opacity-60"
                disabled={subjectDeleteLoading || (confirmSubjectDelete.unitCount !== undefined && confirmSubjectDelete.unitCount > 0 && subjectDeleteText.trim().toLowerCase() !== 'eliminar')}
                onClick={async () => {
                  setSubjectDeleteError('');
                  setSubjectDeleteLoading(true);
                  try {
                    await removeSubject(confirmSubjectDelete.id);
                    setConfirmSubjectDelete(null);
                  } catch {
                    setSubjectDeleteError('No se pudo eliminar');
                  } finally {
                    setSubjectDeleteLoading(false);
                  }
                }}
              >
                {subjectDeleteLoading ? 'Eliminando...' : 'Eliminar'}
              </button>
            </div>
          </div>
        </div>
      )}

      {confirmUnitDelete && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
          <div className="bg-slate-950 border border-slate-800 rounded-xl p-5 w-full max-w-sm">
            <h3 className="text-lg font-semibold mb-2">Eliminar unidad</h3>
            <p className="text-sm text-slate-400 mb-3">¿Seguro que quieres eliminar <strong>{confirmUnitDelete.name}</strong>?</p>
            {confirmUnitDelete.questionCount !== undefined && confirmUnitDelete.questionCount > 0 && (
              <div className="text-sm text-red-400 mb-3">
                Esta unidad tiene preguntas asociadas, si la eliminas se eliminarán todas sus preguntas y respuestas asociadas. Esta acción es irreversible.
              </div>
            )}
            {confirmUnitDelete.questionCount !== undefined && confirmUnitDelete.questionCount > 0 && (
              <input
                className="w-full bg-slate-900 border border-slate-700 rounded px-3 py-2 mb-3"
                placeholder='Escribe "eliminar" para confirmar'
                value={unitDeleteText}
                onChange={e=>setUnitDeleteText(e.target.value)}
              />
            )}
            {unitDeleteError && <div className="text-sm text-red-400 mb-2">{unitDeleteError}</div>}
            <div className="flex gap-2 justify-end">
              <button type="button" className="px-3 py-2 rounded border border-slate-600" onClick={() => setConfirmUnitDelete(null)}>Cancelar</button>
              <button
                type="button"
                className="px-3 py-2 rounded bg-red-600 disabled:opacity-60"
                disabled={unitDeleteLoading || (confirmUnitDelete.questionCount !== undefined && confirmUnitDelete.questionCount > 0 && unitDeleteText.trim().toLowerCase() !== 'eliminar')}
                onClick={async () => {
                  setUnitDeleteError('');
                  setUnitDeleteLoading(true);
                  try {
                    await removeUnit(confirmUnitDelete.id);
                    setConfirmUnitDelete(null);
                  } catch {
                    setUnitDeleteError('No se pudo eliminar');
                  } finally {
                    setUnitDeleteLoading(false);
                  }
                }}
              >
                {unitDeleteLoading ? 'Eliminando...' : 'Eliminar'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
