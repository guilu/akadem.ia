import { useEffect, useMemo, useState } from 'react';

export type Answer = { id: string; text: string };
export type Question = { id: string; text: string; answers: Answer[] };

export default function ExamRunner({ questions, totalTimeSeconds, onFinish }:{
  questions: Question[];
  totalTimeSeconds: number;
  onFinish: (payload: { selections: Record<string,string|undefined>; timeSpentSeconds: number }) => void;
}){
  const [index, setIndex] = useState(0);
  const [remaining, setRemaining] = useState(totalTimeSeconds);
  const [selections, setSelections] = useState<Record<string,string|undefined>>({});

  const shuffled = useMemo(() => (
    questions.map(q => ({ ...q, answers: [...q.answers].sort(() => Math.random() - 0.5) }))
  ), [questions]);

  useEffect(() => {
    if (remaining <= 0) return onFinish({ selections, timeSpentSeconds: totalTimeSeconds });
    const t = setInterval(() => setRemaining(r => r - 1), 1000);
    return () => clearInterval(t);
  }, [remaining]);

  const q = shuffled[index];
  const progress = Math.round(((index+1) / shuffled.length) * 100);

  function choose(ansId: string){ setSelections(prev => ({ ...prev, [q.id]: prev[q.id] === ansId ? undefined : ansId })); }
  function next(){ setIndex(i => Math.min(i+1, shuffled.length-1)); }
  function prev(){ setIndex(i => Math.max(i-1, 0)); }
  function finish(){ onFinish({ selections, timeSpentSeconds: totalTimeSeconds - remaining }); }

  return (
    <div className="max-w-3xl mx-auto p-4">
      <header className="flex items-center justify-between mb-4">
        <div className="text-xl font-semibold">Pregunta {index+1} / {shuffled.length}</div>
        <div aria-label="Timer" className="font-mono text-lg">⏱ {Math.floor(remaining/60)}:{String(remaining%60).padStart(2,'0')}</div>
      </header>

      <div className="w-full bg-slate-700 h-2 rounded mb-6">
        <div className="h-2 bg-cyan-400 rounded" style={{ width: `${progress}%` }} />
      </div>

      <h2 className="text-2xl font-bold mb-3">{q.text}</h2>
      <div className="grid gap-3">
        {q.answers.map(a => {
          const selected = selections[q.id] === a.id;
          return (
            <button key={a.id} onClick={() => choose(a.id)}
              className={`text-left border rounded-xl p-3 transition hover:scale-[1.01] ${selected ? 'border-cyan-400 bg-cyan-950/40' : 'border-slate-600 hover:border-slate-400'}`}>
              {a.text}
            </button>
          );
        })}
      </div>

      <footer className="mt-6 flex gap-2 justify-between">
        <div className="flex gap-2">
          <button onClick={prev} className="px-4 py-2 rounded-xl border border-slate-600">Anterior</button>
          <button onClick={next} className="px-4 py-2 rounded-xl bg-indigo-600">Siguiente</button>
        </div>
        <button onClick={finish} className="px-4 py-2 rounded-xl bg-amber-500">Finalizar</button>
      </footer>
    </div>
  );
}
