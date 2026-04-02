import { useEffect, useMemo, useState } from 'react';
import { Plus, Check, CircleMinus, Pen, TrashBin, FileExport, FileImport, FileCsv, Users, BookOpen, FolderOpen, FileLines, Cog } from 'flowbite-react-icons/outline';
import {
  getUserSettings, updateUserSettings,
  getAdminUsers, saveAdminUser, deleteAdminUser,
  getAdminSubjects, saveAdminSubject, deleteAdminSubject,
  getAdminUnits, saveAdminUnit, deleteAdminUnit,
  getAdminQuestions, saveAdminQuestion, deleteAdminQuestion,
  exportAdminQuestions, importAdminQuestions,
} from '../api';

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

export type AdminAnswer = { id?: string; text: string; correct: boolean };
export type AdminQuestion = {
  id: string;
  unitId: string;
  text: string;
  explanation?: string | null;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  answers: AdminAnswer[];
};

type Tab = 'general' | 'users' | 'subjects' | 'units' | 'questions';

type UserSettings = {
  newCardsLimit: number;
  reviewCardsLimit: number;
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
  title, body, error, loading, requireText,
  onClose, onConfirm
}: {
  title: string;
  body: React.ReactNode;
  error: string;
  loading: boolean;
  requireText?: { value: string; onChange: (v: string) => void; passes: boolean };
  onClose: () => void;
  onConfirm: () => void;
}) {
  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 px-4">
      <div className="bg-bg border border-secondary/25 rounded-2xl p-6 w-full max-w-md shadow-xl">
        <h3 className="text-lg font-bold mb-4">{title}</h3>
        <div className="mb-4">{body}</div>
        {requireText && (
          <input
            className={inp + ' mb-3'}
            placeholder='Escribe "eliminar" para confirmar'
            value={requireText.value}
            onChange={e => requireText.onChange(e.target.value)}
          />
        )}
        {error && (
          <div className="mb-3 rounded-xl border border-red-400/30 bg-red-400/10 px-4 py-2 text-sm text-red-400">{error}</div>
        )}
        <div className="flex gap-2 justify-end">
          <button className={btnOutline} onClick={onClose}>
            <CircleMinus className="w-4 h-4" />
            Cancelar
          </button>
          <button
            className={btnDanger}
            disabled={loading || (requireText ? !requireText.passes : false)}
            onClick={onConfirm}
          >
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

export default function Settings({ isAdmin, token, onSubjectsChanged }: { isAdmin: boolean; token: string; onSubjectsChanged?: () => void }) {
  const [tab, setTab] = useState<Tab>('general');

  // ── User settings ──
  const [settingsForm, setSettingsForm] = useState<UserSettings>({ newCardsLimit: 20, reviewCardsLimit: 100 });
  const [settingsLoading, setSettingsLoading] = useState(false);
  const [settingsSaved, setSettingsSaved] = useState(false);
  const [settingsError, setSettingsError] = useState('');

  async function loadUserSettings() {
    try {
      const data = await getUserSettings(token);
      setSettingsForm(data);
    } catch { /* keep defaults */ }
  }

  async function saveUserSettings() {
    setSettingsLoading(true); setSettingsSaved(false); setSettingsError('');
    try {
      const data = await updateUserSettings(token, settingsForm);
      setSettingsForm(data);
      setSettingsSaved(true);
      setTimeout(() => setSettingsSaved(false), 2500);
    } catch { setSettingsError('No se pudo guardar'); }
    finally { setSettingsLoading(false); }
  }

  const [users, setUsers] = useState<AdminUser[]>([]);
  const [userPage, setUserPage] = useState(1);
  const [userTotalPages, setUserTotalPages] = useState(1);
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

  const [questions, setQuestions] = useState<AdminQuestion[]>([]);
  const [questionPage, setQuestionPage] = useState(1);
  const [questionTotalPages, setQuestionTotalPages] = useState(1);
  const [questionForm, setQuestionForm] = useState<AdminQuestion>({
    id: '', unitId: '', text: '', explanation: '', difficulty: 'EASY',
    answers: [
      { text: '', correct: true },
      { text: '', correct: false },
      { text: '', correct: false },
      { text: '', correct: false }
    ]
  });
  const [questionLoading, setQuestionLoading] = useState(false);
  const [confirmQuestionDelete, setConfirmQuestionDelete] = useState<AdminQuestion | null>(null);
  const [questionDeleteLoading, setQuestionDeleteLoading] = useState(false);
  const [questionDeleteError, setQuestionDeleteError] = useState('');
  const isQuestionEditing = useMemo(() => Boolean(questionForm.id), [questionForm.id]);
  const [questionSubjectId, setQuestionSubjectId] = useState('');
  const [questionUnitId, setQuestionUnitId] = useState('');
  const [exportLoading, setExportLoading] = useState(false);
  const [importOpen, setImportOpen] = useState(false);
  const [importFile, setImportFile] = useState<File | null>(null);
  const [importFormat, setImportFormat] = useState<'csv' | 'json'>('csv');
  const [importLoading, setImportLoading] = useState(false);
  const [importMessage, setImportMessage] = useState('');
  const [importDone, setImportDone] = useState(false);
  const [importStats, setImportStats] = useState<{ created: number; errors: number } | null>(null);

  const subjectById = useMemo(() => subjects.reduce((acc, s) => { acc[s.id] = s; return acc; }, {} as Record<string, AdminSubject>), [subjects]);
  const unitById = useMemo(() => units.reduce((acc, u) => { acc[u.id] = u; return acc; }, {} as Record<string, AdminUnit>), [units]);
  const subjectOptions = useMemo(() => subjects.map(s => ({ value: s.id, label: s.name })), [subjects]);
  const unitOptions = useMemo(() => units.map(u => ({ value: u.id, label: u.name })), [units]);

  function resetForm() { setForm({ id: '', email: '', role: 'STUDENT', firstName: '', lastName: '', occupation: '' }); }
  function resetSubjectForm() { setSubjectForm({ id: '', name: '', description: '' }); }
  function resetUnitForm() { setUnitForm({ id: '', subjectId: '', name: '', description: '', orderIndex: 1 }); }
  function resetQuestionForm() {
    setQuestionForm({ id: '', unitId: '', text: '', explanation: '', difficulty: 'EASY', answers: [{ text: '', correct: true }, { text: '', correct: false }, { text: '', correct: false }, { text: '', correct: false }] });
  }

  async function loadUsers(page = userPage) {
    const data = await getAdminUsers(token, page - 1);
    setUsers(data.items); setUserPage(data.page + 1); setUserTotalPages(data.totalPages || 1);
  }
  async function loadSubjects() {
    const data = await getAdminSubjects(token);
    setSubjects(data);
  }
  async function loadUnits(subjectId: string) {
    if (!subjectId) { setUnits([]); return; }
    const data = await getAdminUnits(token, subjectId);
    setUnits(data);
  }
  async function loadQuestions(unitId: string, page = questionPage) {
    if (!unitId) { setQuestions([]); setQuestionTotalPages(1); return; }
    const data = await getAdminQuestions(token, unitId, page - 1);
    setQuestions(data.items); setQuestionPage(data.page + 1); setQuestionTotalPages(data.totalPages || 1);
  }

  useEffect(() => {
    loadUserSettings();
    if (isAdmin) { loadUsers(1).catch(() => setUsers([])); loadSubjects().catch(() => setSubjects([])); }
  }, []);
  useEffect(() => { if (tab === 'units') loadUnits(unitForm.subjectId).catch(() => setUnits([])); }, [tab, unitForm.subjectId]);
  useEffect(() => {
    if (tab === 'questions') {
      loadUnits(questionSubjectId).catch(() => setUnits([]));
      loadQuestions(questionUnitId, 1).catch(() => setQuestions([]));
    }
  }, [tab, questionSubjectId, questionUnitId]);

  async function saveUser() {
    if (!form.email.trim()) return;
    setLoading(true);
    try {
      await saveAdminUser(token, { id: form.id, email: form.email, firstName: form.firstName || '', lastName: form.lastName || '', occupation: form.occupation || '', role: form.role }, isEditing);
      resetForm(); await loadUsers(1);
    } finally { setLoading(false); }
  }

  async function saveSubject() {
    if (!subjectForm.name.trim()) return;
    setSubjectLoading(true);
    try {
      await saveAdminSubject(token, { id: subjectForm.id, name: subjectForm.name, description: subjectForm.description || '' }, isSubjectEditing);
      resetSubjectForm(); await loadSubjects(); onSubjectsChanged?.();
    } finally { setSubjectLoading(false); }
  }

  async function removeUser(id: string) { await deleteAdminUser(token, id); await loadUsers(userPage); }
  async function removeSubject(id: string) { await deleteAdminSubject(token, id); await loadSubjects(); onSubjectsChanged?.(); }

  async function saveUnit() {
    if (!unitForm.subjectId || !unitForm.name.trim()) return;
    setUnitLoading(true);
    try {
      await saveAdminUnit(token, { id: unitForm.id, subjectId: unitForm.subjectId, name: unitForm.name, description: unitForm.description || '', orderIndex: unitForm.orderIndex || 0 }, isUnitEditing);
      resetUnitForm(); await loadUnits(unitForm.subjectId); await loadSubjects();
    } finally { setUnitLoading(false); }
  }

  async function removeUnit(id: string) { await deleteAdminUnit(token, id); await loadUnits(unitForm.subjectId); await loadSubjects(); }

  async function saveQuestion() {
    const unitId = questionUnitId || questionForm.unitId;
    if (!unitId || !questionForm.text.trim()) return;
    if (questionForm.answers.some(a => !a.text.trim())) return;
    if (questionForm.answers.filter(a => a.correct).length !== 1) return;
    setQuestionLoading(true);
    try {
      await saveAdminQuestion(token, { ...questionForm, unitId }, isQuestionEditing);
      resetQuestionForm(); await loadQuestions(unitId, 1); await loadUnits(questionSubjectId);
    } finally { setQuestionLoading(false); }
  }

  async function removeQuestion(id: string, unitId: string) { await deleteAdminQuestion(token, id); await loadQuestions(unitId, questionPage); await loadUnits(questionSubjectId); }

  async function handleExport(format: 'csv' | 'json') {
    setExportLoading(true);
    try {
      const blob = await exportAdminQuestions(token, format, questionUnitId || undefined);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${subjects.find(s => s.id === questionSubjectId)?.name || 'preguntas'}-${units.find(u => u.id === questionUnitId)?.name || 'preguntas'}.${format}`;
      a.click();
      window.URL.revokeObjectURL(url);
    } finally { setExportLoading(false); }
  }

  async function handleImport() {
    if (!importFile) { setImportMessage('Selecciona un archivo'); return; }
    if (!questionUnitId) { setImportMessage('Selecciona una unidad'); return; }
    setImportLoading(true); setImportMessage('');
    try {
      const data = await importAdminQuestions(token, importFormat, questionUnitId, importFile);
      const created = data.created || 0;
      const errors = (data.errors as number) || 0;
      setImportStats({ created, errors });
      setImportMessage(`Importadas: ${created}. Errores: ${errors}`);
      setImportDone(true);
      await loadQuestions(questionUnitId);
    } catch { setImportMessage('No se pudo importar'); }
    finally { setImportLoading(false); }
  }

  const generalNavItems = [
    { id: 'general' as Tab, label: 'General', icon: <Cog className="w-4 h-4" /> },
  ];

  const adminNavItems = [
    { id: 'users' as Tab, label: 'Usuarios', icon: <Users className="w-4 h-4" /> },
    { id: 'subjects' as Tab, label: 'Materias', icon: <BookOpen className="w-4 h-4" /> },
    { id: 'units' as Tab, label: 'Unidades', icon: <FolderOpen className="w-4 h-4" /> },
    { id: 'questions' as Tab, label: 'Preguntas', icon: <FileLines className="w-4 h-4" /> },
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
              {settingsError && (
                <div className="mt-3 rounded-xl border border-red-400/30 bg-red-400/10 px-4 py-2 text-sm text-red-400">{settingsError}</div>
              )}
              <div className="flex items-center gap-3 mt-5">
                <button onClick={saveUserSettings} disabled={settingsLoading} className={btnPrimary}>
                  {settingsSaved ? <Check className="w-4 h-4" /> : <Check className="w-4 h-4" />}
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

        {/* ── Materias ── */}
        {tab === 'subjects' && (
          <div className="grid gap-5 py-[1.5rem]">
            <h2 className="text-xl font-extrabold tracking-tight">Gestión de <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">Materias</span></h2>

            <div className={card}>
              <div className="grid gap-3 sm:grid-cols-2 mb-4">
                <input className={inp} placeholder="Nombre" value={subjectForm.name} onChange={e => setSubjectForm(f => ({ ...f, name: e.target.value }))} />
                <input className={inp} placeholder="Descripción (opcional)" value={subjectForm.description || ''} onChange={e => setSubjectForm(f => ({ ...f, description: e.target.value }))} />
              </div>
              <div className="flex gap-2">
                <button onClick={saveSubject} disabled={subjectLoading} className={btnPrimary}>
                  {isSubjectEditing ? <Check className="w-4 h-4" /> : <Plus className="w-4 h-4" />}
                  {isSubjectEditing ? 'Guardar cambios' : 'Crear materia'}
                </button>
                {isSubjectEditing && (
                  <button onClick={resetSubjectForm} className={btnOutline}>
                    <CircleMinus className="w-4 h-4" />Cancelar
                  </button>
                )}
              </div>
            </div>

            <div className={card}>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead><tr className="border-b border-secondary/20">
                    <Th>Materia</Th>
                    <Th className="hidden sm:table-cell">Descripción</Th>
                    <Th />
                  </tr></thead>
                  <tbody>
                    {subjects.map(s => (
                      <tr key={s.id} className="border-b border-secondary/10 last:border-0">
                        <Td>{s.name}</Td>
                        <Td className="hidden sm:table-cell">{s.description || '-'}</Td>
                        <Td>
                          <div className="flex gap-2">
                            <button onClick={() => setSubjectForm(s)} className="text-accent hover:opacity-70 transition-opacity" title="Editar"><Pen className="w-4 h-4" /></button>
                            <button onClick={() => { setConfirmSubjectDelete(s); setSubjectDeleteText(''); setSubjectDeleteError(''); }} className="text-red-400 hover:opacity-70 transition-opacity" title="Eliminar"><TrashBin className="w-4 h-4" /></button>
                          </div>
                        </Td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {/* ── Unidades ── */}
        {tab === 'units' && (
          <div className="grid gap-5 py-[1.5rem]">
            <h2 className="text-xl font-extrabold tracking-tight">Gestión de <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">Unidades</span></h2>

            <div className={card}>
              <div className="grid gap-3 sm:grid-cols-2 mb-4">
                <select className={inp} value={unitForm.subjectId} onChange={e => setUnitForm(f => ({ ...f, subjectId: e.target.value }))}>
                  <option value="">Selecciona materia</option>
                  {subjectOptions.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}
                </select>
                <input className={inp} placeholder="Nombre" value={unitForm.name} onChange={e => setUnitForm(f => ({ ...f, name: e.target.value }))} />
                <input className={inp} placeholder="Descripción (opcional)" value={unitForm.description || ''} onChange={e => setUnitForm(f => ({ ...f, description: e.target.value }))} />
                <input className={inp} type="number" placeholder="Orden" value={unitForm.orderIndex} onChange={e => setUnitForm(f => ({ ...f, orderIndex: Number(e.target.value) }))} />
              </div>
              <div className="flex gap-2">
                <button onClick={saveUnit} disabled={unitLoading} className={btnPrimary}>
                  {isUnitEditing ? <Check className="w-4 h-4" /> : <Plus className="w-4 h-4" />}
                  {isUnitEditing ? 'Guardar cambios' : 'Crear unidad'}
                </button>
                {isUnitEditing && (
                  <button onClick={resetUnitForm} className={btnOutline}>
                    <CircleMinus className="w-4 h-4" />Cancelar
                  </button>
                )}
              </div>
            </div>

            <div className={card}>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead><tr className="border-b border-secondary/20">
                    <Th>Materia</Th>
                    <Th>Unidad</Th>
                    <Th className="hidden sm:table-cell">Descripción</Th>
                    <Th className="hidden sm:table-cell">Orden</Th>
                    <Th />
                  </tr></thead>
                  <tbody>
                    {units.map(u => (
                      <tr key={u.id} className="border-b border-secondary/10 last:border-0">
                        <Td>{subjectById[u.subjectId]?.name || '-'}</Td>
                        <Td>{u.name}</Td>
                        <Td className="hidden sm:table-cell">{u.description || '-'}</Td>
                        <Td className="hidden sm:table-cell">{u.orderIndex}</Td>
                        <Td>
                          <div className="flex gap-2">
                            <button onClick={() => setUnitForm(u)} className="text-accent hover:opacity-70 transition-opacity" title="Editar"><Pen className="w-4 h-4" /></button>
                            <button onClick={() => { setConfirmUnitDelete(u); setUnitDeleteError(''); setUnitDeleteText(''); }} className="text-red-400 hover:opacity-70 transition-opacity" title="Eliminar"><TrashBin className="w-4 h-4" /></button>
                          </div>
                        </Td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {/* ── Preguntas ── */}
        {tab === 'questions' && (
          <div className="grid gap-5 py-[1.5rem]">
            <h2 className="text-xl font-extrabold tracking-tight"> Gestión de <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">Preguntas</span></h2>

            <div className={card}>
              <div className="text-xs text-text/50 uppercase tracking-wide font-semibold mb-3">Filtros</div>
              <div className="grid gap-3 sm:grid-cols-2">
                <select className={inp} value={questionSubjectId} onChange={e => setQuestionSubjectId(e.target.value)}>
                  <option value="">Selecciona materia</option>
                  {subjectOptions.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}
                </select>
                <select className={inp} value={questionUnitId} onChange={e => setQuestionUnitId(e.target.value)}>
                  <option value="">Selecciona unidad</option>
                  {unitOptions.map(u => <option key={u.value} value={u.value}>{u.label}</option>)}
                </select>
              </div>
            </div>

            <div className={card}>
              {!questionUnitId && (
                <p className="text-sm text-text/50 mb-4">Selecciona materia y unidad para crear preguntas.</p>
              )}
              <div className="grid gap-3 sm:grid-cols-2 mb-4">
                <input className={inp} placeholder="Enunciado" value={questionForm.text} onChange={e => setQuestionForm(f => ({ ...f, text: e.target.value }))} />
                <input className={inp} placeholder="Explicación (opcional)" value={questionForm.explanation || ''} onChange={e => setQuestionForm(f => ({ ...f, explanation: e.target.value }))} />
                <select className={inp} value={questionForm.difficulty} onChange={e => setQuestionForm(f => ({ ...f, difficulty: e.target.value as AdminQuestion['difficulty'] }))}>
                  <option value="EASY">Fácil</option>
                  <option value="MEDIUM">Media</option>
                  <option value="HARD">Difícil</option>
                </select>
              </div>

              <div className="grid gap-2 mb-4">
                {questionForm.answers.map((a, idx) => (
                  <div key={idx} className="flex gap-3 items-center">
                    <input
                      type="radio"
                      name="correct"
                      checked={a.correct}
                      onChange={() => setQuestionForm(f => ({ ...f, answers: f.answers.map((ans, i) => ({ ...ans, correct: i === idx })) }))}
                      className="shrink-0"
                    />
                    <input
                      className={inp}
                      placeholder={`Respuesta ${idx + 1}`}
                      value={a.text}
                      onChange={e => setQuestionForm(f => ({ ...f, answers: f.answers.map((ans, i) => i === idx ? { ...ans, text: e.target.value } : ans) }))}
                    />
                  </div>
                ))}
              </div>

              <div className="flex flex-wrap gap-2">
                <button onClick={saveQuestion} disabled={questionLoading || !questionSubjectId || !questionUnitId} className={btnPrimary}>
                  {isQuestionEditing ? <Check className="w-4 h-4" /> : <Plus className="w-4 h-4" />}
                  {isQuestionEditing ? 'Guardar cambios' : 'Crear pregunta'}
                </button>
                {isQuestionEditing && (
                  <button onClick={resetQuestionForm} className={btnOutline}>
                    <CircleMinus className="w-4 h-4" />Cancelar
                  </button>
                )}
                <div className="ml-auto flex flex-wrap gap-2">
                  <button onClick={() => handleExport('json')} disabled={exportLoading || !questionUnitId || questions.length === 0} className={btnOutline}>
                    <FileExport className="w-4 h-4" />JSON
                  </button>
                  <button onClick={() => handleExport('csv')} disabled={exportLoading || !questionUnitId || questions.length === 0} className={btnOutline}>
                    <FileCsv className="w-4 h-4" />CSV
                  </button>
                  <button onClick={() => { setImportOpen(true); setImportMessage(''); setImportDone(false); setImportStats(null); setImportFile(null); }} disabled={!questionUnitId} className={btnOutline}>
                    <FileImport className="w-4 h-4" />Importar
                  </button>
                </div>
              </div>
            </div>

            <div className={card}>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead><tr className="border-b border-secondary/20">
                    <Th>Pregunta</Th>
                    <Th className="hidden sm:table-cell">Dificultad</Th>
                    <Th className="hidden sm:table-cell">Respuestas</Th>
                    <Th />
                  </tr></thead>
                  <tbody>
                    {questions.map(q => (
                      <tr key={q.id} className="border-b border-secondary/10 last:border-0">
                        <Td><span className="line-clamp-2">{q.text}</span></Td>
                        <Td className="hidden sm:table-cell">{q.difficulty}</Td>
                        <Td className="hidden sm:table-cell"><span className="line-clamp-1">{q.answers.map(a => a.text).join(', ')}</span></Td>
                        <Td>
                          <div className="flex gap-2">
                            <button onClick={() => {
                              const unit = unitById[q.unitId];
                              if (unit?.subjectId) setQuestionSubjectId(unit.subjectId);
                              setQuestionUnitId(q.unitId);
                              setQuestionForm({ id: q.id, unitId: q.unitId, text: q.text, explanation: q.explanation || '', difficulty: q.difficulty, answers: q.answers.map(a => ({ text: a.text, correct: a.correct })) });
                            }} className="text-accent hover:opacity-70 transition-opacity" title="Editar"><Pen className="w-4 h-4" /></button>
                            <button onClick={() => { setConfirmQuestionDelete(q); setQuestionDeleteError(''); }} className="text-red-400 hover:opacity-70 transition-opacity" title="Eliminar"><TrashBin className="w-4 h-4" /></button>
                          </div>
                        </Td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <Pagination page={questionPage} totalPages={questionTotalPages} onChange={p => loadQuestions(questionUnitId, p)} />
            </div>
          </div>
        )}
      </section>

      {/* ── Modal Importar ── */}
      {importOpen && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 px-4">
          <div className="bg-bg border border-secondary/25 rounded-2xl p-6 w-full max-w-md shadow-xl">
            <h3 className="text-lg font-bold mb-4">Importar preguntas</h3>
            <div className="grid gap-3 mb-4">
              <select className={inp} value={importFormat} onChange={e => setImportFormat(e.target.value as 'csv' | 'json')}>
                <option value="csv">CSV</option>
                <option value="json">JSON</option>
              </select>
              <input type="file" accept={importFormat === 'csv' ? '.csv' : '.json'} onChange={e => setImportFile(e.target.files?.[0] || null)}
                className="text-sm text-text/70 file:mr-3 file:py-1.5 file:px-4 file:rounded-full file:border-0 file:text-sm file:bg-primary/10 file:text-primary hover:file:bg-primary/20 file:transition-colors"
              />
              {importMessage && (() => {
                const badgeClass = importStats
                  ? importStats.created > 0 && importStats.errors === 0
                    ? 'bg-green-500/15 text-green-600 border border-green-500/30'
                    : importStats.created === 0 && importStats.errors > 0
                      ? 'bg-red-500/15 text-red-600 border border-red-500/30'
                      : 'bg-yellow-500/15 text-yellow-600 border border-yellow-500/30'
                  : 'bg-secondary/20 text-text/70';
                return <div className={`text-sm rounded-lg px-3 py-2 font-medium ${badgeClass}`}>{importMessage}</div>;
              })()}
            </div>
            <div className="flex gap-2 justify-end">
              {importDone ? (
                <button className={btnPrimary} onClick={() => setImportOpen(false)}>
                  Cerrar
                </button>
              ) : (
                <>
                  <button className={btnOutline} onClick={() => setImportOpen(false)}>
                    <CircleMinus className="w-4 h-4" />Cancelar
                  </button>
                  <button className={btnPrimary} onClick={handleImport} disabled={importLoading}>
                    <FileImport className="w-4 h-4" />{importLoading ? 'Importando...' : 'Importar'}
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      )}

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

      {/* ── Modal Eliminar materia ── */}
      {confirmSubjectDelete && (
        <DeleteModal
          title="Eliminar materia"
          body={
            <>
              <p className="text-sm text-text/70 mb-3">¿Seguro que quieres eliminar <strong>{confirmSubjectDelete.name}</strong>?</p>
              {(confirmSubjectDelete.unitCount ?? 0) > 0 && (
                <div className="rounded-xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm text-red-400">
                  Esta materia tiene unidades asociadas. Si la eliminas se eliminarán todas sus unidades y preguntas. Esta acción es irreversible.
                </div>
              )}
            </>
          }
          error={subjectDeleteError}
          loading={subjectDeleteLoading}
          requireText={(confirmSubjectDelete.unitCount ?? 0) > 0 ? { value: subjectDeleteText, onChange: setSubjectDeleteText, passes: subjectDeleteText.trim().toLowerCase() === 'eliminar' } : undefined}
          onClose={() => setConfirmSubjectDelete(null)}
          onConfirm={async () => {
            setSubjectDeleteError(''); setSubjectDeleteLoading(true);
            try { await removeSubject(confirmSubjectDelete.id); setConfirmSubjectDelete(null); }
            catch { setSubjectDeleteError('No se pudo eliminar'); }
            finally { setSubjectDeleteLoading(false); }
          }}
        />
      )}

      {/* ── Modal Eliminar unidad ── */}
      {confirmUnitDelete && (
        <DeleteModal
          title="Eliminar unidad"
          body={
            <>
              <p className="text-sm text-text/70 mb-3">¿Seguro que quieres eliminar <strong>{confirmUnitDelete.name}</strong>?</p>
              {(confirmUnitDelete.questionCount ?? 0) > 0 && (
                <div className="rounded-xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm text-red-400">
                  Esta unidad tiene preguntas asociadas. Si la eliminas se eliminarán todas sus preguntas. Esta acción es irreversible.
                </div>
              )}
            </>
          }
          error={unitDeleteError}
          loading={unitDeleteLoading}
          requireText={(confirmUnitDelete.questionCount ?? 0) > 0 ? { value: unitDeleteText, onChange: setUnitDeleteText, passes: unitDeleteText.trim().toLowerCase() === 'eliminar' } : undefined}
          onClose={() => setConfirmUnitDelete(null)}
          onConfirm={async () => {
            setUnitDeleteError(''); setUnitDeleteLoading(true);
            try { await removeUnit(confirmUnitDelete.id); setConfirmUnitDelete(null); }
            catch { setUnitDeleteError('No se pudo eliminar'); }
            finally { setUnitDeleteLoading(false); }
          }}
        />
      )}

      {/* ── Modal Eliminar pregunta ── */}
      {confirmQuestionDelete && (
        <DeleteModal
          title="Eliminar pregunta"
          body={<p className="text-sm text-text/70">¿Seguro que quieres eliminar esta pregunta?</p>}
          error={questionDeleteError}
          loading={questionDeleteLoading}
          onClose={() => setConfirmQuestionDelete(null)}
          onConfirm={async () => {
            setQuestionDeleteError(''); setQuestionDeleteLoading(true);
            try { await removeQuestion(confirmQuestionDelete.id, confirmQuestionDelete.unitId); setConfirmQuestionDelete(null); }
            catch { setQuestionDeleteError('No se pudo eliminar'); }
            finally { setQuestionDeleteLoading(false); }
          }}
        />
      )}
    </div>
  );
}
