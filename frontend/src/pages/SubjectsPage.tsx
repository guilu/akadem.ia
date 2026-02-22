import { Link, useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { Alert, Card } from 'flowbite-react';
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

  // formatDuration moved to utils

  return (
    <section className="mb-8">
      <div className="flex items-center justify-between mb-2">
        <h2 className="text-xl font-semibold">Exámenes</h2>
        {activeAttemptId && (
          <Link className="btn btn-secondary flex items-center gap-2" to={ROUTES.examAttempt(activeAttemptId)}>
            <ArrowsRepeat className="w-4 h-4" />
            Reanudar examen
          </Link>
        )}
      </div>

      <div id="subjects" className="grid sm:grid-cols-2 gap-4">
        {subjects.map(s => (
          <Card key={s.id} className="border border-secondary/40 bg-bg">
            <div className="text-lg font-semibold">{s.name}</div>
            <div className="text-sm text-text/70">{s.description}</div>
            <div className="mt-3">
              <Link className="btn btn-primary flex items-center gap-2" to={ROUTES.subjectBuilder(s.id)}>
                <Plus className="w-4 h-4" />
                Crear examen
              </Link>
            </div>
          </Card>
        ))}
      </div>

      <div className="mt-10">
        <h3 className="text-lg font-semibold mb-3">Historial de exámenes</h3>
        {loading && (
          <div className="text-text/70">Cargando historial...</div>
        )}
        {!loading && error && (
          <Alert color="failure" className="text-sm">{error}</Alert>
        )}
        {!loading && !error && history.length === 0 && (
          <Card className="border border-secondary/40 bg-bg">
            <div className="text-lg font-semibold">Aún no tienes exámenes realizados</div>
            <div className="text-sm text-text/70">Empieza tu primer simulacro para ver tu progreso aquí.</div>
            <div className="mt-3">
              <Link className="btn btn-primary flex items-center gap-2" to={`${ROUTES.subjects}#subjects`}>
                <Plus className="w-4 h-4" />
                Crear examen
              </Link>
            </div>
          </Card>
        )}
        {!loading && !error && history.length > 0 && (
          <div className="grid gap-3">
            {history.map(h => {
              const finished = Boolean(h.finishedAt);
              const timeSpent = finished && h.startedAt && h.finishedAt
                ? Math.max(0, Math.floor((new Date(h.finishedAt).getTime() - new Date(h.startedAt).getTime()) / 1000))
                : 0;
              return (
                <Card key={h.attemptId} className="border border-secondary/40 bg-bg flex flex-col md:flex-row md:items-center md:justify-between gap-3">
                  <div>
                    <div className="font-semibold">{h.subjectName || 'Materia'}</div>
                    <div className="text-sm text-text/70">{new Date(h.startedAt).toLocaleString()}</div>
                    <div className="text-sm mt-1">Estado: <strong>{finished ? 'Finalizado' : 'En curso'}</strong></div>
                    <div className="text-sm">Resultado: <strong>{(h.score ?? 0)} ({h.percent.toFixed(1)}%)</strong></div>
                    <div className="text-sm">Tiempo empleado: <strong>{finished ? formatDuration(timeSpent) : formatDuration(h.totalTimeSeconds)}</strong></div>
                  </div>
                  <div className="flex gap-2">
                    {finished ? (
                      <button
                        className="btn btn-secondary flex items-center gap-2"
                        onClick={() => onViewResult(h.attemptId)}
                      >
                        <ClipboardCheck className="w-4 h-4" />
                        Ver resultados
                      </button>
                    ) : (
                      <button
                        className="btn btn-primary flex items-center gap-2"
                        onClick={() => onResumeAttempt(h.attemptId)}
                      >
                        <ArrowsRepeat className="w-4 h-4" />
                        Reanudar
                      </button>
                    )}
                  </div>
                </Card>
              );
            })}
          </div>
        )}
      </div>
    </section>
  );
}
