import { Navigate, useParams } from 'react-router-dom';
import ExamBuilder from '../components/ExamBuilder';
import { ROUTES } from '../constants/routes';

export default function ExamBuilderPage({ onStart, onUnauthorized }: { onStart: (cfg: { unitCounts: Record<string, number>; minutes: number }) => void; onUnauthorized: () => void }) {
  const { subjectId } = useParams();
  if (!subjectId) return <Navigate to={ROUTES.subjects} replace />;
  return <ExamBuilder subjectId={subjectId} onStart={onStart} onUnauthorized={onUnauthorized} />;
}
