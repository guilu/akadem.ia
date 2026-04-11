import { useEffect, useMemo, useState } from 'react';
import { Plus, Check, CircleMinus, Pen, TrashBin, FileExport, FileImport, FileCsv, BookOpen, FolderOpen, FileLines, Inbox } from 'flowbite-react-icons/outline';
import { apiBase, apiJson } from '../api';
import ScopeFilter, { type ContentScope } from './ScopeFilter';

export type AdminSubject = {
  id: string;
  name: string;
  description?: string | null;
  unitCount?: number;
  visibility?: 'GLOBAL' | 'PRIVATE';
  isEditable?: boolean;
};

export type AdminUnit = {
  id: string;
  subjectId: string;
  name: string;
  description?: string | null;
  orderIndex: number;
  questionCount?: number;
  visibility?: 'GLOBAL' | 'PRIVATE';
  isEditable?: boolean;
};

export type AdminAnswer = { id?: string; text: string; correct: boolean };
export type AdminQuestion = {
  id: string;
  unitId: string;
  text: string;
  explanation?: string | null;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  answers: AdminAnswer[];
  visibility?: 'GLOBAL' | 'PRIVATE';
  isEditable?: boolean;
};

type Tab = 'subjects' | 'units' | 'questions';
type Scope = ContentScope;

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

