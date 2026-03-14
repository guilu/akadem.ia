import { Navigate } from 'react-router-dom';
import Settings from '../components/Settings';
import { ROUTES } from '../constants/routes';

export default function SettingsPage({ isAdmin, token, onSubjectsChanged }: { isAdmin: boolean; token: string; onSubjectsChanged?: () => void }) {
  if (!isAdmin) return <Navigate to={ROUTES.home} replace />;
  return <Settings token={token} onSubjectsChanged={onSubjectsChanged} />;
}
