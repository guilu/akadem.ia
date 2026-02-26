import { useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

const sample = {
  unitName: 'Artículo 1.1',
  total: 10,
  current: 3,
  progress: 0.2,
  time: '00:12'
};

export default function FlashcardsStudyPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const unitId = params.get('unitId');
  const [showAnswer, setShowAnswer] = useState(false);

  const progressPct = Math.round(sample.progress * 100);
  const currentLabel = `${sample.current} de ${sample.total} preguntas`;

  const cta = useMemo(() => {
    if (!showAnswer) return 'Mostrar respuesta';
    return 'Volver a ver';
  }, [showAnswer]);

  return (
    <div className="space-y-6">
      <header className="flex items-center justify-between">
        <button
          onClick={() => navigate(-1)}
          className="text-slate-500 hover:text-slate-800"
        >
          ←
        </button>
        <div className="text-sm text-slate-500">{sample.unitName}</div>
        <div className="text-sm font-semibold text-secondary">{sample.time}</div>
      </header>

      <div className="space-y-2">
        <div className="flex items-center justify-between text-xs text-secondary">
          <span>{currentLabel}</span>
          <span>⏱ {progressPct}%</span>
        </div>
        <div className="h-2 w-full overflow-hidden rounded-full bg-white/80 dark:bg-slate-800">
          <div className="h-full bg-primary" style={{ width: `${progressPct}%` }} />
        </div>
      </div>

      <div className="relative">
        <div className="absolute -left-2 -bottom-2 h-full w-full rounded-3xl bg-secondary/90 dark:bg-secondary/70" />
        <div className="relative rounded-3xl bg-white dark:bg-slate-900 p-6 shadow-lg">
          <div className="text-sm text-secondary">{sample.unitName}</div>
          {!showAnswer ? (
            <div className="mt-4 text-xl font-semibold text-slate-800 dark:text-slate-100">
              Artículo 1.1:
              <div className="mt-2 text-2xl font-bold">¿Qué dice sobre la soberanía nacional?</div>
            </div>
          ) : (
            <div className="mt-4 text-xl font-semibold text-slate-800 dark:text-slate-100">
              La soberanía nacional reside en el pueblo español, del que emanan los poderes del Estado.
            </div>
          )}

          <div className="mt-6 flex flex-col gap-3">
            <button
              onClick={() => setShowAnswer(!showAnswer)}
              className="btn btn-primary"
            >
              {cta}
            </button>

            <div className="flex items-center gap-2">
              <button className="flex-1 rounded-xl bg-primary/20 dark:bg-primary/30 text-primary px-4 py-2 text-sm font-semibold">Difícil</button>
              <button className="flex-1 rounded-xl bg-secondary/15 dark:bg-secondary/25 text-secondary px-4 py-2 text-sm font-semibold">Dudoso</button>
              <button className="flex-1 rounded-xl bg-secondary/30 dark:bg-secondary/35 text-secondary px-4 py-2 text-sm font-semibold">Fácil</button>
            </div>

            {showAnswer && (
              <div className="mt-2 flex items-center justify-between text-sm text-secondary">
                <button className="btn btn-primary">↺ Volver a ver</button>
                <button className="btn btn-primary">✓ Terminar</button>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* actions are inside the card */}
    </div>
  );
}
