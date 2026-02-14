import { useEffect, useMemo, useState } from 'react';
import { Link, Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import type { Question } from './components/ExamRunner';
import { apiBase, apiAuthJson } from './api';
import type { Subject, ExamResult, ExamStartResponse } from './types';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import SubjectsPage from './pages/SubjectsPage';
import ExamBuilderPage from './pages/ExamBuilderPage';
import ExamRunnerPage from './pages/ExamRunnerPage';
import ExamResultPage from './pages/ExamResultPage';
import SettingsPage from './pages/SettingsPage';
import ProtectedRoute from './pages/ProtectedRoute';

export default function App(){
  const navigate = useNavigate();
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [questions, setQuestions] = useState<Question[]>([]);
  const [minutes, setMinutes] = useState(20);
  const [attemptId, setAttemptId] = useState<string>('');
  const [token, setToken] = useState<string>(localStorage.getItem('ak_token') || '');
  const [result, setResult] = useState<ExamResult|null>(null);
  const [menuOpen, setMenuOpen] = useState(false);

  const isAuthed = useMemo(() => Boolean(token), [token]);
  const role = useMemo(() => {
    try {
      if (!token) return null;
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.role || null;
    } catch {
      return null;
    }
  }, [token]);

  async function authedJson<T>(url: string, options: RequestInit = {}) {
    try {
      return await apiAuthJson<T>(url, token, options);
    } catch (err: any) {
      if (err?.status === 401) onLogout();
      throw err;
    }
  }

  useEffect(() => {
    if (!token) {
      setSubjects([]);
      return;
    }
    authedJson<Subject[]>(`${apiBase}/api/subjects`)
      .then(setSubjects)
      .catch(() => setSubjects([]));
  }, [token, apiBase]);

  function onToken(t:string){
    setToken(t);
    localStorage.setItem('ak_token', t);
    navigate('/subjects');
  }

  function onLogout(){
    localStorage.removeItem('ak_token');
    setToken('');
    navigate('/');
  }

  async function startExam(cfg:{ unitCounts: Record<string, number>, minutes: number }){
    const data = await authedJson<ExamStartResponse>(`${apiBase}/api/exams/attempts/start`, {
      method: 'POST',
      headers: { 'Content-Type':'application/json' },
      body: JSON.stringify(cfg)
    });
    setAttemptId(data.attemptId);
    setMinutes(Math.round(data.totalTimeSeconds / 60));
    setQuestions(data.questions);
    navigate('/exam');
  }

  async function finishExam(payload:{ selections: Record<string,string|undefined> }){
    const selections: Record<string,string> = {};
    Object.entries(payload.selections).forEach(([q, a]) => { if(a) selections[q] = a; });
    const data = await authedJson<ExamResult>(`${apiBase}/api/exams/attempts/${attemptId}/submit`, {
      method: 'POST',
      headers: { 'Content-Type':'application/json' },
      body: JSON.stringify({ selections })
    });
    setResult(data);
    navigate('/result');
  }

  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-800 bg-slate-950/80 backdrop-blur">
        <div className="max-w-5xl mx-auto px-6 py-4 flex items-center justify-between">
          <Link className="text-2xl font-bold flex items-center gap-3" to="/">
            <img src="/assets/icons/akdmia-icon-32x32.png" alt="AKDMIA" className="w-8 h-8" />
            Akdemya
          </Link>
          <nav className="hidden md:flex gap-4 items-center">
            <Link className="hover:underline" to="/">Home</Link>
            {!isAuthed && (
              <>
                <Link className="hover:underline" to="/login">Login</Link>
                <Link className="hover:underline" to="/register">Register</Link>
              </>
            )}
            {isAuthed && (
              <>
                <Link className="hover:underline" to="/subjects">Asignaturas</Link>
                {role === 'ADMIN' && (
                  <Link className="hover:underline" to="/settings">Configuración</Link>
                )}
                <button className="text-sm border border-slate-600 rounded px-2 py-1" onClick={onLogout}>Salir</button>
              </>
            )}
          </nav>
          <button
            className="md:hidden text-sm border border-slate-600 rounded px-2 py-1"
            onClick={() => setMenuOpen(o => !o)}
            aria-label="menu"
          >
            {menuOpen ? 'Cerrar' : 'Menu'}
          </button>
        </div>
        {menuOpen && (
          <div className="md:hidden border-t border-slate-800 px-6 py-3 flex flex-col gap-2">
            <Link className="text-left hover:underline" to="/" onClick={()=>setMenuOpen(false)}>Home</Link>
            {!isAuthed && (
              <>
                <Link className="text-left hover:underline" to="/login" onClick={()=>setMenuOpen(false)}>Login</Link>
                <Link className="text-left hover:underline" to="/register" onClick={()=>setMenuOpen(false)}>Register</Link>
              </>
            )}
            {isAuthed && (
              <>
                <Link className="text-left hover:underline" to="/subjects" onClick={()=>setMenuOpen(false)}>Asignaturas</Link>
                {role === 'ADMIN' && (
                  <Link className="text-left hover:underline" to="/settings" onClick={()=>setMenuOpen(false)}>Configuración</Link>
                )}
                <button className="text-left border border-slate-600 rounded px-2 py-1 w-fit" onClick={()=>{ onLogout(); setMenuOpen(false); }}>Salir</button>
              </>
            )}
          </div>
        )}
      </header>

      <main className="max-w-4xl mx-auto p-6">
        <Routes>
          <Route path="/" element={<HomePage isAuthed={isAuthed} />} />
          <Route path="/login" element={<LoginPage isAuthed={isAuthed} onToken={onToken} />} />
          <Route path="/register" element={<RegisterPage isAuthed={isAuthed} onToken={onToken} />} />
          <Route path="/subjects" element={
            <ProtectedRoute allow={isAuthed}>
              <SubjectsPage subjects={subjects} />
            </ProtectedRoute>
          } />
          <Route path="/subjects/:subjectId/builder" element={
            <ProtectedRoute allow={isAuthed}>
              <ExamBuilderPage onStart={startExam} onUnauthorized={onLogout} />
            </ProtectedRoute>
          } />
          <Route path="/exam" element={
            <ProtectedRoute allow={isAuthed}>
              <ExamRunnerPage questions={questions} minutes={minutes} onFinish={finishExam} />
            </ProtectedRoute>
          } />
          <Route path="/result" element={
            <ProtectedRoute allow={isAuthed}>
              <ExamResultPage result={result} />
            </ProtectedRoute>
          } />
          <Route path="/settings" element={<SettingsPage isAdmin={role === 'ADMIN'} token={token} />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}
