import { useEffect, useMemo, useState } from 'react';
import { Plus, Check, CircleMinus, Pen, TrashBin, Users, Cog } from 'flowbite-react-icons/outline';
import { apiBase, apiAuthJson } from '../api';

export type AdminUser = {
  id: string;
  email: string;
  firstName?: string | null;
  lastName?: string | null;
  occupation?: string | null;
  role: 'ADMIN' | 'TEACHER' | 'STUDENT';
};

type Tab = 'general' | 'users';

type UserSettings = {
  newCardsLimit: number;
  reviewCardsLimit: number;
  penaltyRatio: number;
};

const inp = 'w-full bg-white/50 dark:bg-[#24394c] border border-secondary/30 rounded-xl px-4 py-2.5 text-sm text-text placeholder:text-text/35 focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary/50 transition-colors';
const card = 'border border-secondary/25 rounded-2xl p-6';
const btnPrimary = 'btn btn-primary rounded-full px-5 py-2 text-sm shadow-sm shadow-primary/15 flex items-center gap-2 disabled:opacity-60';
const btnOutline = 'btn btn-outline rounded-full px-5 py-2 text-sm flex items-center gap-2 disabled:opacity-50';
const btnDanger = 'inline-flex items-center gap-2 rounded-full px-5 py-2 text-sm bg-red-500 text-white hover:opacity-90 transition-opacity disabled:opacity-50';

function Th({ children, className = '' }: { children?: React.ReactNode; className?: string }) {
  return <th className={`text-left py-2 px-3 text-xs text-text/50 uppercase tracking-wide font-semibold ${className}`}>{children}</th>;
}
function Td({ children, className = '' }: { children?: React.ReactNode; className?: string }) {
  return <td className={`py-3 px-3 text-sm ${className}`}>{children}</td>;
}

function DeleteModal({
  title, body, error, loading,
  onClose, onConfirm
}: {
  title: string;
  body: React.ReactNode;
  error: string;
  loading: boolean;
  onClose: () => void;
  onConfirm: () => void;
}) {
  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 px-4">
      <div className="bg-bg border border-secondary/25 rounded-2xl p-6 w-full max-w-md shadow-xl">
        <h3 className="text-lg font-bold mb-4">{title}</h3>
        <div className="mb-4">{body}</div>
        {error && (
          <div className="mb-3 rounded-xl border border-red-400/30 bg-red-400/10 px-4 py-2 text-sm text-red-400">{error}</div>
        )}
        <div className="flex gap-2 justify-end">
          <button className={btnOutline} onClick={onClose}>
            <CircleMinus className="w-4 h-4" />
            Cancelar
          </button>
          <button className={btnDanger} disabled={loading} onClick={onConfirm}>
            <TrashBin className="w-4 h-4" />
            {loading ? 'Eliminando...' : 'Eliminar'}
          </button>
        </div>
      </div>
    </div>
  );
}

function Pagination({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (p: number) => void }) {
  if (totalPages <= 1) return null;
  return (
    <div className="flex items-center justify-center gap-3 mt-4 text-sm">
      <button disabled={page <= 1} onClick={() => onChange(page - 1)} className={btnOutline}>‹ Anterior</button>
      <span className="text-text/50">{page} / {totalPages}</span>
      <button disabled={page >= totalPages} onClick={() => onChange(page + 1)} className={btnOutline}>Siguiente ›</button>
    </div>
  );
}

