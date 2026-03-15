import { BookOpen } from 'flowbite-react-icons/outline';
import { useNavigate } from 'react-router-dom';
import type { UnitSummary } from '../../pages/FlashcardsPage';

export default function UnitCard({ unit, onClick }: { unit: UnitSummary; onClick?: () => void }) {
  const navigate = useNavigate();
  const reviewCount = unit.reviewCount ?? 0;
  const newCount = unit.newCount ?? 0;
  const dueCount = unit.dueCount ?? 0;
  const pending = (unit.dueCount ?? reviewCount) + newCount;

  return (
    <button
      type="button"
      onClick={() => onClick ? onClick() : navigate(`/flashcards/study?unitId=${unit.unitId}`)}
      className="group w-full text-left border border-secondary/25 rounded-2xl p-5 transition-all hover:border-primary/40 hover:shadow-lg hover:shadow-primary/5 hover:-translate-y-0.5"
    >
      <div className="flex items-center gap-4">
        <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary group-hover:bg-primary/15 transition-colors">
          <BookOpen className="w-5 h-5" />
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-start justify-between gap-2">
            <div className="font-bold text-sm leading-snug">{unit.unitName}</div>
            <div className="text-right shrink-0">
              <div className="text-xl font-extrabold tabular-nums">{pending}</div>
              <div className="text-xs text-text/40 uppercase tracking-wide">cards</div>
            </div>
          </div>
        </div>
        <div className="text-text/30 text-lg">›</div>
      </div>

      <div className="mt-3 flex flex-wrap gap-4 text-xs text-text/55">
        <span>🟡 {reviewCount} en repaso{dueCount > 0 ? ` (${dueCount} pendientes)` : ''}</span>
        <span>🆕 {newCount} nuevas</span>
      </div>
    </button>
  );
}
