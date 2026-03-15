import { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, ArrowRight, CheckCircle, CircleMinus } from 'flowbite-react-icons/outline';

export type Answer = { id: string; text: string };
export type Question = { id: string; text: string; answers: Answer[] };

export default function ExamRunner({ questions, totalTimeSeconds, onFinish, initialSelections, initialIndex, onSelect }: {
  questions: Question[];
  totalTimeSeconds: number;
  onFinish: (payload: { selections: Record<string, string | undefined>; timeSpentSeconds: number }) => void;
  initialSelections?: Record<string, string | undefined>;
  initialIndex?: number;
  onSelect?: (questionId: string, answerId: string | undefined) => void;
}) {
  const [index, setIndex] = useState(initialIndex ?? 0);
  const [remaining, setRemaining] = useState(totalTimeSeconds);
  const [selections, setSelections] = useState<Record<string, string | undefined>>(initialSelections || {});
  const [finished, setFinished] = useState(false);
  const [confirmFinish, setConfirmFinish] = useState(false);

  const shuffled = useMemo(() => {
    function secureShuffleArray<T>(arr: T[]): T[] {
      const a = [...arr];
      for (let i = a.length - 1; i > 0; i--) {
        const buf = new Uint32Array(1);
        crypto.getRandomValues(buf);
        const j = buf[0] % (i + 1);
        [a[i], a[j]] = [a[j], a[i]];
      }
      return a;
    }
    return questions.map(q => ({ ...q, answers: secureShuffleArray(q.answers) }));
  }, [questions]);

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
  const progress = shuffled.length ? Math.round(((safeIndex + 1) / shuffled.length) * 100) : 0;
  const answeredCount = Object.values(selections).filter(Boolean).length;

  const mins = Math.floor(remaining / 60);
  const secs = String(remaining % 60).padStart(2, '0');
  const timerUrgent = remaining <= 60;

  if (totalTimeSeconds <= 0) return (
    <div className="max-w-xl mx-auto border border-secondary/25 rounded-2xl p-8">
      <h2 className="text-xl font-bold mb-2">Tiempo inválido</h2>
      <p className="text-text/60">Configura un tiempo válido para empezar.</p>
    </div>
  );

  if (shuffled.length === 0) return (
    <div className="max-w-xl mx-auto border border-secondary/25 rounded-2xl p-8">
      <h2 className="text-xl font-bold mb-2">No hay preguntas disponibles</h2>
      <p className="text-text/60">Selecciona otra configuración y vuelve a intentarlo.</p>
    </div>
  );

  if (!q) return (
    <div className="max-w-xl mx-auto border border-secondary/25 rounded-2xl p-8">
      <h2 className="text-xl font-bold mb-2">No se pudo cargar la pregunta</h2>
      <p className="text-text/60">Vuelve a intentarlo.</p>
    </div>
  );

  function choose(ansId: string) {
    setSelections(prev => {
      const next = prev[q.id] === ansId ? undefined : ansId;
      if (prev[q.id] === next) return prev;
      onSelect?.(q.id, next);
      return { ...prev, [q.id]: next };
    });
  }
  function next() { setIndex(i => Math.min(i + 1, shuffled.length - 1)); }
  function prev() { setIndex(i => Math.max(i - 1, 0)); }
  function finish() {
    if (finished) return;
    setFinished(true);
    setConfirmFinish(false);
    onFinish({ selections, timeSpentSeconds: totalTimeSeconds - remaining });
  }

  return (
    <div className="max-w-3xl mx-auto">

      {/* ── Header ── */}
      <header className="flex items-center justify-between mb-5">
        <div className="text-sm font-medium text-text/60">
          Pregunta <span className="text-text font-bold">{safeIndex + 1}</span> de <span className="text-text font-bold">{shuffled.length}</span>
          <span className="ml-3 text-text/40">· {answeredCount} respondidas</span>
        </div>
        <div className={`font-mono text-lg font-bold tabular-nums px-4 py-1.5 rounded-full border ${timerUrgent ? 'border-red-400/40 bg-red-400/10 text-red-400' : 'border-secondary/25 text-text/70'}`}>
          ⏱ {mins}:{secs}
        </div>
      </header>

      {/* ── Progress bar ── */}
      <div className="w-full bg-secondary/20 h-1.5 rounded-full mb-8">
        <div className="h-1.5 bg-primary rounded-full transition-all duration-300" style={{ width: `${progress}%` }} />
      </div>

      {/* ── Question ── */}
      <div className="border border-secondary/25 rounded-2xl p-6 mb-5">
        <h2 className="text-xl font-bold leading-snug mb-6">{q.text}</h2>
        <div className="grid gap-3">
          {q.answers.map(a => {
            const selected = selections[q.id] === a.id;
            return (
              <button
                key={a.id}
                onClick={() => choose(a.id)}
                className={`text-left rounded-xl px-5 py-3.5 text-sm border transition-all hover:scale-[1.005] ${
                  selected
                    ? 'border-primary bg-primary/10 text-primary font-medium shadow-sm shadow-primary/10'
                    : 'border-secondary/25 hover:border-secondary/50'
                }`}
              >
                {a.text}
              </button>
            );
          })}
        </div>
      </div>

      {/* ── Navigation ── */}
      <footer className="flex gap-2 justify-between">
        <div className="flex gap-2">
          <button
            onClick={prev}
            disabled={safeIndex === 0}
            className="btn btn-outline rounded-full px-4 py-2 text-sm flex items-center gap-1.5 disabled:opacity-40 disabled:cursor-not-allowed"
          >
            <ArrowLeft className="w-4 h-4" />
            <span className="hidden sm:inline">Anterior</span>
          </button>
          <button
            onClick={next}
            disabled={safeIndex === shuffled.length - 1}
            className="btn btn-outline rounded-full px-4 py-2 text-sm flex items-center gap-1.5 disabled:opacity-40 disabled:cursor-not-allowed"
          >
            <span className="hidden sm:inline">Siguiente</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
        <button
          onClick={() => setConfirmFinish(true)}
          className="btn btn-primary rounded-full px-5 py-2 text-sm shadow-lg shadow-primary/20 flex items-center gap-2"
        >
          <CheckCircle className="w-4 h-4" />
          Finalizar
        </button>
      </footer>

      {/* ── Confirm modal ── */}
      {confirmFinish && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 px-4">
          <div className="bg-bg border border-secondary/25 rounded-2xl p-6 w-full max-w-sm shadow-xl">
            <h3 className="text-lg font-bold mb-2">¿Finalizar examen?</h3>
            <p className="text-sm text-text/60 mb-6">
              Se enviarán tus respuestas ({answeredCount} de {shuffled.length}) y se calculará la nota.
            </p>
            <div className="flex gap-2 justify-end">
              <button onClick={() => setConfirmFinish(false)} className="btn btn-outline rounded-full px-5 py-2 text-sm flex items-center gap-2">
                <CircleMinus className="w-4 h-4" />
                Cancelar
              </button>
              <button onClick={finish} className="btn btn-primary rounded-full px-5 py-2 text-sm shadow-lg shadow-primary/20 flex items-center gap-2">
                <CheckCircle className="w-4 h-4" />
                Finalizar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
