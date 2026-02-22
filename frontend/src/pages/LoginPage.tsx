import { Navigate } from 'react-router-dom';
import Login from '../components/Login';
import { ROUTES } from '../constants/routes';

export default function LoginPage({ isAuthed, onToken }: { isAuthed: boolean; onToken: (t: string) => void }) {
  if (isAuthed) return <Navigate to={ROUTES.subjects} replace />;
  return (
    <div className="mt-8">
      <Login onToken={onToken} />
    </div>
  );
}
