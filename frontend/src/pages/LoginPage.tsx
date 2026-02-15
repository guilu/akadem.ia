import { Navigate } from 'react-router-dom';
import Login from '../components/Login';

export default function LoginPage({ isAuthed, onToken }: { isAuthed: boolean; onToken: (t: string) => void }) {
  if (isAuthed) return <Navigate to="/subjects" replace />;
  return (
    <div className="mt-8">
      <Login onToken={onToken} />
    </div>
  );
}
