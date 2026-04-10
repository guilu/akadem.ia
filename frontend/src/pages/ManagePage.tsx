import Management from '../components/Management';

export default function ManagePage({ isAdmin, token, onSubjectsChanged }: { isAdmin: boolean; token: string; onSubjectsChanged?: () => void }) {
  return <Management isAdmin={isAdmin} token={token} onSubjectsChanged={onSubjectsChanged} />;
}
