import Settings from '../components/Settings';

export default function SettingsPage({ isAdmin }: { isAdmin: boolean }) {
  return <Settings isAdmin={isAdmin} />;
}
