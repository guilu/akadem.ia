import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowLeft } from 'flowbite-react-icons/outline';
import { apiAuthJson, apiBase } from '../api';

type IntervalHints = {
  again: string;
  good: string;
  easy: string;
};

type ReviewState = 'NEW' | 'LEARNING' | 'REVIEW';

type StudyItem = {
  flashcardId: string;
  front: string;
  back: string;
  state?: ReviewState;
  intervalHints?: IntervalHints;
};

type StudyNextResponse = StudyItem;

type StudyQueueResponse = {
  new: number;
  due: number;
  learning: number;
};

type ReviewRequest = {
  flashcardId: string;
  grade: 'AGAIN' | 'GOOD' | 'EASY';
  reviewedAt: string;
};

export default function FlashcardsStudyPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const unitId = params.get('unitId');
  const [showAnswer, setShowAnswer] = useState(false);
  const [items, setItems] = useState<StudyItem[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [finished, setFinished] = useState(false);
  const [answeredCount, setAnsweredCount] = useState(0);
  const [queueCounts, setQueueCounts] = useState({ new: 0, due: 0, learning: 0 });

  const token = localStorage.getItem('ak_token') || '';

  const fetchNext = async () => {
    const data = await apiAuthJson<StudyNextResponse | undefined>(
      `${apiBase}/api/flashcards/study/next?unitId=${unitId}`,
      token
    );
    return data || null;
  };

  const fetchQueue = async () => {
    const data = await apiAuthJson<StudyQueueResponse>(
      `${apiBase}/api/flashcards/study/queue?unitId=${unitId}`,
      token
    );
    return data;
  };

  const currentItem = items[currentIndex];
  const remaining = Math.max(queueCounts.new + queueCounts.due + queueCounts.learning, 0);
  const totalInteractions = answeredCount + remaining;
  const progressPct = totalInteractions ? Math.round((answeredCount / totalInteractions) * 100) : 0;

  useEffect(() => {
    if (!unitId) {
      setError('Falta el unitId.');
      setLoading(false);
      return;
    }
    let mounted = true;
    setLoading(true);
    setError('');
    Promise.all([fetchQueue(), fetchNext()])
      .then(([queue, data]) => {
        if (!mounted) return;
        setQueueCounts(queue);
        setAnsweredCount(0);
        if (!data) {
          setItems([]);
          setFinished(true);
          return;
        }
        setItems([data]);
        setCurrentIndex(0);
      })
      .catch(() => {
        if (!mounted) return;
        setError('No se pudo cargar la sesión de estudio.');
      })
      .finally(() => {
        if (!mounted) return;
        setLoading(false);
      });
    return () => { mounted = false; };
  }, [token, unitId]);

  useEffect(() => {
    if (!loading && remaining === 0 && !currentItem) {
      setFinished(true);
    }
  }, [remaining, loading, currentItem]);

  const handleReview = async (grade: ReviewRequest['grade']) => {
    if (!currentItem || submitting) return;
    setSubmitting(true);
    try {
      await apiAuthJson(`${apiBase}/api/flashcards/study/review`, token, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          flashcardId: currentItem.flashcardId,
          grade,
          reviewedAt: new Date().toISOString()
        } satisfies ReviewRequest)
      });
      const [newQueue, next] = await Promise.all([fetchQueue(), fetchNext()]);
      if (newQueue) setQueueCounts(newQueue);
      setAnsweredCount((prev) => prev + 1);
      setShowAnswer(false);
      if (!next) {
        setFinished(true);
        return;
      }
      setItems((prev) => [...prev, next]);
      setCurrentIndex((prev) => prev + 1);
    } catch {
      setError('No se pudo registrar la respuesta.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleFinish = () => {
    setConfirmOpen(true);
  };

  const confirmFinish = () => {
    setConfirmOpen(false);
    setFinished(true);
  };

  if (loading) {
    return <div className="text-sm text-secondary">Cargando sesión...</div>;
  }

  if (error) {
    return (
      <div className="space-y-3">
        <p className="text-sm text-red-500">{error}</p>
        <button className="btn btn-primary" onClick={() => navigate('/flashcards')}>Volver</button>
      </div>
    );
  }

  if (finished) {
    return (
      <div className="space-y-4">
        <header className="flex items-center justify-between">
          <button
            onClick={() => navigate('/flashcards')}
            className="btn btn-secondary h-10 w-10 rounded-full p-0 flex items-center justify-center"
            aria-label="Volver"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <h1 className="text-3xl font-bold text-slate-900 dark:text-white">Resumen</h1>
          <div className="h-10 w-10" />
        </header>
        <div className="rounded-3xl bg-white dark:bg-slate-900 p-6 shadow-lg">
          <h2 className="text-2xl font-bold">Sesión completada</h2>
          <p className="mt-2 text-secondary">Has respondido {answeredCount} tarjetas.</p>
          <button className="btn btn-primary mt-6" onClick={() => navigate('/flashcards')}>Volver a unidades</button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <header className="flex items-center justify-between">
        <button
          onClick={() => navigate(-1)}
          className="btn btn-secondary h-10 w-10 rounded-full p-0 flex items-center justify-center"
          aria-label="Volver"
        >
          <ArrowLeft className="h-5 w-5" />
        </button>
        <h1 className="text-3xl font-bold text-slate-900 dark:text-white">Sesión de estudio</h1>
        <div className="h-10 w-10" />
      </header>

      <div className="space-y-2">
        <div className="flex items-center justify-between text-xs text-secondary">
          <div className="flex flex-wrap items-center gap-3">
            <span className="text-red-500">🔴 {queueCounts.learning} learning</span>
            <span className="text-amber-500">🟡 {queueCounts.due} due</span>
            <span className="text-emerald-600">🟢 {queueCounts.new} new</span>
          </div>
          <span>⏱ {progressPct}%</span>
        </div>
        <div className="h-2 w-full overflow-hidden rounded-full bg-white/80 dark:bg-slate-800">
          <div className="h-full bg-primary" style={{ width: `${progressPct}%` }} />
        </div>
      </div>

      <div className="relative">
        <div className="absolute -left-2 -bottom-2 h-full w-full rounded-3xl bg-secondary/90 dark:bg-secondary/70" />
        <div className="relative rounded-3xl bg-white dark:bg-slate-900 p-6 shadow-lg min-h-[55vh] flex flex-col">
          {!showAnswer ? (
            <div className="mt-4 text-xl font-semibold text-slate-800 dark:text-slate-100">
              {currentItem?.front || 'Sin tarjetas disponibles.'}
            </div>
          ) : (
            <div className="mt-4 text-xl font-semibold text-slate-800 dark:text-slate-100">
              {currentItem?.back}
            </div>
          )}

          <div className="mt-auto pt-6 flex flex-col gap-3">
            {!showAnswer && (
              <button
                onClick={() => setShowAnswer(true)}
                className="btn btn-primary"
              >
                Mostrar respuesta
              </button>
            )}

            {showAnswer && (
              <div className="flex items-center gap-2">
                <button
                  className="flex-1 rounded-xl bg-red-500/20 text-red-700 dark:text-red-200 px-4 py-2 text-sm font-semibold"
                  onClick={() => handleReview('AGAIN')}
                  disabled={submitting}
                >
                  <div className="flex flex-col items-center">
                    <span>Again</span>
                    <span className="text-xs text-red-700/70 dark:text-red-200/80">{currentItem?.intervalHints?.again}</span>
                  </div>
                </button>
                <button
                  className="flex-1 rounded-xl bg-amber-400/20 text-amber-700 dark:text-amber-100 px-4 py-2 text-sm font-semibold"
                  onClick={() => handleReview('GOOD')}
                  disabled={submitting}
                >
                  <div className="flex flex-col items-center">
                    <span>Good</span>
                    <span className="text-xs text-amber-700/70 dark:text-amber-100/80">{currentItem?.intervalHints?.good}</span>
                  </div>
                </button>
                <button
                  className="flex-1 rounded-xl bg-emerald-400/20 text-emerald-700 dark:text-emerald-100 px-4 py-2 text-sm font-semibold"
                  onClick={() => handleReview('EASY')}
                  disabled={submitting}
                >
                  <div className="flex flex-col items-center">
                    <span>Easy</span>
                    <span className="text-xs text-emerald-700/70 dark:text-emerald-100/80">{currentItem?.intervalHints?.easy}</span>
                  </div>
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="fixed bottom-6 right-6">
        <button className="btn btn-primary" onClick={handleFinish}>Terminar</button>
      </div>

      {confirmOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="w-full max-w-sm rounded-2xl bg-white dark:bg-slate-900 p-6 shadow-xl">
            <h3 className="text-lg font-semibold">Te quedan {remaining} tarjetas</h3>
            <p className="mt-2 text-sm text-secondary">¿Quieres terminar la sesión?</p>
            <div className="mt-4 flex justify-end gap-2">
              <button className="btn btn-secondary" onClick={() => setConfirmOpen(false)}>Cancelar</button>
              <button className="btn btn-primary" onClick={confirmFinish}>Terminar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
