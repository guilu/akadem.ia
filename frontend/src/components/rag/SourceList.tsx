import { useState } from 'react';
import { Inbox } from 'flowbite-react-icons/outline';
import type { SourceDocument } from '../../types';

interface Props {
  sources: SourceDocument[];
  selectedId: string | null;
  onSelect: (id: string) => void;
  loading: boolean;
  onDelete?: (id: string) => Promise<void>;
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

export default function SourceList({ sources, selectedId, onSelect, loading, onDelete }: Props) {
  const [confirmId, setConfirmId] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

  const docToDelete = sources.find((s) => s.id === confirmId);

  async function handleConfirmDelete() {
    if (!confirmId || !onDelete) return;
    setDeleting(true);
    try {
      await onDelete(confirmId);
    } finally {
      setDeleting(false);
      setConfirmId(null);
    }
  }

  if (loading) {
    return <p className="text-sm text-text/50 py-4 text-center">Cargando documentos…</p>;
  }

  if (sources.length === 0) {
    return (
      <div className="text-center py-6 space-y-2">
        <Inbox className="w-8 h-8 mx-auto text-text/25" />
        <p className="text-sm text-text/50">No hay documentos subidos todavía.</p>
      </div>
    );
  }

  return (
    <>
      <ul className="divide-y divide-secondary/15 rounded-xl border border-secondary/20 overflow-hidden">
        {sources.map((src) => (
          <li key={src.id}>
            <div
              className={`w-full px-4 py-3 flex items-center justify-between gap-4 transition-colors ${
                selectedId === src.id ? 'bg-primary/10' : 'hover:bg-secondary/5'
              }`}
            >
              <button
                onClick={() => onSelect(src.id)}
                className="flex items-center gap-3 min-w-0 flex-1 text-left"
              >
                <svg className="w-5 h-5 text-text/40 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                <span className="text-sm font-medium truncate">{src.name}</span>
              </button>
              <div className="flex items-center gap-2 shrink-0">
                <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${STATUS_COLOR[src.status]}`}>
                  {STATUS_LABEL[src.status]}
                </span>
                {onDelete && (
                  <button
                    onClick={() => setConfirmId(src.id)}
                    title="Eliminar documento"
                    className="p-1.5 rounded-lg text-text/30 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-500/10 transition-colors"
                  >
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                  </button>
                )}
              </div>
            </div>
          </li>
        ))}
      </ul>

      {/* Confirmation modal */}
      {confirmId && docToDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
          <div className="bg-bg rounded-2xl border border-secondary/20 shadow-xl p-6 max-w-sm w-full space-y-4">
            <div className="flex items-start gap-3">
              <div className="w-10 h-10 rounded-full bg-red-100 dark:bg-red-500/15 flex items-center justify-center shrink-0">
                <svg className="w-5 h-5 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" />
                </svg>
              </div>
              <div>
                <h3 className="font-semibold text-text">¿Eliminar documento?</h3>
                <p className="text-sm text-text/60 mt-1">
                  Se eliminará <span className="font-medium text-text">"{docToDelete.name}"</span> junto con todos sus títulos y preguntas asociadas. Esta acción no se puede deshacer.
                </p>
              </div>
            </div>
            <div className="flex gap-3 justify-end">
              <button
                onClick={() => setConfirmId(null)}
                disabled={deleting}
                className="px-4 py-2 text-sm rounded-xl border border-secondary/25 text-text/70 hover:bg-secondary/10 transition-colors disabled:opacity-40"
              >
                Cancelar
              </button>
              <button
                onClick={handleConfirmDelete}
                disabled={deleting}
                className="px-4 py-2 text-sm rounded-xl bg-red-500 text-white font-medium hover:bg-red-600 transition-colors disabled:opacity-40"
              >
                {deleting ? 'Eliminando…' : 'Eliminar'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
