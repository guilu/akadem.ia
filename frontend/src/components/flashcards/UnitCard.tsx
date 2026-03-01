import { BookOpen } from 'flowbite-react-icons/outline';
import { useNavigate } from 'react-router-dom';
import type { UnitSummary } from '../../pages/FlashcardsPage';

export default function UnitCard({ unit }: { unit: UnitSummary }) {
  const navigate = useNavigate();
  const reviewCount = unit.reviewCount ?? 0;
  const newCount = unit.newCount ?? 0;
  const dueCount = unit.dueCount ?? 0;

  const pending = (unit.dueCount ?? reviewCount) + newCount;

  return (
    <button
      type="button"
      onClick={() => navigate(`/flashcards/study?unitId=${unit.unitId}`)}
      className="w-full text-left rounded-2xl border border-gray-200 bg-white/90 p-4 shadow-sm transition hover:shadow-md hover:scale-[1.01] dark:border-slate-700 dark:bg-slate-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/60 focus-visible:ring-offset-2"
    >
      <div className="flex items-center gap-3">
        <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-white/80 text-secondary dark:bg-slate-700/70">
          <BookOpen className="w-6 h-6" />
        </div>
        <div className="flex-1">
          <div className="flex items-start justify-between gap-2">
            <div className="font-semibold text-slate-900 dark:text-white">{unit.unitName}</div>
            <div className="flex flex-col items-end text-slate-500 dark:text-slate-400">
              <div className="text-lg font-semibold text-slate-900 dark:text-slate-200">{pending}</div>
              <div className="text-xs uppercase tracking-wide">cards</div>
            </div>
          </div>
          {/* metrics bar removed */}
        </div>
        <div className="text-slate-400">›</div>
      </div>

      <div className="mt-3 flex flex-wrap gap-4 text-sm text-slate-500">
        <span>
          🟡 {reviewCount} en repaso{dueCount > 0 ? ` (${dueCount} pendientes)` : ''}
        </span>
        <span>🆕 {newCount} nuevas</span>
      </div>
    </button>
  );
}
