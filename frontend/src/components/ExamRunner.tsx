import { useEffect, useMemo, useState } from 'react';
import { Modal, Button } from 'flowbite-react';

export type Answer = { id: string; text: string };
export type Question = { id: string; text: string; answers: Answer[] };

export default function ExamRunner({ questions, totalTimeSeconds, onFinish, initialSelections, initialIndex, onSelect }:{
  questions: Question[];
  totalTimeSeconds: number;
  onFinish: (payload: { selections: Record<string,string|undefined>; timeSpentSeconds: number }) => void;
  initialSelections?: Record<string,string|undefined>;
  initialIndex?: number;
  onSelect?: (questionId: string, answerId: string | undefined) => void;
}){
  const [index, setIndex] = useState(initialIndex ?? 0);
  const [remaining, setRemaining] = useState(totalTimeSeconds);
  const [selections, setSelections] = useState<Record<string,string|undefined>>(initialSelections || {});
  const [finished, setFinished] = useState(false);
  const [confirmFinish, setConfirmFinish] = useState(false);

  const shuffled = useMemo(() => (
    questions.map(q => ({ ...q, answers: [...q.answers].sort(() => Math.random() - 0.5) }))
  ), [questions]);

  useEffect(() => {
    if (totalTimeSeconds <= 0 || finished) return;
    const t = setInterval(() => setRemaining(r => r - 1), 1000);
    return () => clearInterval(t);
  }, [totalTimeSeconds, finished]);

  useEffect(() => {
    if (finished) return;
    if (remaining <= 0) {
      setFinished(true);
      onFinish({ selections, timeSpentSeconds: totalTimeSeconds });
    }
  }, [remaining, finished]);

  const safeIndex = Math.min(Math.max(index, 0), Math.max(shuffled.length - 1, 0));
  const q = shuffled[safeIndex];
  const progress = shuffled.length ? Math.round(((safeIndex+1) / shuffled.length) * 100) : 0;

  if (totalTimeSeconds <= 0) {
    return (
      <div className="max-w-xl mx-auto p-4 border border-slate-700 rounded-xl">
        <h2 className="text-xl font-semibold mb-2">Tiempo inválido</h2>
        <p className="text-text/70">Configura un tiempo válido para empezar.</p>
      </div>
    );
  }

  if (shuffled.length === 0) {
    return (
      <div className="max-w-xl mx-auto p-4 border border-slate-700 rounded-xl">
        <h2 className="text-xl font-semibold mb-2">No hay preguntas disponibles</h2>
        <p className="text-text/70">Selecciona otra configuración y vuelve a intentarlo.</p>
      </div>
    );
  }

  if (!q) {
    return (
      <div className="max-w-xl mx-auto p-4 border border-slate-700 rounded-xl">
        <h2 className="text-xl font-semibold mb-2">No se pudo cargar la pregunta</h2>
        <p className="text-text/70">Vuelve a intentarlo.</p>
      </div>
    );
  }

  function choose(ansId: string){
    setSelections(prev => {
      const next = prev[q.id] === ansId ? undefined : ansId;
      if (prev[q.id] === next) return prev;
      onSelect?.(q.id, next);
      return { ...prev, [q.id]: next };
    });
  }
  function next(){ setIndex(i => Math.min(i+1, shuffled.length-1)); }
  function prev(){ setIndex(i => Math.max(i-1, 0)); }
  function finish(){
    if (finished) return;
    setFinished(true);
    setConfirmFinish(false);
    onFinish({ selections, timeSpentSeconds: totalTimeSeconds - remaining });
  }

  return (
    <div className="max-w-3xl mx-auto p-4">
      <header className="flex items-center justify-between mb-4">
        <div className="text-xl font-semibold">Pregunta {index+1} / {shuffled.length}</div>
        <div aria-label="Timer" className="font-mono text-lg">⏱ {Math.floor(remaining/60)}:{String(remaining%60).padStart(2,'0')}</div>
      </header>

      <div className="w-full bg-slate-700 h-2 rounded mb-6">
        <div className="h-2 bg-accent rounded" style={{ width: `${progress}%` }} />
      </div>

      <h2 className="text-2xl font-bold mb-3">{q.text}</h2>
      <div className="grid gap-3">
        {q.answers.map(a => {
          const selected = selections[q.id] === a.id;
          return (
            <button key={a.id} onClick={() => choose(a.id)}
              className={`text-left border rounded-xl p-3 transition hover:scale-[1.01] ${selected ? 'border-accent bg-accent/10' : 'border-slate-600 hover:border-slate-400'}`}>
              {a.text}
            </button>
          );
        })}
      </div>

      <footer className="mt-6 flex gap-2 justify-between">
        <div className="flex gap-2">
          <button
            onClick={prev}
            disabled={safeIndex === 0}
            className={`px-4 py-2 rounded-xl border border-slate-600 ${safeIndex === 0 ? 'opacity-40 cursor-not-allowed' : ''}`}
          >
            Anterior
          </button>
          <button
            onClick={next}
            disabled={safeIndex === shuffled.length - 1}
            className={`btn btn-secondary ${safeIndex === shuffled.length - 1 ? 'opacity-40 cursor-not-allowed' : ''}`}
          >
            Siguiente
          </button>
        </div>
        <button onClick={() => setConfirmFinish(true)} className="btn btn-primary">Finalizar</button>
      </footer>

      <Modal show={confirmFinish} onClose={() => setConfirmFinish(false)}>
        <Modal.Header>¿Finalizar examen?</Modal.Header>
        <Modal.Body>
          <p className="text-text/70">Se enviarán tus respuestas y se calculará la nota.</p>
        </Modal.Body>
        <Modal.Footer>
          <Button color="light" onClick={() => setConfirmFinish(false)} className="btn btn-outline">Cancelar</Button>
          <Button onClick={finish} className="btn btn-primary">Finalizar</Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
}
