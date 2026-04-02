import { useEffect, useMemo, useState } from 'react';
import { Navigate, useNavigate, useParams } from 'react-router-dom';
import ExamRunner, { Question } from '../components/ExamRunner';
import { getExamAttempt, saveExamAnswer, submitExam } from '../api';
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

export default function ExamAttemptPage({ token, onUnauthorized, onFinish }:{
  token: string;
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
    getExamAttempt<AttemptResponse>(token, attemptId)
      .then(data => setAttempt(data))
      .catch(err => {
        if (err?.status === 401) onUnauthorized();
        if (err?.code === 'timeout') {
          setError(timeoutMessage);
          return;
        }
        setError('No se pudo cargar el intento');
      })
      .finally(() => setLoading(false));
  }, [attemptId, token]);

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
      await saveExamAnswer(token, attemptId!, questionId, answerId);
    } catch (err: any) {
      if (err?.status === 401) onUnauthorized();
      if (err?.code === 'timeout') {
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
      const result = await submitExam(token, attemptId!, filtered);
      sessionStorage.removeItem('akdmia.activeAttemptId');
      onFinish(result);
      navigate(ROUTES.examResult);
    } catch (err: any) {
      if (err?.status === 401) onUnauthorized();
      if (err?.code === 'timeout') {
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
