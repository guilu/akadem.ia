import { Navigate } from 'react-router-dom';

export default function ProtectedRoute({ allow, children }: { allow: boolean; children: React.ReactElement }) {
  if (!allow) return <Navigate to="/login" replace />;
  return children;
}
