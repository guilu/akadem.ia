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
      <div className="max-w-xl mx-auto border border-secondary/25 rounded-2xl p-8">
        <h2 className="text-xl font-bold mb-2">Error</h2>
        <p className="text-text/55">{error || 'No se pudo cargar el intento.'}</p>
      </div>
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
