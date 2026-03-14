import Settings from '../components/Settings';

export default function SettingsPage({ isAdmin, token, onSubjectsChanged }: { isAdmin: boolean; token: string; onSubjectsChanged?: () => void }) {
  return <Settings isAdmin={isAdmin} token={token} onSubjectsChanged={onSubjectsChanged} />;
}
