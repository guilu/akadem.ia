import { Navigate } from 'react-router-dom';
import Register from '../components/Register';
import { ROUTES } from '../constants/routes';

export default function RegisterPage({ isAuthed, onAuthSuccess }: { isAuthed: boolean; onAuthSuccess: (user: { email: string; role: string }) => void }) {
  if (isAuthed) return <Navigate to={ROUTES.subjects} replace />;
  return <Register onAuthSuccess={onAuthSuccess} />;
}
