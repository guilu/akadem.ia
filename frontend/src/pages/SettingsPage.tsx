import { Navigate } from 'react-router-dom';
import Settings from '../components/Settings';

export default function SettingsPage({ isAdmin, token }: { isAdmin: boolean; token: string }) {
  if (!isAdmin) return <Navigate to="/" replace />;
  return <Settings token={token} />;
}
