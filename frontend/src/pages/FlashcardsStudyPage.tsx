import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowLeft, CircleMinus, CheckCircle } from 'flowbite-react-icons/outline';
import { apiAuthJson, apiBase } from '../api';

type IntervalHints = { again: string; good: string; easy: string };
type ReviewState = 'NEW' | 'LEARNING' | 'REVIEW';
type StudyItem = { flashcardId: string; front: string; back: string; state?: ReviewState; intervalHints?: IntervalHints };
type StudyQueueResponse = { new: number; due: number; learning: number };
type ReviewRequest = { flashcardId: string; grade: 'AGAIN' | 'GOOD' | 'EASY'; reviewedAt: string };

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
    const data = await apiAuthJson<StudyItem | undefined>(`${apiBase}/api/flashcards/study/next?unitId=${unitId}`, token);
    return data || null;
  };
  const fetchQueue = async () => apiAuthJson<StudyQueueResponse>(`${apiBase}/api/flashcards/study/queue?unitId=${unitId}`, token);

  const currentItem = items[currentIndex];
  const remaining = Math.max(queueCounts.new + queueCounts.due + queueCounts.learning, 0);
  const totalInteractions = answeredCount + remaining;
  const progressPct = totalInteractions ? Math.round((answeredCount / totalInteractions) * 100) : 0;

  useEffect(() => {
    if (!unitId) { setError('Falta el unitId.'); setLoading(false); return; }
    let mounted = true;
    setLoading(true);
    setError('');
    Promise.all([fetchQueue(), fetchNext()])
      .then(([queue, data]) => {
        if (!mounted) return;
        setQueueCounts(queue);
        setAnsweredCount(0);
        if (!data) { setItems([]); setFinished(true); return; }
        setItems([data]);
        setCurrentIndex(0);
      })
      .catch(() => { if (mounted) setError('No se pudo cargar la sesión de estudio.'); })
      .finally(() => { if (mounted) setLoading(false); });
    return () => { mounted = false; };
  }, [token, unitId]);

  useEffect(() => {
    if (!loading && remaining === 0 && !currentItem) setFinished(true);
  }, [remaining, loading, currentItem]);

  const handleReview = async (grade: ReviewRequest['grade']) => {
    if (!currentItem || submitting) return;
    setSubmitting(true);
    try {
      await apiAuthJson(`${apiBase}/api/flashcards/study/review`, token, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ flashcardId: currentItem.flashcardId, grade, reviewedAt: new Date().toISOString() } satisfies ReviewRequest)
      });
      const [newQueue, next] = await Promise.all([fetchQueue(), fetchNext()]);
      if (newQueue) setQueueCounts(newQueue);
      setAnsweredCount((prev) => prev + 1);
      setShowAnswer(false);
      if (!next) { setFinished(true); return; }
      setItems((prev) => [...prev, next]);
      setCurrentIndex((prev) => prev + 1);
    } catch {
      setError('No se pudo registrar la respuesta.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <div className="text-sm text-text/55">Cargando sesión...</div>;
  }

  if (error) {
    return (
      <div className="space-y-4">
        <div className="rounded-xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm text-red-400">{error}</div>
        <button className="btn btn-primary rounded-full px-5 py-2 text-sm" onClick={() => navigate('/flashcards')}>Volver</button>
      </div>
    );
  }

  if (finished) {
    return (
      <div className="space-y-6">
        <header className="flex items-center gap-4">
          <button
            onClick={() => navigate('/flashcards')}
            className="btn btn-outline h-10 w-10 rounded-full p-0 flex items-center justify-center"
            aria-label="Volver"
          >
            <ArrowLeft className="h-4 w-4" />
          </button>
          <h1 className="text-2xl font-extrabold tracking-tight">Sesión completada</h1>
        </header>
        <div className="border border-secondary/25 rounded-2xl p-8 text-center">
          <div className="text-5xl mb-4">🎉</div>
          <div className="text-xl font-bold mb-2">¡Bien hecho!</div>
          <p className="text-text/55 text-sm mb-6">Has respondido <strong>{answeredCount}</strong> tarjetas en esta sesión.</p>
          <button
            className="btn btn-primary rounded-full px-8 py-2.5 text-sm shadow-lg shadow-primary/20 inline-flex items-center gap-2"
            onClick={() => navigate('/flashcards')}
          >
            <CheckCircle className="w-4 h-4" />
            Volver a unidades
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-5 max-w-2xl mx-auto">

      {/* ── Header ── */}
      <header className="flex items-center justify-between">
        <button
          onClick={() => navigate(-1)}
          className="btn btn-outline h-9 w-9 rounded-full p-0 flex items-center justify-center"
          aria-label="Volver"
        >
          <ArrowLeft className="h-4 w-4" />
        </button>
        <h1 className="text-xl font-extrabold tracking-tight">Sesión de estudio</h1>
        <div className="w-9" />
      </header>

      {/* ── Progress ── */}
      <div className="space-y-1.5">
        <div className="flex items-center justify-between text-xs text-text/50">
          <div className="flex flex-wrap items-center gap-3">
            <span className="text-red-400">🔴 {queueCounts.learning} aprendiendo</span>
            <span className="text-accent">🟡 {queueCounts.due} pendientes</span>
            <span className="text-lime-500">🟢 {queueCounts.new} nuevas</span>
          </div>
          <span className="font-medium">{progressPct}%</span>
        </div>
        <div className="h-1.5 w-full rounded-full bg-secondary/20 overflow-hidden">
          <div className="h-full bg-primary rounded-full transition-all duration-500" style={{ width: `${progressPct}%` }} />
        </div>
      </div>

      {/* ── Card ── */}
      <div className="relative">
        <div className="absolute -left-1.5 -bottom-1.5 h-full w-full rounded-2xl bg-bg brightness-90 dark:brightness-75" />
        <div className="relative border border-secondary/25 rounded-2xl p-7 min-h-[52vh] flex flex-col bg-bg">
          <div className="flex-1">
            {!showAnswer ? (
              <p className="text-xl font-semibold leading-relaxed">{currentItem?.front || 'Sin tarjetas disponibles.'}</p>
            ) : (
              <p className="text-xl font-semibold leading-relaxed">{currentItem?.back}</p>
            )}
          </div>

          <div className="pt-6 flex flex-col gap-3">
            {!showAnswer ? (
              <button
                onClick={() => setShowAnswer(true)}
                className="btn btn-primary rounded-full py-3 text-base shadow-lg shadow-primary/20 flex items-center justify-center gap-2"
              >
                Mostrar respuesta
              </button>
            ) : (
              <div className="flex items-stretch gap-2">
                <button
                  className="flex-1 rounded-xl border border-red-400/30 bg-red-400/10 text-red-400 hover:bg-red-400/15 transition-colors px-4 py-3 text-sm font-semibold disabled:opacity-50"
                  onClick={() => handleReview('AGAIN')}
                  disabled={submitting}
                >
                  <div className="flex flex-col items-center gap-0.5">
                    <span>Again</span>
                    <span className="text-xs text-red-400/60">{currentItem?.intervalHints?.again}</span>
                  </div>
                </button>
                <button
                  className="flex-1 rounded-xl border border-yellow-400/30 bg-yellow-400/10 text-yellow-500 hover:bg-yellow-400/15 transition-colors px-4 py-3 text-sm font-semibold disabled:opacity-50"
                  onClick={() => handleReview('GOOD')}
                  disabled={submitting}
                >
                  <div className="flex flex-col items-center gap-0.5">
                    <span>Good</span>
                    <span className="text-xs text-yellow-500/60">{currentItem?.intervalHints?.good}</span>
                  </div>
                </button>
                <button
                  className="flex-1 rounded-xl border border-lime-400/30 bg-lime-400/10 text-lime-500 hover:bg-lime-400/15 transition-colors px-4 py-3 text-sm font-semibold disabled:opacity-50"
                  onClick={() => handleReview('EASY')}
                  disabled={submitting}
                >
                  <div className="flex flex-col items-center gap-0.5">
                    <span>Easy</span>
                    <span className="text-xs text-lime-500/60">{currentItem?.intervalHints?.easy}</span>
                  </div>
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* ── Finish button ── */}
      <div className="flex justify-end">
        <button
          className="btn btn-outline rounded-full px-5 py-2 text-sm flex items-center gap-2"
          onClick={() => setConfirmOpen(true)}
        >
          <CircleMinus className="w-4 h-4" />
          Terminar sesión
        </button>
      </div>

      {/* ── Confirm modal ── */}
      {confirmOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 px-4">
          <div className="w-full max-w-sm border border-secondary/25 rounded-2xl bg-bg p-6 shadow-xl">
            <h3 className="text-lg font-bold mb-2">Terminar sesión</h3>
            <p className="text-sm text-text/60 mb-6">
              {remaining > 0 ? `Quedan ${remaining} tarjetas pendientes.` : 'Has completado todas las tarjetas.'} ¿Quieres terminar igualmente?
            </p>
            <div className="flex justify-end gap-2">
              <button className="btn btn-outline rounded-full px-5 py-2 text-sm" onClick={() => setConfirmOpen(false)}>
                Cancelar
              </button>
              <button className="btn btn-primary rounded-full px-5 py-2 text-sm shadow-sm shadow-primary/15" onClick={() => { setConfirmOpen(false); setFinished(true); }}>
                Terminar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
