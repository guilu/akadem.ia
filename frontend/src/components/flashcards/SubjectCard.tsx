type SubjectSummary = {
  subjectId: string;
  subjectName: string;
  newCount: number;
  reviewCount: number;
  dueCount: number;
  unitCount: number;
};

export type { SubjectSummary };

export default function SubjectCard({ subject, onClick }: { subject: SubjectSummary; onClick: () => void }) {
  const pending = subject.dueCount + subject.newCount;

  return (
    <button
      type="button"
      onClick={onClick}
      className="group w-full text-left border border-secondary/25 rounded-2xl p-5 transition-all hover:border-primary/40 hover:shadow-lg hover:shadow-primary/5 hover:-translate-y-0.5"
    >
      <div className="flex items-center gap-4">
        <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-accent/10 text-accent group-hover:bg-accent/15 transition-colors text-xl">
          📚
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
        <div className="text-text/30 text-lg">›</div>
      </div>

      <div className="mt-3 flex flex-wrap gap-4 text-xs text-text/55">
        <span>📖 {subject.unitCount} {subject.unitCount === 1 ? 'mazo' : 'mazos'}</span>
        {subject.newCount > 0 && <span>🆕 {subject.newCount} nuevas</span>}
        {subject.reviewCount > 0 && <span>🟡 {subject.reviewCount} en repaso</span>}
        {subject.dueCount > 0 && <span>🔁 {subject.dueCount} pendientes</span>}
      </div>
    </button>
  );
}
