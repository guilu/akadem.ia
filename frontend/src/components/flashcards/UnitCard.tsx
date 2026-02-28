import { BookOpen } from 'flowbite-react-icons/outline';
import { useNavigate } from 'react-router-dom';
import type { UnitSummary } from '../../pages/FlashcardsPage';

export default function UnitCard({ unit }: { unit: UnitSummary }) {
  const navigate = useNavigate();
  const reviewCount = unit.reviewCount ?? 0;
  const newCount = unit.newCount ?? 0;
  const dueCount = unit.dueCount ?? 0;

  const pending = (unit.dueCount ?? reviewCount) + newCount;
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
