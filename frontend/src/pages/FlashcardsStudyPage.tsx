import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { apiAuthJson, apiBase } from '../api';

type StudyItem = {
  flashcardId: string;
  front: string;
  back: string;
};

type StudyQueueResponse = {
  items: StudyItem[];
};

type ReviewRequest = {
  flashcardId: string;
  grade: 'AGAIN' | 'HARD' | 'GOOD' | 'EASY';
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

  const token = localStorage.getItem('ak_token') || '';

  useEffect(() => {
    if (!unitId) {
      setError('Falta el unitId.');
      setLoading(false);
      return;
    }
    let mounted = true;
    setLoading(true);
    setError('');
    apiAuthJson<StudyQueueResponse>(`${apiBase}/api/flashcards/study/queue?unitId=${unitId}&limit=50`, token)
      .then((data) => {
        if (!mounted) return;
        setItems(data?.items || []);
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
    if (!loading && items.length > 0 && currentIndex >= items.length) {
      setFinished(true);
    }
  }, [currentIndex, items.length, loading]);

  const remaining = Math.max(items.length - currentIndex, 0);
  const progressPct = items.length ? Math.round((currentIndex / items.length) * 100) : 0;
  const currentLabel = items.length ? `${Math.min(currentIndex + 1, items.length)} de ${items.length} preguntas` : '0 de 0 preguntas';

  const currentItem = items[currentIndex];

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
      setShowAnswer(false);
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
            className="text-slate-500 hover:text-slate-800"
          >
            ←
          </button>
          <div className="text-sm text-slate-500">Resumen</div>
          <div />
        </header>
        <div className="rounded-3xl bg-white dark:bg-slate-900 p-6 shadow-lg">
          <h2 className="text-2xl font-bold">Sesión completada</h2>
          <p className="mt-2 text-secondary">Has respondido {items.length} tarjetas.</p>
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
          className="text-slate-500 hover:text-slate-800"
        >
          ←
        </button>
        <div className="text-sm text-slate-500">Sesión de estudio</div>
        <button className="text-sm font-semibold text-primary" onClick={handleFinish}>Terminar</button>
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
        <div className="relative rounded-3xl bg-white dark:bg-slate-900 p-6 shadow-lg min-h-[260px] flex flex-col">
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
                  className="flex-1 rounded-xl bg-primary/20 dark:bg-primary/30 text-primary px-4 py-2 text-sm font-semibold"
                  onClick={() => handleReview('AGAIN')}
                  disabled={submitting}
                >
                  🔴 Difícil
                </button>
                <button
                  className="flex-1 rounded-xl bg-secondary/15 dark:bg-secondary/25 text-secondary px-4 py-2 text-sm font-semibold"
                  onClick={() => handleReview('HARD')}
                  disabled={submitting}
                >
                  🟡 Dudoso
                </button>
                <button
                  className="flex-1 rounded-xl bg-secondary/30 dark:bg-secondary/35 text-secondary px-4 py-2 text-sm font-semibold"
                  onClick={() => handleReview('GOOD')}
                  disabled={submitting}
                >
                  🟢 Fácil
                </button>
              </div>
            )}
          </div>
        </div>
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
