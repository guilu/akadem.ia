import { FolderOpen, CirclePlus, Clock, Refresh, ChevronRight } from 'flowbite-react-icons/outline';

type SyllabusSummary = {
  syllabusId: string | null;
  syllabusName: string;
  newCount: number;
  reviewCount: number;
  dueCount: number;
  subjectCount: number;
};

export type { SyllabusSummary };

type Props = {
  syllabus: SyllabusSummary;
  onClick: () => void;
};

export default function SyllabusCard({ syllabus, onClick }: Props) {
  const pending = syllabus.dueCount + syllabus.newCount;
  const isNoSyllabus = syllabus.syllabusId === null;

  return (
    <div className="group relative">
      <button
        type="button"
        onClick={onClick}
        className="w-full text-left border border-secondary/25 rounded-2xl p-5 transition-all hover:border-primary/40 hover:shadow-lg hover:shadow-primary/5 hover:-translate-y-0.5"
      >
        <div className="flex items-center gap-4">
          <div className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-accent/10 transition-colors ${isNoSyllabus ? 'text-text/50 group-hover:bg-accent/5' : 'text-accent group-hover:bg-accent/15'}`}>
            <FolderOpen className="w-5 h-5" />
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-start justify-between gap-2">
              <div className={`font-bold text-sm leading-snug ${isNoSyllabus ? 'italic text-text/60' : ''}`}>
                {syllabus.syllabusName}
              </div>
              <div className="text-right shrink-0">
                <div className="text-xl font-extrabold tabular-nums text-text/80">{pending}</div>
                <div className="text-xs text-text/40 uppercase tracking-wide">pendientes</div>
              </div>
            </div>
          </div>
          <ChevronRight className="w-4 h-4 text-text/30 shrink-0" />
        </div>

        <div className="mt-3 flex flex-wrap gap-4 text-xs text-text/55">
          <span className="flex items-center gap-1">
            <FolderOpen className="w-3.5 h-3.5" />
            {syllabus.subjectCount} {syllabus.subjectCount === 1 ? 'materia' : 'materias'}
          </span>
          {syllabus.newCount > 0 && (
            <span className="flex items-center gap-1 text-lime-500">
              <CirclePlus className="w-3.5 h-3.5" />
              {syllabus.newCount} nuevas
            </span>
          )}
          {syllabus.reviewCount > 0 && (
            <span className="flex items-center gap-1 text-yellow-500">
              <Clock className="w-3.5 h-3.5" />
              {syllabus.reviewCount} en repaso
            </span>
          )}
          {syllabus.dueCount > 0 && (
            <span className="flex items-center gap-1 text-primary">
              <Refresh className="w-3.5 h-3.5" />
              {syllabus.dueCount} pendientes
            </span>
          )}
        </div>
      </button>
    </div>
  );
}
