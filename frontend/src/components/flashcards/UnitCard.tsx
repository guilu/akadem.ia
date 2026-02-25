import { BookOpen } from 'flowbite-react-icons/outline';
import { useNavigate } from 'react-router-dom';
import type { UnitSummary } from '../../pages/FlashcardsPage';

export default function UnitCard({ unit }: { unit: UnitSummary }) {
  const navigate = useNavigate();
  const due = unit.dueCards ?? 0;
  const created = unit.newCards ?? 0;
  const total = unit.totalCards ?? 0;
  const review = Math.max(total - due - created, 0);
  const learning = 0;

  const totalForBar = due + learning + review + created;
  const segment = (count: number) =>
    totalForBar === 0 ? '0%' : `${Math.max((count / totalForBar) * 100, 4)}%`;

  const pending = due + learning + created;
  const cta = pending > 0 ? `Estudiar ${pending} tarjetas` : 'Sin pendientes';

  return (
    <button
      type="button"
      onClick={() => navigate(`/flashcards/study?unitId=${unit.unitId}`)}
      className="w-full text-left rounded-2xl border border-gray-200 bg-white/90 p-4 shadow-sm transition hover:shadow-md hover:scale-[1.01] dark:border-slate-700 dark:bg-slate-800"
    >
      <div className="flex items-center gap-3">
        <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-white/80 text-secondary">
          <BookOpen className="w-6 h-6" />
        </div>
        <div className="flex-1">
          <div className="flex items-center justify-between gap-2">
            <div className="font-semibold text-slate-900 dark:text-white">{unit.unitName}</div>
            <div className="text-sm font-semibold text-slate-500">{cta}</div>
          </div>
          <div className="mt-2 h-2 w-full overflow-hidden rounded-full bg-white/80">
            <div className="flex h-full">
              <div className="bg-red-400" style={{ width: segment(due) }} />
              <div className="bg-yellow-300" style={{ width: segment(learning) }} />
              <div className="bg-green-400" style={{ width: segment(review) }} />
              <div className="bg-slate-300" style={{ width: segment(created) }} />
            </div>
          </div>
        </div>
        <div className="text-slate-400">›</div>
      </div>

      <div className="mt-3 flex flex-wrap gap-4 text-sm text-slate-500">
        <span>🔴 {due} falladas</span>
        <span>🟡 {learning} en repaso</span>
        <span>🟢 {review} dominadas</span>
        <span>🆕 {created} nuevas</span>
      </div>
    </button>
  );
}
