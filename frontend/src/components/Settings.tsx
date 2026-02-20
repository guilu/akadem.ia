import { useEffect, useMemo, useState } from 'react';
import { Button, Card, FileInput, Select, Table, TableBody, TableCell, TableHead, TableHeadCell, TableRow, TextInput } from 'flowbite-react';
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

export type AdminAnswer = { id?: string; text: string; correct: boolean };
export type AdminQuestion = {
  id: string;
  unitId: string;
  text: string;
  explanation?: string | null;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  answers: AdminAnswer[];
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

  const [questions, setQuestions] = useState<AdminQuestion[]>([]);
  const [questionForm, setQuestionForm] = useState<AdminQuestion>({
    id: '',
    unitId: '',
    text: '',
    explanation: '',
    difficulty: 'EASY',
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

  function resetForm() {
    setForm({ id: '', email: '', role: 'STUDENT', firstName: '', lastName: '', occupation: '' });
  }

  function resetSubjectForm() {
    setSubjectForm({ id: '', name: '', description: '' });
  }

  function resetUnitForm() {
    setUnitForm({ id: '', subjectId: '', name: '', description: '', orderIndex: 1 });
  }

  function resetQuestionForm() {
    setQuestionForm({
      id: '',
      unitId: '',
      text: '',
      explanation: '',
      difficulty: 'EASY',
      answers: [
        { text: '', correct: true },
        { text: '', correct: false },
        { text: '', correct: false },
        { text: '', correct: false }
      ]
    });
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

  async function loadQuestions(unitId: string) {
    if (!unitId) {
      setQuestions([]);
      return;
    }
    const data = await apiAuthJson<AdminQuestion[]>(`${apiBase}/api/admin/questions?unitId=${unitId}`, token);
    setQuestions(data);
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

  useEffect(() => {
    if (tab === 'questions') {
      loadUnits(questionSubjectId).catch(() => setUnits([]));
      loadQuestions(questionUnitId).catch(() => setQuestions([]));
    }
  }, [tab, questionSubjectId, questionUnitId]);

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

  async function saveQuestion() {
    const unitId = questionUnitId || questionForm.unitId;
    if (!unitId || !questionForm.text.trim()) return;
    if (questionForm.answers.some(a => !a.text.trim())) return;
    const correctCount = questionForm.answers.filter(a => a.correct).length;
    if (correctCount !== 1) return;
    setQuestionLoading(true);
    try {
      const payload = {
        unitId,
        text: questionForm.text,
        explanation: questionForm.explanation || null,
        difficulty: questionForm.difficulty,
        answers: questionForm.answers.map(a => ({ text: a.text, correct: a.correct }))
      };
      if (isQuestionEditing) {
        await apiAuthJson(`${apiBase}/api/admin/questions/${questionForm.id}`, token, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
      } else {
        await apiAuthJson(`${apiBase}/api/admin/questions`, token, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
      }
      resetQuestionForm();
      await loadQuestions(unitId);
      await loadUnits(questionSubjectId);
    } finally {
      setQuestionLoading(false);
    }
  }

  async function removeQuestion(id: string, unitId: string) {
    await apiAuthJson(`${apiBase}/api/admin/questions/${id}`, token, { method: 'DELETE' });
    await loadQuestions(unitId);
    await loadUnits(questionSubjectId);
  }

  async function handleExport(format: 'csv' | 'json') {
    setExportLoading(true);
    try {
      const query = questionUnitId ? `?unitId=${questionUnitId}&format=${format}` : `?format=${format}`;
      const res = await fetch(`${apiBase}/api/admin/questions/export${query}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (!res.ok) throw new Error('export_failed');
      const blob = await res.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `questions.${format}`;
      a.click();
      window.URL.revokeObjectURL(url);
    } finally {
      setExportLoading(false);
    }
  }

  async function handleImport() {
    if (!importFile) {
      setImportMessage('Selecciona un archivo');
      return;
    }
    setImportLoading(true);
    setImportMessage('');
    try {
      const formData = new FormData();
      formData.append('file', importFile);
      const res = await fetch(`${apiBase}/api/admin/questions/import?format=${importFormat}`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: formData
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data.error || 'import_failed');
      setImportMessage(`Importadas: ${data.created || 0}. Errores: ${data.errors || 0}`);
      await loadQuestions(questionUnitId);
    } catch {
      setImportMessage('No se pudo importar');
    } finally {
      setImportLoading(false);
    }
  }

  return (
    <div className="grid md:grid-cols-[220px_1fr] gap-6">
      <aside className="border border-slate-800 rounded-xl p-3 h-fit">
        <div className="text-xs text-text/70 mb-2">Administración</div>
        <button className={`w-full text-left px-3 py-2 rounded ${tab === 'users' ? 'btn btn-secondary' : ''}`} onClick={()=>setTab('users')}>Usuarios</button>
        <button className={`w-full text-left px-3 py-2 rounded ${tab === 'subjects' ? 'btn btn-secondary' : ''}`} onClick={()=>setTab('subjects')}>Materias</button>
        <button className={`w-full text-left px-3 py-2 rounded ${tab === 'units' ? 'btn btn-secondary' : ''}`} onClick={()=>setTab('units')}>Unidades</button>
        <button className={`w-full text-left px-3 py-2 rounded ${tab === 'questions' ? 'btn btn-secondary' : ''}`} onClick={()=>setTab('questions')}>Preguntas</button>
      </aside>

      <section className="grid gap-6">
        {tab === 'users' && (
          <div className="grid gap-4">
            <h2 className="text-xl font-semibold">Usuarios</h2>
            <Card className="border border-secondary/40 bg-bg">
              <div className="grid gap-2 sm:grid-cols-2">
                <TextInput placeholder="email" value={form.email} onChange={e=>setForm(f=>({ ...f, email: e.target.value }))} />
                <Select value={form.role} onChange={e=>setForm(f=>({ ...f, role: e.target.value as AdminUser['role'] }))}>
                  <option value="ADMIN">ADMIN</option>
                  <option value="TEACHER">TEACHER</option>
                  <option value="STUDENT">STUDENT</option>
                </Select>
                <TextInput placeholder="nombre" value={form.firstName || ''} onChange={e=>setForm(f=>({ ...f, firstName: e.target.value }))} />
                <TextInput placeholder="apellidos" value={form.lastName || ''} onChange={e=>setForm(f=>({ ...f, lastName: e.target.value }))} />
                <Select value={form.occupation || ''} onChange={e=>setForm(f=>({ ...f, occupation: e.target.value }))}>
                  <option value="">ocupación (opcional)</option>
                  <option value="STUDENT">Estudiante</option>
                  <option value="TEACHER">Profesor</option>
                  <option value="OPOSITOR">Opositor</option>
                  <option value="OTHER">Otro</option>
                </Select>
              </div>
              <div className="flex gap-2 mt-3">
                <Button onClick={saveUser} disabled={loading} className="btn btn-primary">
                  {isEditing ? 'Guardar cambios' : 'Crear usuario'}
                </Button>
                {isEditing && (
                  <Button onClick={resetForm} color="light" className="btn btn-outline">Cancelar</Button>
                )}
              </div>
            </Card>

            <Card className="border border-secondary/40 bg-bg">
              <div className="overflow-x-auto">
                <Table>
                  <TableHead>
                    <TableHeadCell>Email</TableHeadCell>
                    <TableHeadCell className="hidden sm:table-cell">Rol</TableHeadCell>
                    <TableHeadCell className="hidden sm:table-cell">Nombre</TableHeadCell>
                    <TableHeadCell><span className="sr-only">Acciones</span></TableHeadCell>
                  </TableHead>
                  <TableBody className="divide-y">
                    {users.map(u => (
                      <TableRow key={u.id} className="border-secondary/30 bg-transparent">
                        <TableCell>{u.email}</TableCell>
                        <TableCell className="hidden sm:table-cell">{u.role}</TableCell>
                        <TableCell className="hidden sm:table-cell">{[u.firstName, u.lastName].filter(Boolean).join(' ')}</TableCell>
                        <TableCell>
                          <div className="flex gap-2">
                            <button type="button" className="text-accent cursor-pointer" onClick={()=>setForm(u)} aria-label="Editar" title="Editar">✏️</button>
                            <button type="button" className="text-red-400 cursor-pointer" onClick={()=>setConfirmDelete(u)} aria-label="Eliminar" title="Eliminar">🗑️</button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            </Card>
          </div>
        )}

        {tab === 'subjects' && (
          <div className="grid gap-4">
            <h2 className="text-xl font-semibold">Materias</h2>
            <Card className="border border-secondary/40 bg-bg">
              <div className="grid gap-2 sm:grid-cols-2">
                <TextInput placeholder="nombre" value={subjectForm.name} onChange={e=>setSubjectForm(f=>({ ...f, name: e.target.value }))} />
                <TextInput placeholder="descripción (opcional)" value={subjectForm.description || ''} onChange={e=>setSubjectForm(f=>({ ...f, description: e.target.value }))} />
              </div>
              <div className="flex gap-2 mt-3">
                <Button onClick={saveSubject} disabled={subjectLoading} className="btn btn-primary">
                  {isSubjectEditing ? 'Guardar cambios' : 'Crear materia'}
                </Button>
                {isSubjectEditing && (
                  <Button onClick={resetSubjectForm} color="light" className="btn btn-outline">Cancelar</Button>
                )}
              </div>
            </Card>

            <Card className="border border-secondary/40 bg-bg">
              <div className="overflow-x-auto">
                <Table>
                  <TableHead>
                    <TableHeadCell>Materia</TableHeadCell>
                    <TableHeadCell className="hidden sm:table-cell">Descripción</TableHeadCell>
                    <TableHeadCell><span className="sr-only">Acciones</span></TableHeadCell>
                  </TableHead>
                  <TableBody className="divide-y">
                    {subjects.map(s => (
                      <TableRow key={s.id} className="border-secondary/30 bg-transparent">
                        <TableCell>{s.name}</TableCell>
                        <TableCell className="hidden sm:table-cell">{s.description || '-'} </TableCell>
                        <TableCell>
                          <div className="flex gap-2">
                            <button type="button" className="text-accent cursor-pointer" onClick={()=>setSubjectForm(s)} aria-label="Editar" title="Editar">✏️</button>
                            <button
                              type="button"
                              className="text-red-400 cursor-pointer"
                              onClick={()=>{ setConfirmSubjectDelete(s); setSubjectDeleteText(''); setSubjectDeleteError(''); }}
                              aria-label="Eliminar"
                              title="Eliminar"
                            >🗑️</button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            </Card>
          </div>
        )}

        {tab === 'units' && (
          <div className="grid gap-4">
            <h2 className="text-xl font-semibold">Unidades</h2>
            <Card className="border border-secondary/40 bg-bg">
              <div className="grid gap-2 sm:grid-cols-2">
                <Select value={unitForm.subjectId} onChange={e=>setUnitForm(f=>({ ...f, subjectId: e.target.value }))}>
                  <option value="">Selecciona materia</option>
                  {subjects.map(s => (
                    <option key={s.id} value={s.id}>{s.name}</option>
                  ))}
                </Select>
                <TextInput placeholder="nombre" value={unitForm.name} onChange={e=>setUnitForm(f=>({ ...f, name: e.target.value }))} />
                <TextInput placeholder="descripción (opcional)" value={unitForm.description || ''} onChange={e=>setUnitForm(f=>({ ...f, description: e.target.value }))} />
                <TextInput type="number" placeholder="orden" value={unitForm.orderIndex} onChange={e=>setUnitForm(f=>({ ...f, orderIndex: Number(e.target.value) }))} />
              </div>
              <div className="flex gap-2 mt-3">
                <Button onClick={saveUnit} disabled={unitLoading} className="btn btn-primary">
                  {isUnitEditing ? 'Guardar cambios' : 'Crear unidad'}
                </Button>
                {isUnitEditing && (
                  <Button onClick={resetUnitForm} color="light" className="btn btn-outline">Cancelar</Button>
                )}
              </div>
            </Card>

            <Card className="border border-secondary/40 bg-bg">
              <div className="overflow-x-auto">
                <Table>
                  <TableHead>
                    <TableHeadCell>Materia</TableHeadCell>
                    <TableHeadCell>Unidad</TableHeadCell>
                    <TableHeadCell className="hidden sm:table-cell">Descripción</TableHeadCell>
                    <TableHeadCell className="hidden sm:table-cell">Orden</TableHeadCell>
                    <TableHeadCell><span className="sr-only">Acciones</span></TableHeadCell>
                  </TableHead>
                  <TableBody className="divide-y">
                    {units.map(u => (
                      <TableRow key={u.id} className="border-secondary/30 bg-transparent">
                        <TableCell>{subjects.find(s => s.id === u.subjectId)?.name || '-'}</TableCell>
                        <TableCell>{u.name}</TableCell>
                        <TableCell className="hidden sm:table-cell">{u.description || '-'}</TableCell>
                        <TableCell className="hidden sm:table-cell">{u.orderIndex}</TableCell>
                        <TableCell>
                          <div className="flex gap-2">
                            <button type="button" className="text-accent cursor-pointer" onClick={()=>setUnitForm(u)} aria-label="Editar" title="Editar">✏️</button>
                            <button type="button" className="text-red-400 cursor-pointer" onClick={()=>{ setConfirmUnitDelete(u); setUnitDeleteError(''); setUnitDeleteText(''); }} aria-label="Eliminar" title="Eliminar">🗑️</button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            </Card>
          </div>
        )}

        {tab === 'questions' && (
          <div className="grid gap-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h2 className="text-xl font-semibold">Preguntas</h2>
              <div className="flex gap-2">
                <Button onClick={() => handleExport('json')} disabled={exportLoading} className="btn btn-outline text-primary">Exportar JSON</Button>
                <Button onClick={() => handleExport('csv')} disabled={exportLoading} className="btn btn-outline text-primary">Exportar CSV</Button>
                <Button onClick={() => { setImportOpen(true); setImportMessage(''); }} className="btn btn-secondary">Importar</Button>
              </div>
            </div>
            <Card className="border border-secondary/40 bg-bg">
              <div className="grid gap-2 sm:grid-cols-2">
                <Select value={questionSubjectId} onChange={e=>setQuestionSubjectId(e.target.value)}>
                  <option value="">Selecciona materia</option>
                  {subjects.map(s => (
                    <option key={s.id} value={s.id}>{s.name}</option>
                  ))}
                </Select>
                <Select value={questionUnitId} onChange={e=>setQuestionUnitId(e.target.value)}>
                  <option value="">Selecciona unidad</option>
                  {units.map(u => (
                    <option key={u.id} value={u.id}>{u.name}</option>
                  ))}
                </Select>
                <TextInput placeholder="enunciado" value={questionForm.text} onChange={e=>setQuestionForm(f=>({ ...f, text: e.target.value }))} />
                <TextInput placeholder="explicación (opcional)" value={questionForm.explanation || ''} onChange={e=>setQuestionForm(f=>({ ...f, explanation: e.target.value }))} />
                <Select value={questionForm.difficulty} onChange={e=>setQuestionForm(f=>({ ...f, difficulty: e.target.value as AdminQuestion['difficulty'] }))}>
                  <option value="EASY">Fácil</option>
                  <option value="MEDIUM">Media</option>
                  <option value="HARD">Difícil</option>
                </Select>
              </div>

              <div className="mt-4 grid gap-2">
                {questionForm.answers.map((a, idx) => (
                  <div key={idx} className="flex gap-2 items-center">
                    <input
                      type="radio"
                      name="correct"
                      checked={a.correct}
                      onChange={() => setQuestionForm(f => ({
                        ...f,
                        answers: f.answers.map((ans, i) => ({ ...ans, correct: i === idx }))
                      }))}
                    />
                    <TextInput
                      placeholder={`Respuesta ${idx + 1}`}
                      value={a.text}
                      onChange={e => setQuestionForm(f => ({
                        ...f,
                        answers: f.answers.map((ans, i) => i === idx ? { ...ans, text: e.target.value } : ans)
                      }))}
                      className="flex-1"
                    />
                  </div>
                ))}
              </div>

              <div className="flex gap-2 mt-3">
                <Button onClick={saveQuestion} disabled={questionLoading} className="btn btn-primary">
                  {isQuestionEditing ? 'Guardar cambios' : 'Crear pregunta'}
                </Button>
                {isQuestionEditing && (
                  <Button onClick={resetQuestionForm} color="light" className="btn btn-outline">Cancelar</Button>
                )}
              </div>
            </Card>

            <Card className="border border-secondary/40 bg-bg">
              <div className="overflow-x-auto">
                <Table>
                  <TableHead>
                    <TableHeadCell>Pregunta</TableHeadCell>
                    <TableHeadCell className="hidden sm:table-cell">Dificultad</TableHeadCell>
                    <TableHeadCell className="hidden sm:table-cell">Respuestas</TableHeadCell>
                    <TableHeadCell><span className="sr-only">Acciones</span></TableHeadCell>
                  </TableHead>
                  <TableBody className="divide-y">
                    {questions.map(q => (
                      <TableRow key={q.id} className="border-secondary/30 bg-transparent">
                        <TableCell>{q.text}</TableCell>
                        <TableCell className="hidden sm:table-cell">{q.difficulty}</TableCell>
                        <TableCell className="hidden sm:table-cell">{q.answers.map(a => a.text).join(', ')}</TableCell>
                        <TableCell>
                          <div className="flex gap-2">
                            <button type="button" className="text-accent cursor-pointer" onClick={()=>{
                              setQuestionForm({
                                id: q.id,
                                unitId: q.unitId,
                                text: q.text,
                                explanation: q.explanation || '',
                                difficulty: q.difficulty,
                                answers: q.answers.map(a => ({ text: a.text, correct: a.correct }))
                              });
                            }} aria-label="Editar" title="Editar">✏️</button>
                            <button type="button" className="text-red-400 cursor-pointer" onClick={()=>{ setConfirmQuestionDelete(q); setQuestionDeleteError(''); }} aria-label="Eliminar" title="Eliminar">🗑️</button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            </Card>
          </div>
        )}
      </section>

      {importOpen && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
          <div className="bg-bg border border-secondary/40 rounded-xl p-5 w-full max-w-md">
            <h3 className="text-lg font-semibold mb-2">Importar preguntas</h3>
            <div className="grid gap-3">
              <Select value={importFormat} onChange={e=>setImportFormat(e.target.value as 'csv' | 'json')}>
                <option value="csv">CSV</option>
                <option value="json">JSON</option>
              </Select>
              <FileInput accept={importFormat === 'csv' ? '.csv' : '.json'} onChange={e=>setImportFile(e.target.files?.[0] || null)} />
              {importMessage && <div className="text-sm text-text/70">{importMessage}</div>}
            </div>
            <div className="flex gap-2 justify-end mt-4">
              <Button color="light" className="btn btn-outline" onClick={() => setImportOpen(false)}>Cancelar</Button>
              <Button className="btn btn-primary" onClick={handleImport} disabled={importLoading}>
                {importLoading ? 'Importando...' : 'Importar'}
              </Button>
            </div>
          </div>
        </div>
      )}

      {confirmDelete && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
          <div className="bg-bg border border-secondary/40 rounded-xl p-5 w-full max-w-sm">
            <h3 className="text-lg font-semibold mb-2">Eliminar usuario</h3>
            <p className="text-sm text-text/70 mb-4">¿Seguro que quieres eliminar <strong>{confirmDelete.email}</strong>?</p>
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
          <div className="bg-bg border border-secondary/40 rounded-xl p-5 w-full max-w-sm">
            <h3 className="text-lg font-semibold mb-2">Eliminar materia</h3>
            <p className="text-sm text-text/70 mb-3">¿Seguro que quieres eliminar <strong>{confirmSubjectDelete.name}</strong>?</p>
            {confirmSubjectDelete.unitCount !== undefined && confirmSubjectDelete.unitCount > 0 && (
              <div className="text-sm text-red-400 mb-3">
                Esta materia tiene unidades asociadas, si la eliminas se eliminarán todas sus unidades y preguntas asociadas. Esta acción es irreversible.
              </div>
            )}
            {confirmSubjectDelete.unitCount !== undefined && confirmSubjectDelete.unitCount > 0 && (
              <input
                className="w-full rounded px-3 py-2 mb-3"
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
          <div className="bg-bg border border-secondary/40 rounded-xl p-5 w-full max-w-sm">
            <h3 className="text-lg font-semibold mb-2">Eliminar unidad</h3>
            <p className="text-sm text-text/70 mb-3">¿Seguro que quieres eliminar <strong>{confirmUnitDelete.name}</strong>?</p>
            {confirmUnitDelete.questionCount !== undefined && confirmUnitDelete.questionCount > 0 && (
              <div className="text-sm text-red-400 mb-3">
                Esta unidad tiene preguntas asociadas, si la eliminas se eliminarán todas sus preguntas y respuestas asociadas. Esta acción es irreversible.
              </div>
            )}
            {confirmUnitDelete.questionCount !== undefined && confirmUnitDelete.questionCount > 0 && (
              <input
                className="w-full rounded px-3 py-2 mb-3"
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

      {confirmQuestionDelete && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
          <div className="bg-bg border border-secondary/40 rounded-xl p-5 w-full max-w-sm">
            <h3 className="text-lg font-semibold mb-2">Eliminar pregunta</h3>
            <p className="text-sm text-text/70 mb-4">¿Seguro que quieres eliminar esta pregunta?</p>
            {questionDeleteError && <div className="text-sm text-red-400 mb-2">{questionDeleteError}</div>}
            <div className="flex gap-2 justify-end">
              <button type="button" className="px-3 py-2 rounded border border-slate-600" onClick={() => setConfirmQuestionDelete(null)}>Cancelar</button>
              <button
                type="button"
                className="px-3 py-2 rounded bg-red-600 disabled:opacity-60"
                disabled={questionDeleteLoading}
                onClick={async () => {
                  setQuestionDeleteError('');
                  setQuestionDeleteLoading(true);
                  try {
                    await removeQuestion(confirmQuestionDelete.id, confirmQuestionDelete.unitId);
                    setConfirmQuestionDelete(null);
                  } catch {
                    setQuestionDeleteError('No se pudo eliminar');
                  } finally {
                    setQuestionDeleteLoading(false);
                  }
                }}
              >
                {questionDeleteLoading ? 'Eliminando...' : 'Eliminar'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
