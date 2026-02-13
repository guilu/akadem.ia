import { useEffect, useMemo, useState } from 'react';
import ExamRunner, { Question } from './components/ExamRunner';
import ExamBuilder from './components/ExamBuilder';
import Login from './components/Login';
import Register from './components/Register';
import { apiBase } from './api';

type Subject = { id: string; name: string; description?: string };

type View = 'home'|'login'|'register'|'subjects'|'builder'|'runner'|'result';

export default function App(){
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [view, setView] = useState<View>('home');
  const [subject, setSubject] = useState<Subject|undefined>();
  const [questions, setQuestions] = useState<Question[]>([]);
  const [minutes, setMinutes] = useState(20);
  const [attemptId, setAttemptId] = useState<string>('');
  const [token, setToken] = useState<string>(localStorage.getItem('ak_token') || '');
  const [result, setResult] = useState<{ total:number, correct:number, percentage:number }|null>(null);

  const isAuthed = useMemo(() => Boolean(token), [token]);

  async function authedFetch(url: string, options: RequestInit = {}) {
    const res = await fetch(url, {
      ...options,
      headers: { ...(options.headers || {}), Authorization: `Bearer ${token}` }
    });
    if (res.status === 401) {
      onLogout();
      throw new Error('unauthorized');
    }
    return res;
  }

  useEffect(() => {
    if (!token) {
      setSubjects([]);
      return;
    }
    authedFetch(`${apiBase}/api/subjects`)
      .then(r => r.ok ? r.json() : [])
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
    const res = await authedFetch(`${apiBase}/api/exams/attempts/start`, {
      method: 'POST',
      headers: { 'Content-Type':'application/json' },
      body: JSON.stringify(cfg)
    });
    const data = await res.json();
    setAttemptId(data.attemptId);
    setMinutes(Math.round(data.totalTimeSeconds / 60));
    setQuestions(data.questions);
    setView('runner');
  }

  async function finishExam(payload:{ selections: Record<string,string|undefined> }){
    const selections: Record<string,string> = {};
    Object.entries(payload.selections).forEach(([q, a]) => { if(a) selections[q] = a; });
    const res = await authedFetch(`${apiBase}/api/exams/attempts/${attemptId}/submit`, {
      method: 'POST',
      headers: { 'Content-Type':'application/json' },
      body: JSON.stringify({ selections })
    });
    const data = await res.json();
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
          <nav className="flex gap-4 items-center">
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
                <button className="text-sm border border-slate-600 rounded px-2 py-1" onClick={onLogout}>Salir</button>
              </>
            )}
          </nav>
        </div>
      </header>

      <main className="max-w-4xl mx-auto p-6">
        {view === 'home' && (
          <section className="mt-6 text-center">
            <h1 className="text-3xl font-bold mb-3">Tu academia online para tests</h1>
            <p className="text-slate-400">Practica con simulacros, mide tu progreso y mejora tus resultados.</p>
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
      </main>
    </div>
  );
}
