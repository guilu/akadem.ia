import { Navigate } from 'react-router-dom';
import ExamRunner, { Question } from '../components/ExamRunner';
import { ROUTES } from '../constants/routes';

export default function ExamRunnerPage({ questions, minutes, onFinish }: { questions: Question[]; minutes: number; onFinish: (payload: { selections: Record<string, string | undefined> }) => void }) {
  if (!questions.length) return <Navigate to={ROUTES.subjects} replace />;
  return <ExamRunner questions={questions} totalTimeSeconds={minutes * 60} onFinish={onFinish} />;
}
