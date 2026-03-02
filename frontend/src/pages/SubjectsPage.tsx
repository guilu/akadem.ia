import { Link } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { formatDuration } from '../utils/format';
import { Plus, ArrowsRepeat, ClipboardCheck } from 'flowbite-react-icons/outline';
import { apiAuthJson, apiBase } from '../api';
import type { Subject, ExamAttemptSummary } from '../types';
import { ROUTES } from '../constants/routes';

export default function SubjectsPage({ subjects, activeAttemptId, token, onUnauthorized, onViewResult, onResumeAttempt }: {
  subjects: Subject[];
  activeAttemptId?: string;
  token: string;
  onUnauthorized: () => void;
  onViewResult: (attemptId: string) => void;
  onResumeAttempt: (attemptId: string) => void;
}) {
  const [history, setHistory] = useState<ExamAttemptSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showAll, setShowAll] = useState(false);

  const INITIAL_SHOW = 5;

  function scoreColor(percent: number) {
    if (percent < 50) return 'text-red-500';
    if (percent <= 60) return 'text-orange-500';
    if (percent <= 80) return 'text-yellow-500';
    return 'text-lime-500';
  }

  useEffect(() => {
    setLoading(true);
    setError('');
    apiAuthJson<ExamAttemptSummary[]>(`${apiBase}/api/exams/attempts`, token)
      .then(setHistory)
      .catch(err => {
        if (err?.status === 401) onUnauthorized();
        setError('No se pudo cargar el historial.');
        setHistory([]);
      })
      .finally(() => setLoading(false));
  }, [token, apiBase]);

  return (
    <section className="mb-8">

      {/* ── Header ── */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight">
            Elige tu{' '}
            <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">
              simulacro
            </span>
          </h1>
          <p className="text-text/55 text-sm mt-1">Selecciona una materia para configurar tu examen.</p>
        </div>
        {activeAttemptId && (
          <Link
            className="btn btn-outline rounded-full px-5 py-2.5 text-sm flex items-center gap-2"
            to={ROUTES.examAttempt(activeAttemptId)}
          >
            <ArrowsRepeat className="w-4 h-4" />
            Reanudar examen
          </Link>
        )}
      </div>

      {/* ── Subject cards ── */}
      <div className="grid sm:grid-cols-2 gap-4 mb-12">
        {subjects.map(s => (
          <article
            key={s.id}
            className="group border border-secondary/25 rounded-2xl p-6 transition-all hover:border-primary/40 hover:shadow-lg hover:shadow-primary/5 hover:-translate-y-0.5"
          >
            <div className="w-10 h-10 rounded-xl bg-primary/10 text-primary flex items-center justify-center mb-4 transition-colors group-hover:bg-primary/15">
              <Plus className="w-5 h-5" />
            </div>
            <div className="text-lg font-bold mb-1">{s.name}</div>
            <div className="text-sm text-text/55 mb-5 leading-relaxed">{s.description}</div>
            <Link
              className="btn btn-primary rounded-full px-5 py-2 text-sm shadow-sm shadow-primary/15 inline-flex items-center gap-2"
              to={ROUTES.subjectBuilder(s.id)}
            >
              <Plus className="w-4 h-4" />
              Crear examen
            </Link>
          </article>
        ))}
      </div>

      {/* ── History ── */}
      <div>
        <h2 className="text-xl font-extrabold tracking-tight mb-4">Historial de exámenes</h2>

        {loading && (
          <div className="text-sm text-text/55">Cargando historial...</div>
        )}

        {!loading && error && (
          <div className="rounded-xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm text-red-400">{error}</div>
        )}

        {!loading && !error && history.length === 0 && (
          <div className="border border-secondary/25 rounded-2xl p-6">
            <div className="text-base font-bold mb-1">Aún no tienes exámenes realizados</div>
            <div className="text-sm text-text/55">Empieza tu primer simulacro para ver tu progreso aquí.</div>
          </div>
        )}

        {!loading && !error && history.length > 0 && (
          <div className="grid gap-3">
            {(showAll ? history : history.slice(0, INITIAL_SHOW)).map(h => {
              const finished = Boolean(h.finishedAt);
              const timeSpent = finished && h.startedAt && h.finishedAt
                ? Math.max(0, Math.floor((new Date(h.finishedAt).getTime() - new Date(h.startedAt).getTime()) / 1000))
                : 0;
              const pct = h.percent ?? 0;
              const scoreColor = pct < 50 ? 'text-red-400' : pct < 70 ? 'text-orange-400' : pct < 90 ? 'text-yellow-500' : 'text-lime-500';

              return (
                <div
                  key={h.attemptId}
                  className="border border-secondary/25 rounded-2xl px-5 py-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"
                >
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="font-bold text-sm">{h.subjectName || 'Materia'}</span>
                      <span className={`text-xs px-2 py-0.5 rounded-full border font-medium ${
                        finished
                          ? 'border-lime-400/30 bg-lime-400/10 text-lime-500'
                          : 'border-accent/30 bg-accent/10 text-accent'
                      }`}>
                        {finished ? 'Finalizado' : 'En curso'}
                      </span>
                    </div>
                    <div className="text-xs text-text/45 mb-2">{new Date(h.startedAt).toLocaleString()}</div>
                    <div className="flex flex-wrap gap-x-5 gap-y-1 text-xs text-text/60">
                      <span>Resultado: <strong className={`${scoreColor}`}>{(h.score ?? 0)} ({pct.toFixed(1)}%)</strong></span>
                      <span>Tiempo: <strong>{finished ? formatDuration(timeSpent) : formatDuration(h.totalTimeSeconds)}</strong></span>
                    </div>
                  </div>
                  <div>
                    {finished ? (
                      <button
                        className="btn btn-outline rounded-full px-5 py-2 text-sm flex items-center gap-2"
                        onClick={() => onViewResult(h.attemptId)}
                      >
                        <ClipboardCheck className="w-4 h-4" />
                        Ver resultados
                      </button>
                    ) : (
                      <button
                        className="btn btn-primary rounded-full px-5 py-2 text-sm shadow-sm shadow-primary/15 flex items-center gap-2"
                        onClick={() => onResumeAttempt(h.attemptId)}
                      >
                        <ArrowsRepeat className="w-4 h-4" />
                        Reanudar
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
            {history.length > INITIAL_SHOW && (
              <button
                className="btn btn-outline w-full"
                onClick={() => setShowAll(v => !v)}
              >
                {showAll ? 'Ver menos' : `Ver ${history.length - INITIAL_SHOW} más`}
              </button>
            )}
          </div>
        )}
      </div>
    </section>
  );
}
