import Management from '../components/Management';

export default function ManagePage({ isAdmin, onSubjectsChanged }: { isAdmin: boolean; onSubjectsChanged?: () => void }) {
  return <Management isAdmin={isAdmin} onSubjectsChanged={onSubjectsChanged} />;
}
