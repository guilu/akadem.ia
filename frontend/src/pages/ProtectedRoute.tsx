import { Navigate } from 'react-router-dom';
import { ROUTES } from '../constants/routes';

export default function ProtectedRoute({ allow, children }: { allow: boolean; children: React.ReactElement }) {
  if (!allow) return <Navigate to={ROUTES.login} replace />;
  return children;
}
