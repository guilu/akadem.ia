import type { SourceDocument } from '../../types';

interface Props {
  sources: SourceDocument[];
  selectedId: string | null;
  onSelect: (id: string) => void;
  loading: boolean;
}

const STATUS_LABEL: Record<SourceDocument['status'], string> = {
  UPLOADED: 'Subido',
  PENDING_REVIEW: 'Pendiente revisión',
  PROCESSED: 'Procesado',
  FAILED: 'Error'
};

const STATUS_COLOR: Record<SourceDocument['status'], string> = {
  UPLOADED: 'bg-yellow-400/15 text-yellow-600 dark:text-yellow-400',
  PENDING_REVIEW: 'bg-blue-400/15 text-blue-600 dark:text-blue-400',
  PROCESSED: 'bg-green-400/15 text-green-600 dark:text-green-400',
  FAILED: 'bg-red-400/15 text-red-600 dark:text-red-400'
};

export default function SourceList({ sources, selectedId, onSelect, loading }: Props) {
  if (loading) {
    return <p className="text-sm text-text/50 py-4 text-center">Cargando documentos…</p>;
  }

  if (sources.length === 0) {
    return <p className="text-sm text-text/50 py-4 text-center">No hay documentos subidos todavía.</p>;
  }

  return (
    <ul className="divide-y divide-secondary/15 rounded-xl border border-secondary/20 overflow-hidden">
      {sources.map((src) => (
        <li key={src.id}>
          <button
            onClick={() => onSelect(src.id)}
            className={`w-full text-left px-4 py-3 flex items-center justify-between gap-4 transition-colors ${
              selectedId === src.id
                ? 'bg-primary/10'
                : 'hover:bg-secondary/5'
            }`}
          >
            <span className="flex items-center gap-3 min-w-0">
              <svg className="w-5 h-5 text-text/40 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              <span className="text-sm font-medium truncate">{src.name}</span>
            </span>
            <span className={`text-xs px-2 py-0.5 rounded-full font-medium shrink-0 ${STATUS_COLOR[src.status]}`}>
              {STATUS_LABEL[src.status]}
            </span>
          </button>
        </li>
      ))}
    </ul>
  );
}
