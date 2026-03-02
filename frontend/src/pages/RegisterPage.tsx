import { Navigate } from 'react-router-dom';
import Register from '../components/Register';
import { ROUTES } from '../constants/routes';

export default function RegisterPage({ isAuthed, onToken }: { isAuthed: boolean; onToken: (t: string) => void }) {
  if (isAuthed) return <Navigate to={ROUTES.subjects} replace />;
  return <Register onToken={onToken} />;
}
