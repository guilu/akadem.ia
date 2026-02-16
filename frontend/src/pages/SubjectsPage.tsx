import { Link } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { apiAuthJson, apiBase } from '../api';
import type { Subject, ExamAttemptSummary } from '../types';

export default function SubjectsPage({ subjects, activeAttemptId, token, onUnauthorized, onViewResult, onResumeAttempt }: {
  subjects: Subject[];
  activeAttemptId?: string;
  token: string;
  onUnauthorized: () => void;
  onViewResult: (attemptId: string) => void;
  onResumeAttempt: (attemptId: string) => void;
}) {
  const navigate = useNavigate();
  const [history, setHistory] = useState<ExamAttemptSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

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

  function formatDuration(seconds: number) {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}m ${String(secs).padStart(2,'0')}s`;
  }

  return (
    <section className="mb-8">
      <div className="flex items-center justify-between mb-2">
        <h2 className="text-xl font-semibold">Exámenes</h2>
        {activeAttemptId && (
          <Link className="px-3 py-2 rounded-xl bg-indigo-600 inline-flex" to={`/exams/attempts/${activeAttemptId}`}>
            Reanudar examen
          </Link>
        )}
      </div>

      <div className="grid sm:grid-cols-2 gap-4">
        {subjects.map(s => (
          <div key={s.id} className="border border-slate-700 rounded-xl p-4">
            <div className="text-lg font-semibold">{s.name}</div>
            <div className="text-sm text-slate-400">{s.description}</div>
            <div className="mt-3">
              <Link className="px-3 py-2 rounded-xl bg-cyan-600 inline-flex" to={`/subjects/${s.id}/builder`}>
                Crear examen
              </Link>
            </div>
          </div>
        ))}
      </div>

      <div className="mt-10">
        <h3 className="text-lg font-semibold mb-3">Historial de exámenes</h3>
        {loading && (
          <div className="text-slate-400">Cargando historial...</div>
        )}
        {!loading && error && (
          <div className="text-slate-400">{error}</div>
        )}
        {!loading && !error && history.length === 0 && (
          <div className="text-slate-400">Aún no tienes exámenes realizados.</div>
        )}
        {!loading && !error && history.length > 0 && (
          <div className="grid gap-3">
            {history.map(h => {
              const finished = Boolean(h.finishedAt);
              const timeSpent = finished && h.startedAt && h.finishedAt
                ? Math.max(0, Math.floor((new Date(h.finishedAt).getTime() - new Date(h.startedAt).getTime()) / 1000))
                : 0;
              return (
                <div key={h.attemptId} className="border border-slate-700 rounded-xl p-4 flex flex-col md:flex-row md:items-center md:justify-between gap-3">
                  <div>
                    <div className="font-semibold">{h.subjectName || 'Materia'}</div>
                    <div className="text-sm text-slate-400">{new Date(h.startedAt).toLocaleString()}</div>
                    <div className="text-sm mt-1">Estado: <strong>{finished ? 'Finalizado' : 'En curso'}</strong></div>
                    <div className="text-sm">Resultado: <strong>{(h.score ?? 0)} ({h.percent.toFixed(1)}%)</strong></div>
                    <div className="text-sm">Tiempo empleado: <strong>{finished ? formatDuration(timeSpent) : formatDuration(h.totalTimeSeconds)}</strong></div>
                  </div>
                  <div className="flex gap-2">
                    {finished ? (
                      <button
                        className="px-3 py-2 rounded bg-indigo-600"
                        onClick={() => onViewResult(h.attemptId)}
                      >
                        Ver resultados
                      </button>
                    ) : (
                      <button
                        className="px-3 py-2 rounded bg-cyan-600"
                        onClick={() => onResumeAttempt(h.attemptId)}
                      >
                        Reanudar
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </section>
  );
}