export default function Settings({ isAdmin, token }: { isAdmin: boolean; token: string }) {
  const [tab, setTab] = useState<Tab>('general');

  // ── User settings ──
  const [settingsForm, setSettingsForm] = useState<UserSettings>({ newCardsLimit: 20, reviewCardsLimit: 100, penaltyRatio: 3 });
  const [settingsLoading, setSettingsLoading] = useState(false);
  const [settingsSaved, setSettingsSaved] = useState(false);
  const [settingsError, setSettingsError] = useState('');

  async function loadUserSettings() {
    try {
      const data = await apiAuthJson<UserSettings>(`${apiBase}/api/settings`, token);
      setSettingsForm(data);
    } catch { /* keep defaults */ }
  }

  async function saveUserSettings() {
    setSettingsLoading(true); setSettingsSaved(false); setSettingsError('');
    try {
      const data = await apiAuthJson<UserSettings>(`${apiBase}/api/settings`, token, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(settingsForm),
      });
      setSettingsForm(data);
      setSettingsSaved(true);
      setTimeout(() => setSettingsSaved(false), 2500);
    } catch { setSettingsError('No se pudo guardar'); }
    finally { setSettingsLoading(false); }
  }

  // ── Admin users ──
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [userPage, setUserPage] = useState(1);
  const [userTotalPages, setUserTotalPages] = useState(1);
  const [form, setForm] = useState<AdminUser>({ id: '', email: '', role: 'STUDENT' });
  const [loading, setLoading] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState<AdminUser | null>(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [deleteError, setDeleteError] = useState('');
  const isEditing = useMemo(() => Boolean(form.id), [form.id]);

  function resetForm() { setForm({ id: '', email: '', role: 'STUDENT', firstName: '', lastName: '', occupation: '' }); }

  async function loadUsers(page = userPage) {
    const data = await apiAuthJson<{ items: AdminUser[]; page: number; totalPages: number }>(`${apiBase}/api/admin/users?page=${page - 1}&size=10`, token);
    setUsers(data.items); setUserPage(data.page + 1); setUserTotalPages(data.totalPages || 1);
  }

  useEffect(() => {
    loadUserSettings();
    if (isAdmin) { loadUsers(1).catch(() => setUsers([])); }
  }, []);

  async function saveUser() {
    if (!form.email.trim()) return;
    setLoading(true);
    try {
      const body = JSON.stringify({ email: form.email, firstName: form.firstName || null, lastName: form.lastName || null, occupation: form.occupation || null, role: form.role });
      const opts = { method: isEditing ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' }, body };
      await apiAuthJson(isEditing ? `${apiBase}/api/admin/users/${form.id}` : `${apiBase}/api/admin/users`, token, opts);
      resetForm(); await loadUsers(1);
    } finally { setLoading(false); }
  }

  async function removeUser(id: string) { await apiAuthJson(`${apiBase}/api/admin/users/${id}`, token, { method: 'DELETE' }); await loadUsers(userPage); }

  const generalNavItems = [
    { id: 'general' as Tab, label: 'General', icon: <Cog className="w-4 h-4" /> },
  ];

  const adminNavItems = [
    { id: 'users' as Tab, label: 'Usuarios', icon: <Users className="w-4 h-4" /> },
  ];

  return (
    <div className="grid md:grid-cols-[200px_1fr] gap-6">

      {/* ── Sidebar ── */}
      <aside className={`${card} h-fit lg:mt-[4.5rem]`}>
        <div className="text-xs text-text/50 uppercase tracking-wide font-semibold mb-3">Ajustes</div>
        {generalNavItems.map(({ id, label, icon }) => (
          <button
            key={id}
            onClick={() => setTab(id)}
            className={`w-full text-left px-3 py-2.5 rounded-xl flex items-center gap-2.5 text-sm transition-colors mb-1 ${
              tab === id ? 'bg-primary/10 text-primary font-semibold' : 'text-text/70 hover:bg-secondary/10'
            }`}
          >
            {icon}{label}
          </button>
        ))}
        {isAdmin && (
          <>
            <div className="text-xs text-text/50 uppercase tracking-wide font-semibold mt-5 mb-3">Administración</div>
            {adminNavItems.map(({ id, label, icon }) => (
              <button
                key={id}
                onClick={() => setTab(id)}
                className={`w-full text-left px-3 py-2.5 rounded-xl flex items-center gap-2.5 text-sm transition-colors mb-1 ${
                  tab === id ? 'bg-primary/10 text-primary font-semibold' : 'text-text/70 hover:bg-secondary/10'
                }`}
              >
                {icon}{label}
              </button>
            ))}
          </>
        )}
      </aside>

      {/* ── Content ── */}
      <section className="grid gap-5">

        {/* ── General ── */}
        {tab === 'general' && (
          <div className="grid gap-5 py-[1.5rem]">
            <h2 className="text-xl font-extrabold tracking-tight">Ajustes <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">Generales</span></h2>

            <div className={card}>
              <div className="text-xs text-text/50 uppercase tracking-wide font-semibold mb-4">Flashcards</div>
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="grid gap-1.5">
                  <label className="text-sm font-medium text-text/80">Tarjetas nuevas por día</label>
                  <p className="text-xs text-text/45">Máximo de tarjetas nuevas que se introducen cada día de estudio.</p>
                  <input
                    className={inp}
                    type="number"
                    min={1}
                    max={9999}
                    value={settingsForm.newCardsLimit}
                    onChange={e => setSettingsForm(f => ({ ...f, newCardsLimit: Math.max(1, Number(e.target.value)) }))}
                  />
                </div>
                <div className="grid gap-1.5">
                  <label className="text-sm font-medium text-text/80">Repasos por día</label>
                  <p className="text-xs text-text/45">Máximo de tarjetas en repaso (due + learning) que se muestran cada día.</p>
                  <input
                    className={inp}
                    type="number"
                    min={1}
                    max={9999}
                    value={settingsForm.reviewCardsLimit}
                    onChange={e => setSettingsForm(f => ({ ...f, reviewCardsLimit: Math.max(1, Number(e.target.value)) }))}
                  />
                </div>
              </div>
            </div>

            <div className={card}>
              <div className="text-xs text-text/50 uppercase tracking-wide font-semibold mb-4">Examen</div>
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="grid gap-1.5">
                  <label className="text-sm font-medium text-text/80">Preguntas erróneas para restar 1 acierto</label>
                  <p className="text-xs text-text/45">Número de respuestas incorrectas necesarias para penalizar 1 acierto en la puntuación.</p>
                  <input
                    className={inp}
                    type="number"
                    min={1}
                    max={99}
                    value={settingsForm.penaltyRatio}
                    onChange={e => setSettingsForm(f => ({ ...f, penaltyRatio: Math.max(1, Number(e.target.value)) }))}
                  />
                </div>
              </div>
              {settingsError && (
                <div className="mt-3 rounded-xl border border-red-400/30 bg-red-400/10 px-4 py-2 text-sm text-red-400">{settingsError}</div>
              )}
              <div className="flex items-center gap-3 mt-5">
                <button onClick={saveUserSettings} disabled={settingsLoading} className={btnPrimary}>
                  <Check className="w-4 h-4" />
                  {settingsLoading ? 'Guardando...' : settingsSaved ? 'Guardado' : 'Guardar cambios'}
                </button>
                {settingsSaved && (
                  <span className="text-sm text-primary font-medium">Cambios guardados correctamente</span>
                )}
              </div>
            </div>
          </div>
        )}

        {/* ── Usuarios ── */}
        {tab === 'users' && (
          <div className="grid gap-5 py-[1.5rem]">
            <h2 className="text-xl font-extrabold tracking-tight">Gestión de <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">Usuarios</span></h2>

            <div className={card}>
              <div className="grid gap-3 sm:grid-cols-2 mb-4">
                <input className={inp} placeholder="Email" value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} />
                <select className={inp} value={form.role} onChange={e => setForm(f => ({ ...f, role: e.target.value as AdminUser['role'] }))}>
                  <option value="ADMIN">Admin</option>
                  <option value="TEACHER">Profesor</option>
                  <option value="STUDENT">Estudiante</option>
                </select>
                <input className={inp} placeholder="Nombre" value={form.firstName || ''} onChange={e => setForm(f => ({ ...f, firstName: e.target.value }))} />
                <input className={inp} placeholder="Apellidos" value={form.lastName || ''} onChange={e => setForm(f => ({ ...f, lastName: e.target.value }))} />
                <select className={inp} value={form.occupation || ''} onChange={e => setForm(f => ({ ...f, occupation: e.target.value }))}>
                  <option value="">Ocupación (opcional)</option>
                  <option value="STUDENT">Estudiante</option>
                  <option value="TEACHER">Profesor</option>
                  <option value="OPOSITOR">Opositor</option>
                  <option value="OTHER">Otro</option>
                </select>
              </div>
              <div className="flex gap-2">
                <button onClick={saveUser} disabled={loading} className={btnPrimary}>
                  {isEditing ? <Check className="w-4 h-4" /> : <Plus className="w-4 h-4" />}
                  {isEditing ? 'Guardar cambios' : 'Crear usuario'}
                </button>
                {isEditing && (
                  <button onClick={resetForm} className={btnOutline}>
                    <CircleMinus className="w-4 h-4" />Cancelar
                  </button>
                )}
              </div>
            </div>

            <div className={card}>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead><tr className="border-b border-secondary/20">
                    <Th>Email</Th>
                    <Th className="hidden sm:table-cell">Rol</Th>
                    <Th className="hidden sm:table-cell">Nombre</Th>
                    <Th />
                  </tr></thead>
                  <tbody>
                    {users.map(u => (
                      <tr key={u.id} className="border-b border-secondary/10 last:border-0">
                        <Td>{u.email}</Td>
                        <Td className="hidden sm:table-cell">{u.role}</Td>
                        <Td className="hidden sm:table-cell">{[u.firstName, u.lastName].filter(Boolean).join(' ')}</Td>
                        <Td>
                          <div className="flex gap-2">
                            <button onClick={() => setForm(u)} className="text-accent hover:opacity-70 transition-opacity" title="Editar"><Pen className="w-4 h-4" /></button>
                            <button onClick={() => setConfirmDelete(u)} className="text-red-400 hover:opacity-70 transition-opacity" title="Eliminar"><TrashBin className="w-4 h-4" /></button>
                          </div>
                        </Td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <Pagination page={userPage} totalPages={userTotalPages} onChange={p => loadUsers(p)} />
            </div>
          </div>
        )}
      </section>

      {/* ── Modal Eliminar usuario ── */}
      {confirmDelete && (
        <DeleteModal
          title="Eliminar usuario"
          body={<p className="text-sm text-text/70">¿Seguro que quieres eliminar <strong>{confirmDelete.email}</strong>?</p>}
          error={deleteError}
          loading={deleteLoading}
          onClose={() => setConfirmDelete(null)}
          onConfirm={async () => {
            setDeleteError(''); setDeleteLoading(true);
            try { await removeUser(confirmDelete.id); setConfirmDelete(null); }
            catch { setDeleteError('No se pudo eliminar'); }
            finally { setDeleteLoading(false); }
          }}
        />
      )}
    </div>
  );
}
