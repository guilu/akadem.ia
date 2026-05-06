import { Navigate } from 'react-router-dom';
import Login from '../components/Login';
import { ROUTES } from '../constants/routes';

export default function LoginPage({ isAuthed, onAuthSuccess }: { isAuthed: boolean; onAuthSuccess: (user: { email: string; role: string }) => void }) {
  if (isAuthed) return <Navigate to={ROUTES.subjects} replace />;
  return <Login onAuthSuccess={onAuthSuccess} />;
}
