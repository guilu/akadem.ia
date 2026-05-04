import { useEffect, useMemo, useState } from 'react';
import { Navigate, useNavigate, useParams } from 'react-router-dom';
import ExamRunner, { Question } from '../components/ExamRunner';
import { apiJson, apiBase } from '../api';
import { timeoutMessage } from '../utils/messages';
import { ROUTES } from '../constants/routes';
import type { ExamResult } from '../types';

type AttemptResponse = {
  attemptId: string;
  totalTimeSeconds: number;
  questions: Array<{ id: string; text: string; answers: Array<{ id: string; text: string }>; selectedAnswerId?: string | null }>;
  nextQuestionIndex: number;
  finished: boolean;
};

export default function ExamAttemptPage({ onUnauthorized, onFinish }:{
  onUnauthorized: () => void;
  onFinish: (result: ExamResult) => void;
}){
  const { attemptId } = useParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>('');
  const [attempt, setAttempt] = useState<AttemptResponse | null>(null);
  // timeoutMessage from utils

  const selections = useMemo(() => {
    if (!attempt) return {} as Record<string, string | undefined>;
    const map: Record<string, string | undefined> = {};
    attempt.questions.forEach(q => { if (q.selectedAnswerId) map[q.id] = q.selectedAnswerId; });
    return map;
  }, [attempt]);

  useEffect(() => {
    if (!attemptId) return;
    setLoading(true);
    setError('');
    apiJson<AttemptResponse>(`${apiBase}/api/exams/attempts/${attemptId}`, { timeoutMs: 15000 })
      .then(data => setAttempt(data))
      .catch((err: unknown) => {
        if (err && typeof err === 'object' && 'status' in err && (err as { status: number }).status === 401) onUnauthorized();
        if (err && typeof err === 'object' && 'code' in err && (err as { code: string }).code === 'timeout') {
          setError(timeoutMessage);
          return;
        }
        setError('No se pudo cargar el intento');
      })
      .finally(() => setLoading(false));
  }, [attemptId, apiBase]);

  if (!attemptId) return <Navigate to={ROUTES.subjects} replace />;

  if (loading) {
    return (
      <div className="max-w-xl mx-auto border border-secondary/25 rounded-2xl p-8">
        <h2 className="text-xl font-bold mb-2">Cargando intento...</h2>
        <p className="text-text/55">Espera un momento.</p>
      </div>
    );
  }

  if (error || !attempt) {
    return (
      <main className="min-h-[calc(100vh-4rem)] flex flex-col items-center justify-center px-4 relative overflow-hidden">
        <div className="pointer-events-none absolute -top-24 -right-24 w-96 h-96 bg-primary/5 rounded-full blur-3xl" />
        <div className="pointer-events-none absolute bottom-12 -left-24 w-64 h-64 bg-accent/5 rounded-full blur-3xl" />

        <div className="max-w-xl w-full text-center space-y-8 py-12 relative z-10">
          {/* Icon */}
          <div className="relative inline-flex items-center justify-center">
            <div className="absolute inset-0 bg-primary/10 rounded-full blur-2xl animate-pulse" />
            <div className="relative bg-card p-8 rounded-full shadow-sm border border-secondary/15">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-16 h-16 text-primary">
                <path fillRule="evenodd" d="M9.401 3.003c1.155-2 4.043-2 5.197 0l7.355 12.748c1.154 2-.29 4.5-2.599 4.5H4.645c-2.309 0-3.752-2.5-2.598-4.5L9.4 3.003zM12 8.25a.75.75 0 01.75.75v3.75a.75.75 0 01-1.5 0V9a.75.75 0 01.75-.75zm0 8.25a.75.75 0 100-1.5.75.75 0 000 1.5z" clipRule="evenodd" />
              </svg>
            </div>
          </div>

          {/* Content */}
          <div className="space-y-4">
            <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight text-text leading-tight">
              Error al reanudar el intento
            </h1>
            <p className="text-lg md:text-xl text-secondary leading-relaxed px-6">
              No hemos podido cargar tu sesión de examen. Por favor, inténtalo de nuevo o vuelve a la selección de temas.
            </p>
          </div>

          {/* Actions */}
          <div className="flex flex-col items-center gap-6 pt-4">
            <button
              type="button"
              onClick={() => window.location.reload()}
              className="bg-primary text-bg px-10 py-4 rounded-xl font-bold text-lg hover:opacity-90 active:scale-95 transition-all shadow-lg shadow-primary/20"
            >
              Intentar de nuevo
            </button>
            <p className="text-sm font-medium text-secondary">
              ¿Sigues teniendo problemas?{' '}
              <a
                className="text-primary hover:underline font-bold"
                href={`mailto:soporte@akadem.ia?subject=${encodeURIComponent('Error al reanudar el intento')}`}
              >
                Contactar con soporte
              </a>
            </p>
          </div>

          {/* Info badges */}
          <div className="flex justify-center items-center gap-10 sm:gap-12 pt-8 opacity-40">
            <div className="flex flex-col items-center gap-2">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-6 h-6 text-secondary">
                <path strokeLinecap="round" strokeLinejoin="round" d="M2.25 15a4.5 4.5 0 004.5 4.5H18a3.75 3.75 0 001.332-7.257 3 3 0 00-3.758-3.848 5.25 5.25 0 00-10.233 2.33A4.502 4.502 0 002.25 15z" />
                <path strokeLinecap="round" strokeLinejoin="round" d="M4.5 4.5l15 15" />
              </svg>
              <span className="text-xs uppercase tracking-widest text-secondary font-bold">Sync Error</span>
            </div>
            <div className="flex flex-col items-center gap-2">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-6 h-6 text-secondary">
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span className="text-xs uppercase tracking-widest text-secondary font-bold">Session Timeout</span>
            </div>
            <div className="flex flex-col items-center gap-2">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-6 h-6 text-secondary">
                <path strokeLinecap="round" strokeLinejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z" />
              </svg>
              <span className="text-xs uppercase tracking-widest text-secondary font-bold">Secure Connection</span>
            </div>
          </div>
        </div>
      </main>
    );
  }

  const questions: Question[] = attempt.questions.map(q => ({ id: q.id, text: q.text, answers: q.answers }));

  async function handleSelect(questionId: string, answerId: string | undefined){
    const current = selections[questionId];
    if (current === answerId) return;
    try {
      await apiJson(`${apiBase}/api/exams/attempts/${attemptId}/answers/${questionId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ selectedAnswerId: answerId }),
        timeoutMs: 15000
      });
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'status' in err && (err as { status: number }).status === 401) onUnauthorized();
      if (err && typeof err === 'object' && 'code' in err && (err as { code: string }).code === 'timeout') {
        alert(timeoutMessage);
        return;
      }
      alert('No se pudo guardar la respuesta.');
    }
  }

  async function finishExam(payload:{ selections: Record<string,string|undefined> }){
    const filtered: Record<string,string> = {};
    Object.entries(payload.selections).forEach(([q, a]) => { if(a) filtered[q] = a; });
    try {
      const result = await apiJson<ExamResult>(`${apiBase}/api/exams/attempts/${attemptId}/submit`, {
        method: 'POST',
        headers: { 'Content-Type':'application/json' },
        body: JSON.stringify({ selections: filtered }),
        timeoutMs: 15000
      });
      sessionStorage.removeItem('akdmia.activeAttemptId');
      onFinish(result);
      navigate(ROUTES.examResult);
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'status' in err && (err as { status: number }).status === 401) onUnauthorized();
      if (err && typeof err === 'object' && 'code' in err && (err as { code: string }).code === 'timeout') {
        alert(timeoutMessage);
        return;
      }
      alert('No se pudo finalizar el examen.');
    }
  }

  return (
    <ExamRunner
      questions={questions}
      totalTimeSeconds={attempt.totalTimeSeconds}
      initialSelections={selections}
      initialIndex={attempt.nextQuestionIndex}
      onSelect={handleSelect}
      onFinish={finishExam}
    />
  );
}
