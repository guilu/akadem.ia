import Settings from '../components/Settings';

export default function SettingsPage({ isAdmin, token }: { isAdmin: boolean; token: string }) {
  return <Settings isAdmin={isAdmin} token={token} />;
}
