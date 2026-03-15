import { Navigate, useParams } from 'react-router-dom';
import ExamBuilder from '../components/ExamBuilder';
import { ROUTES } from '../constants/routes';

export default function ExamBuilderPage({ onStart, onStartRandom, onUnauthorized }: {
  onStart: (cfg: { unitCounts: Record<string, number>; minutes: number }) => void;
  onStartRandom: (cfg: { subjectId: string; count: number; minutes: number; difficulty?: 'EASY' | 'MEDIUM' | 'HARD' }) => void;
  onUnauthorized: () => void;
}) {
  const { subjectId } = useParams();
  if (!subjectId) return <Navigate to={ROUTES.subjects} replace />;
  return <ExamBuilder subjectId={subjectId} onStart={onStart} onStartRandom={onStartRandom} onUnauthorized={onUnauthorized} />;
}
