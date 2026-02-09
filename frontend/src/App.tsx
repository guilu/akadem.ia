import { useEffect, useState } from 'react';
import ExamRunner, { Question } from './components/ExamRunner';
import ExamBuilder from './components/ExamBuilder';
import Login from './components/Login';

type Subject = { id: string; name: string; description?: string };

export default function App(){
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [mode, setMode] = useState<'home'|'builder'|'runner'|'result'>('home');
  const [subject, setSubject] = useState<Subject|undefined>();
  const [questions, setQuestions] = useState<Question[]>([]);
  const [minutes, setMinutes] = useState(20);
  const [attemptId, setAttemptId] = useState<string>('');
  const [token, setToken] = useState<string>(localStorage.getItem('ak_token') || '');
  const [result, setResult] = useState<{ total:number, correct:number, percentage:number }|null>(null);

  useEffect(() => {
    if (!token) { setSubjects([]); return; }
    fetch('http://localhost:8080/api/subjects', {
      headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}) }
    }).then(r=>r.json()).then(setSubjects);
  }, [token]);

  function onToken(t:string){
    setToken(t);
    localStorage.setItem('ak_token', t);
  }

  async function startExam(cfg:{ unitCounts: Record<string, number>, minutes: number }){
    const res = await fetch('http://localhost:8080/api/exams/attempts/start', {
      method: 'POST',
      headers: { 'Content-Type':'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: JSON.stringify(cfg)
    });
    const data = await res.json();
    setAttemptId(data.attemptId);
    setMinutes(Math.round(data.totalTimeSeconds / 60));
    setQuestions(data.questions);
    setMode('runner');
  }

  async function finishExam(payload:{ selections: Record<string,string|undefined> }){
    const selections: Record<string,string> = {};
    Object.entries(payload.selections).forEach(([q, a]) => { if(a) selections[q] = a; });
    const res = await fetch(`http://localhost:8080/api/exams/attempts/${attemptId}/submit`, {
      method: 'POST',
      headers: { 'Content-Type':'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: JSON.stringify({ selections })
    });
    const data = await res.json();
    setResult(data);
    setMode('result');
  }

  return (
    <div className="max-w-4xl mx-auto p-6">
      <header className="flex items-center justify-between mb-8">
        <h1 className="text-3xl font-bold">Akdemya</h1>
        <nav className="flex gap-4 items-center">
          <a className="hover:underline" href="#" onClick={()=>setMode('home')}>Home</a>
          {token ? (
            <button className="text-sm border border-slate-600 rounded px-2 py-1" onClick={()=>{ localStorage.removeItem('ak_token'); setToken(''); }}>Salir</button>
          ) : (
            <></>
          )}
        </nav>
      </header>

      {!token && <div className="mb-8"><Login onToken={onToken}/></div>}

      {mode === 'home' && (
        <section className="mb-8">
          <h2 className="text-xl font-semibold mb-2">Asignaturas</h2>
          {!token ? (
            <div className="text-slate-400">Inicia sesión para ver las asignaturas disponibles.</div>
          ) : (
            <div className="grid sm:grid-cols-2 gap-4">
              {subjects.map(s => (
                <div key={s.id} className="border border-slate-700 rounded-xl p-4">
                  <div className="text-lg font-semibold">{s.name}</div>
                  <div className="text-sm text-slate-400">{s.description}</div>
                  <div className="mt-3">
                    <button className="px-3 py-2 rounded-xl bg-cyan-600" onClick={()=>{ setSubject(s); setMode('builder'); }}>
                      Crear simulacro
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      )}

      {mode === 'builder' && subject && (
        <ExamBuilder subjectId={subject.id} onStart={startExam}/>
      )}

      {mode === 'runner' && questions.length > 0 && (
        <ExamRunner questions={questions} totalTimeSeconds={minutes*60} onFinish={finishExam}/>
      )}

      {mode === 'result' && result && (
        <div className="max-w-xl mx-auto p-4 border border-slate-700 rounded-xl">
          <h2 className="text-2xl font-bold mb-3">Resultados</h2>
          <p>Correctas: <strong>{result.correct}</strong> / {result.total}</p>
          <p>Porcentaje: <strong>{result.percentage.toFixed(1)}%</strong></p>
          <div className="mt-4">
            <button className="px-3 py-2 rounded bg-indigo-600" onClick={()=>{ setResult(null); setMode('home'); }}>Volver al inicio</button>
          </div>
        </div>
      )}
    </div>
  );
}
