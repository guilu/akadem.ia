import { Navigate } from 'react-router-dom';
import Settings from '../components/Settings';
import { ROUTES } from '../constants/routes';

export default function SettingsPage({ isAdmin, token }: { isAdmin: boolean; token: string }) {
  if (!isAdmin) return <Navigate to={ROUTES.home} replace />;
  return <Settings token={token} />;
}
