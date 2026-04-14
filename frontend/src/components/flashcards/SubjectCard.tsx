import { BookOpen, CirclePlus, Clock, Refresh, ChevronRight } from 'flowbite-react-icons/outline';

type SubjectSummary = {
  subjectId: string;
  subjectName: string;
  newCount: number;
  reviewCount: number;
  dueCount: number;
  unitCount: number;
};

export type { SubjectSummary };

type Props = {
  subject: SubjectSummary;
  onClick: () => void;
  onExport?: (format: 'csv' | 'json') => void;
};

export default function SubjectCard({ subject, onClick, onExport }: Props) {
  const pending = subject.dueCount + subject.newCount;

  return (
    <div className="group relative">
      <button
        type="button"
        onClick={onClick}
        className="w-full text-left border border-secondary/25 rounded-2xl bg-card p-5 transition-all hover:border-primary/40 hover:shadow-lg hover:shadow-primary/5 hover:-translate-y-0.5"
      >
        <div className="flex items-center gap-4">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-accent/10 text-accent group-hover:bg-accent/15 transition-colors">
            <BookOpen className="w-5 h-5" />
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-start justify-between gap-2">
              <div className="font-bold text-sm leading-snug">{subject.subjectName}</div>
              <div className="text-right shrink-0">
                <div className="text-xl font-extrabold tabular-nums">{pending}</div>
                <div className="text-xs text-text/40 uppercase tracking-wide">pendientes</div>
              </div>
            </div>
          </div>
          <ChevronRight className="w-4 h-4 text-text/30 shrink-0" />
        </div>

        <div className="mt-3 flex flex-wrap gap-4 text-xs text-text/55">
          <span className="flex items-center gap-1">
            <BookOpen className="w-3.5 h-3.5" />
            {subject.unitCount} {subject.unitCount === 1 ? 'mazo' : 'mazos'}
          </span>
          {subject.newCount > 0 && (
            <span className="flex items-center gap-1 text-lime-500">
              <CirclePlus className="w-3.5 h-3.5" />
              {subject.newCount} nuevas
            </span>
          )}
          {subject.reviewCount > 0 && (
            <span className="flex items-center gap-1 text-yellow-500">
              <Clock className="w-3.5 h-3.5" />
              {subject.reviewCount} en repaso
            </span>
          )}
          {subject.dueCount > 0 && (
            <span className="flex items-center gap-1 text-primary">
              <Refresh className="w-3.5 h-3.5" />
              {subject.dueCount} pendientes
            </span>
          )}
        </div>
      </button>

      {onExport && (
        <div className="absolute bottom-3.5 right-10 flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity z-10">
          {(['csv', 'json'] as const).map((fmt) => (
            <button
              key={fmt}
              type="button"
              title={`Exportar ${fmt.toUpperCase()}`}
              onClick={(e) => { e.stopPropagation(); onExport(fmt); }}
              className="flex items-center gap-1 px-2 py-0.5 rounded-lg bg-secondary/20 hover:bg-secondary/40 text-text/50 hover:text-text text-xs font-medium transition-colors"
            >
              <svg className="w-3 h-3 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
              </svg>
              {fmt.toUpperCase()}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
