import { useEffect, useMemo, useState } from 'react';
import ExamRunner, { Question } from './components/ExamRunner';
import ExamBuilder from './components/ExamBuilder';
import Login from './components/Login';
import Register from './components/Register';
import Settings from './components/Settings';
import { apiBase, apiAuthJson } from './api';
import type { Subject, ExamResult, ExamStartResponse } from './types';

type View = 'home'|'login'|'register'|'subjects'|'builder'|'runner'|'result'|'settings';

export default function App(){
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [view, setView] = useState<View>('home');
  const [subject, setSubject] = useState<Subject|undefined>();
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
    setView('subjects');
  }

  function onLogout(){
    localStorage.removeItem('ak_token');
    setToken('');
    setView('home');
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
    setView('runner');
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
    setView('result');
  }

  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-800 bg-slate-950/80 backdrop-blur">
        <div className="max-w-5xl mx-auto px-6 py-4 flex items-center justify-between">
          <button className="text-2xl font-bold flex items-center gap-3" onClick={()=>setView('home')}>
            <img src="/assets/icons/akdmia-icon-32x32.png" alt="AKDMIA" className="w-8 h-8" />
            Akdemya
          </button>
          <nav className="hidden md:flex gap-4 items-center">
            <button className="hover:underline" onClick={()=>setView('home')}>Home</button>
            {!isAuthed && (
              <>
                <button className="hover:underline" onClick={()=>setView('login')}>Login</button>
                <button className="hover:underline" onClick={()=>setView('register')}>Register</button>
              </>
            )}
            {isAuthed && (
              <>
                <button className="hover:underline" onClick={()=>setView('subjects')}>Asignaturas</button>
                {role === 'ADMIN' && (
                  <button className="hover:underline" onClick={()=>setView('settings')}>Configuración</button>
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
            <button className="text-left hover:underline" onClick={()=>{ setView('home'); setMenuOpen(false); }}>Home</button>
            {!isAuthed && (
              <>
                <button className="text-left hover:underline" onClick={()=>{ setView('login'); setMenuOpen(false); }}>Login</button>
                <button className="text-left hover:underline" onClick={()=>{ setView('register'); setMenuOpen(false); }}>Register</button>
              </>
            )}
            {isAuthed && (
              <>
                <button className="text-left hover:underline" onClick={()=>{ setView('subjects'); setMenuOpen(false); }}>Asignaturas</button>
                {role === 'ADMIN' && (
                  <button className="text-left hover:underline" onClick={()=>{ setView('settings'); setMenuOpen(false); }}>Configuración</button>
                )}
                <button className="text-left border border-slate-600 rounded px-2 py-1 w-fit" onClick={()=>{ onLogout(); setMenuOpen(false); }}>Salir</button>
              </>
            )}
          </div>
        )}
      </header>

      <main className="max-w-4xl mx-auto p-6">
        {view === 'home' && (
          <section className="mt-6">
            <div className="text-center">
              <h1 className="text-4xl font-bold mb-3">Prepárate con simulacros reales</h1>
              <p className="text-slate-400 max-w-2xl mx-auto">
                Practica con tests cronometrados, sigue tu progreso y mejora tus resultados.
              </p>
              <div className="mt-6 flex flex-col sm:flex-row gap-3 justify-center">
                {isAuthed && (
                  <button className="px-5 py-3 rounded-xl bg-cyan-600" onClick={()=>setView('subjects')}>
                    Crear simulacro
                  </button>
                )}
                {!isAuthed && (
                  <div className="flex gap-2 justify-center">
                    <button className="px-3 py-2 rounded-full border border-slate-600 text-sm" onClick={()=>setView('login')}>Login</button>
                    <button className="px-3 py-2 rounded-full border border-slate-600 text-sm" onClick={()=>setView('register')}>Register</button>
                  </div>
                )}
              </div>
            </div>

            <div className="mt-10 grid gap-4 sm:grid-cols-3">
              <div className="border border-slate-700 rounded-xl p-4">
                <div className="text-2xl font-bold">+20</div>
                <div className="text-sm text-slate-400">Preguntas por materia</div>
              </div>
              <div className="border border-slate-700 rounded-xl p-4">
                <div className="text-2xl font-bold">Simulacros</div>
                <div className="text-sm text-slate-400">Con tiempo real</div>
              </div>
              <div className="border border-slate-700 rounded-xl p-4">
                <div className="text-2xl font-bold">Progreso</div>
                <div className="text-sm text-slate-400">Resultados inmediatos</div>
              </div>
            </div>
          </section>
        )}

        {view === 'login' && !isAuthed && (
          <div className="mt-8"><Login onToken={onToken}/></div>
        )}

        {view === 'register' && !isAuthed && (
          <div className="mt-8"><Register onToken={onToken}/></div>
        )}

        {view === 'subjects' && isAuthed && (
          <section className="mb-8">
            <h2 className="text-xl font-semibold mb-2">Asignaturas</h2>
            <div className="grid sm:grid-cols-2 gap-4">
              {subjects.map(s => (
                <div key={s.id} className="border border-slate-700 rounded-xl p-4">
                  <div className="text-lg font-semibold">{s.name}</div>
                  <div className="text-sm text-slate-400">{s.description}</div>
                  <div className="mt-3">
                    <button className="px-3 py-2 rounded-xl bg-cyan-600" onClick={()=>{ setSubject(s); setView('builder'); }}>
                      Crear simulacro
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </section>
        )}

        {view === 'builder' && subject && (
          <ExamBuilder subjectId={subject.id} onStart={startExam} onUnauthorized={onLogout}/>
        )}

        {view === 'runner' && questions.length > 0 && (
          <ExamRunner questions={questions} totalTimeSeconds={minutes*60} onFinish={finishExam}/>
        )}

        {view === 'result' && result && (
          <div className="max-w-xl mx-auto p-4 border border-slate-700 rounded-xl">
            <h2 className="text-2xl font-bold mb-3">Resultados</h2>
            <p>Correctas: <strong>{result.correct}</strong> / {result.total}</p>
            <p>Porcentaje: <strong>{result.percentage.toFixed(1)}%</strong></p>
            <div className="mt-4">
              <button className="px-3 py-2 rounded bg-indigo-600" onClick={()=>{ setResult(null); setView('subjects'); }}>Volver a asignaturas</button>
            </div>
          </div>
        )}

        {view === 'settings' && role === 'ADMIN' && (
          <Settings token={token} />
        )}
      </main>
    </div>
  );
}