export default function Management({ isAdmin, onSubjectsChanged }: { isAdmin: boolean; onSubjectsChanged?: () => void }) {
  const [tab, setTab] = useState<Tab>('subjects');

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

  const [subjectScope, setSubjectScope] = useState<Scope>('ALL');
  const [unitScope, setUnitScope] = useState<Scope>('ALL');
  const [questionScope, setQuestionScope] = useState<Scope>('ALL');

  const subjectById = useMemo(() => subjects.reduce((acc, s) => { acc[s.id] = s; return acc; }, {} as Record<string, AdminSubject>), [subjects]);
  const unitById = useMemo(() => units.reduce((acc, u) => { acc[u.id] = u; return acc; }, {} as Record<string, AdminUnit>), [units]);
  const subjectOptions = useMemo(() => subjects.map(s => ({ value: s.id, label: s.name })), [subjects]);
  const unitOptions = useMemo(() => units.map(u => ({ value: u.id, label: u.name })), [units]);

  function resetSubjectForm() { setSubjectForm({ id: '', name: '', description: '' }); }
  function resetUnitForm() { setUnitForm({ id: '', subjectId: '', name: '', description: '', orderIndex: 1 }); }
  function resetQuestionForm() {
    setQuestionForm({ id: '', unitId: '', text: '', explanation: '', difficulty: 'EASY', answers: [{ text: '', correct: true }, { text: '', correct: false }, { text: '', correct: false }, { text: '', correct: false }] });
  }

  async function loadSubjects(scope: Scope = subjectScope) {
    const scopeParam = scope === 'ALL' ? '' : `&scope=${scope}`;
    const data = await apiJson<AdminSubject[]>(`${apiBase}/api/manage/subjects?_=1${scopeParam}`);
    setSubjects(data);
  }
  async function loadUnits(subjectId: string, scope: Scope = unitScope) {
    if (!subjectId) { setUnits([]); return; }
    const scopeParam = scope === 'ALL' ? '' : `&scope=${scope}`;
    const data = await apiJson<AdminUnit[]>(`${apiBase}/api/manage/units?subjectId=${subjectId}${scopeParam}`);
    setUnits(data);
  }
  async function loadQuestions(unitId: string, page = questionPage, scope: Scope = questionScope) {
    if (!unitId) { setQuestions([]); setQuestionTotalPages(1); return; }
    const scopeParam = scope === 'ALL' ? '' : `&scope=${scope}`;
    const data = await apiJson<{ items: AdminQuestion[]; page: number; totalPages: number }>(`${apiBase}/api/manage/questions?unitId=${unitId}&page=${page}&size=10${scopeParam}`);
    setQuestions(data.items); setQuestionPage(data.page + 1); setQuestionTotalPages(data.totalPages || 1);
  }

  useEffect(() => {
    loadSubjects('ALL').catch(() => setSubjects([]));
  }, []);
  useEffect(() => { if (tab === 'units') loadUnits(unitForm.subjectId, unitScope).catch(() => setUnits([])); }, [tab, unitForm.subjectId, unitScope]);
  useEffect(() => {
    if (tab === 'questions') {
      loadUnits(questionSubjectId, 'ALL').catch(() => setUnits([]));
      loadQuestions(questionUnitId, 1, questionScope).catch(() => setQuestions([]));
    }
  }, [tab, questionSubjectId, questionUnitId, questionScope]);
  useEffect(() => { if (tab === 'subjects') loadSubjects(subjectScope).catch(() => setSubjects([])); }, [tab, subjectScope]);

  async function saveSubject() {
    if (!subjectForm.name.trim()) return;
    setSubjectLoading(true);
    try {
      const visibility = isAdmin && subjectScope === 'GLOBAL' ? 'GLOBAL' : 'PRIVATE';
      const body = JSON.stringify({ name: subjectForm.name, description: subjectForm.description || null, visibility });
      const opts = { method: isSubjectEditing ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' }, body };
      await apiJson(isSubjectEditing ? `${apiBase}/api/manage/subjects/${subjectForm.id}` : `${apiBase}/api/manage/subjects`, opts);
      resetSubjectForm(); await loadSubjects(subjectScope); onSubjectsChanged?.();
    } finally { setSubjectLoading(false); }
  }

  async function removeSubject(id: string) { await apiJson(`${apiBase}/api/manage/subjects/${id}`, { method: 'DELETE' }); await loadSubjects(subjectScope); onSubjectsChanged?.(); }

  async function saveUnit() {
    if (!unitForm.subjectId || !unitForm.name.trim()) return;
    setUnitLoading(true);
    try {
      const visibility = isAdmin && unitScope === 'GLOBAL' ? 'GLOBAL' : 'PRIVATE';
      const payload = { subjectId: unitForm.subjectId, name: unitForm.name, description: unitForm.description || null, orderIndex: unitForm.orderIndex || 0, visibility };
      const opts = { method: isUnitEditing ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) };
      await apiJson(isUnitEditing ? `${apiBase}/api/manage/units/${unitForm.id}` : `${apiBase}/api/manage/units`, opts);
      resetUnitForm(); await loadUnits(unitForm.subjectId, unitScope); await loadSubjects(subjectScope);
    } finally { setUnitLoading(false); }
  }

  async function removeUnit(id: string) { await apiJson(`${apiBase}/api/manage/units/${id}`, { method: 'DELETE' }); await loadUnits(unitForm.subjectId, unitScope); await loadSubjects(subjectScope); }

  async function saveQuestion() {
    const unitId = questionUnitId || questionForm.unitId;
    if (!unitId || !questionForm.text.trim()) return;
    if (questionForm.answers.some(a => !a.text.trim())) return;
    if (questionForm.answers.filter(a => a.correct).length !== 1) return;
    setQuestionLoading(true);
    try {
      const visibility = isAdmin && questionScope === 'GLOBAL' ? 'GLOBAL' : 'PRIVATE';
      const payload = { unitId, text: questionForm.text, explanation: questionForm.explanation || null, difficulty: questionForm.difficulty, answers: questionForm.answers.map(a => ({ text: a.text, correct: a.correct })), visibility };
      const opts = { method: isQuestionEditing ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) };
      await apiJson(isQuestionEditing ? `${apiBase}/api/manage/questions/${questionForm.id}` : `${apiBase}/api/manage/questions`, opts);
      resetQuestionForm(); await loadQuestions(unitId, 1, questionScope); await loadUnits(questionSubjectId, 'ALL');
    } finally { setQuestionLoading(false); }
  }

  async function removeQuestion(id: string, unitId: string) { await apiJson(`${apiBase}/api/manage/questions/${id}`, { method: 'DELETE' }); await loadQuestions(unitId, questionPage, questionScope); await loadUnits(questionSubjectId, 'ALL'); }

  async function handleExport(format: 'csv' | 'json') {
    setExportLoading(true);
    try {
      const query = questionUnitId ? `?unitId=${questionUnitId}&format=${format}` : `?format=${format}`;
      const res = await fetch(`${apiBase}/api/manage/questions/export${query}`, { credentials: 'include' });
      if (!res.ok) throw new Error('export_failed');
      const blob = await res.blob();
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
      const formData = new FormData();
      formData.append('file', importFile);
      const res = await fetch(`${apiBase}/api/manage/questions/import?format=${importFormat}&unitId=${questionUnitId}`, { method: 'POST', credentials: 'include', body: formData });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data.error || 'import_failed');
      const created = data.created || 0;
      const errors = data.errors || 0;
      setImportStats({ created, errors });
      setImportMessage(`Importadas: ${created}. Errores: ${errors}`);
      setImportDone(true);
      await loadQuestions(questionUnitId);
    } catch { setImportMessage('No se pudo importar'); }
    finally { setImportLoading(false); }
  }

  const navItems = [
    { id: 'subjects' as Tab, label: 'Materias', icon: <BookOpen className="w-4 h-4" /> },
    { id: 'units' as Tab, label: 'Unidades', icon: <FolderOpen className="w-4 h-4" /> },
    { id: 'questions' as Tab, label: 'Preguntas', icon: <FileLines className="w-4 h-4" /> },
  ];

  return (
    <div className="grid md:grid-cols-[200px_1fr] gap-6">

      {/* ── Sidebar ── */}
      <aside className={`${card} h-fit lg:mt-[4.5rem]`}>
        <div className="text-xs text-text/50 uppercase tracking-wide font-semibold mb-3">Gestionar contenido</div>
        {navItems.map(({ id, label, icon }) => (
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
      </aside>

      {/* ── Content ── */}
      <section className="grid gap-5">

        {/* ── Materias ── */}
        {tab === 'subjects' && (
          <div className="grid gap-5 py-[1.5rem]">
            <div className="flex items-center justify-between flex-wrap gap-3">
              <h2 className="text-xl font-extrabold tracking-tight">Gestión de <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">Materias</span></h2>
              <ScopeFilter scope={subjectScope} onChange={setSubjectScope} isAdmin={isAdmin} />
            </div>

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
                    <Th className="hidden sm:table-cell">Visibilidad</Th>
                    <Th />
                  </tr></thead>
                  <tbody>
                    {subjects.length === 0 ? (
                      <tr><td colSpan={4} className="py-8 text-center text-sm text-text/55"><Inbox className="w-7 h-7 mx-auto mb-2 text-text/25" />No hay materias disponibles.</td></tr>
                    ) : subjects.map(s => (
                      <tr key={s.id} className="border-b border-secondary/10 last:border-0">
                        <Td>{s.name}</Td>
                        <Td className="hidden sm:table-cell">{s.description || '-'}</Td>
                        <Td className="hidden sm:table-cell">
                          <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${s.visibility === 'GLOBAL' ? 'bg-blue-500/15 text-blue-600' : 'bg-secondary/20 text-text/60'}`}>
                            {s.visibility === 'GLOBAL' ? 'Global' : 'Personal'}
                          </span>
                        </Td>
                        <Td>
                          {s.isEditable !== false && (
                            <div className="flex gap-2">
                              <button onClick={() => setSubjectForm(s)} className="text-accent hover:opacity-70 transition-opacity" title="Editar"><Pen className="w-4 h-4" /></button>
                              <button onClick={() => { setConfirmSubjectDelete(s); setSubjectDeleteText(''); setSubjectDeleteError(''); }} className="text-red-400 hover:opacity-70 transition-opacity" title="Eliminar"><TrashBin className="w-4 h-4" /></button>
                            </div>
                          )}
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
            <div className="flex items-center justify-between flex-wrap gap-3">
              <h2 className="text-xl font-extrabold tracking-tight">Gestión de <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">Unidades</span></h2>
              <ScopeFilter scope={unitScope} onChange={setUnitScope} isAdmin={isAdmin} />
            </div>

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
                    <Th className="hidden sm:table-cell">Visibilidad</Th>
                    <Th />
                  </tr></thead>
                  <tbody>
                    {units.length === 0 ? (
                      <tr><td colSpan={6} className="py-8 text-center text-sm text-text/55"><Inbox className="w-7 h-7 mx-auto mb-2 text-text/25" />No hay unidades disponibles.</td></tr>
                    ) : units.map(u => (
                      <tr key={u.id} className="border-b border-secondary/10 last:border-0">
                        <Td>{subjectById[u.subjectId]?.name || '-'}</Td>
                        <Td>{u.name}</Td>
                        <Td className="hidden sm:table-cell">{u.description || '-'}</Td>
                        <Td className="hidden sm:table-cell">{u.orderIndex}</Td>
                        <Td className="hidden sm:table-cell">
                          <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${u.visibility === 'GLOBAL' ? 'bg-blue-500/15 text-blue-600' : 'bg-secondary/20 text-text/60'}`}>
                            {u.visibility === 'GLOBAL' ? 'Global' : 'Personal'}
                          </span>
                        </Td>
                        <Td>
                          {u.isEditable !== false && (
                            <div className="flex gap-2">
                              <button onClick={() => setUnitForm(u)} className="text-accent hover:opacity-70 transition-opacity" title="Editar"><Pen className="w-4 h-4" /></button>
                              <button onClick={() => { setConfirmUnitDelete(u); setUnitDeleteError(''); setUnitDeleteText(''); }} className="text-red-400 hover:opacity-70 transition-opacity" title="Eliminar"><TrashBin className="w-4 h-4" /></button>
                            </div>
                          )}
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
            <div className="flex items-center justify-between flex-wrap gap-3">
              <h2 className="text-xl font-extrabold tracking-tight">Gestión de <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">Preguntas</span></h2>
              <ScopeFilter scope={questionScope} onChange={setQuestionScope} isAdmin={isAdmin} />
            </div>

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
                    {questions.length === 0 ? (
                      <tr><td colSpan={4} className="py-8 text-center text-sm text-text/55"><Inbox className="w-7 h-7 mx-auto mb-2 text-text/25" />{questionUnitId ? 'No hay preguntas disponibles.' : 'Selecciona una materia y unidad para ver las preguntas.'}</td></tr>
                    ) : questions.map(q => (
                      <tr key={q.id} className="border-b border-secondary/10 last:border-0">
                        <Td><span className="line-clamp-2">{q.text}</span></Td>
                        <Td className="hidden sm:table-cell">{q.difficulty}</Td>
                        <Td className="hidden sm:table-cell"><span className="line-clamp-1">{q.answers.map(a => a.text).join(', ')}</span></Td>
                        <Td>
                          {q.isEditable !== false && (
                            <div className="flex gap-2">
                              <button onClick={() => {
                                const unit = unitById[q.unitId];
                                if (unit?.subjectId) setQuestionSubjectId(unit.subjectId);
                                setQuestionUnitId(q.unitId);
                                setQuestionForm({ id: q.id, unitId: q.unitId, text: q.text, explanation: q.explanation || '', difficulty: q.difficulty, answers: q.answers.map(a => ({ text: a.text, correct: a.correct })) });
                              }} className="text-accent hover:opacity-70 transition-opacity" title="Editar"><Pen className="w-4 h-4" /></button>
                              <button onClick={() => { setConfirmQuestionDelete(q); setQuestionDeleteError(''); }} className="text-red-400 hover:opacity-70 transition-opacity" title="Eliminar"><TrashBin className="w-4 h-4" /></button>
                            </div>
                          )}
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
