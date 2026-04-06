import { useEffect, useState } from 'react';
import { Inbox } from 'flowbite-react-icons/outline';
import { getDrafts, approveDraft, rejectDraft } from '../../api';
import type { SourceDocument, GeneratedDraft } from '../../types';

interface Props {
  token: string;
  sources: SourceDocument[];
}

type StatusFilter = 'GENERATED' | 'VALIDATED' | 'REJECTED' | '';

const DIFFICULTY_LABEL: Record<string, string> = {
  EASY: 'Fácil', MEDIUM: 'Media', HARD: 'Difícil'
};

const STATUS_LABEL: Record<string, string> = {
  GENERATED: 'Pendiente', VALIDATED: 'Aprobada', REJECTED: 'Rechazada'
};

const STATUS_COLOR: Record<string, string> = {
  GENERATED: 'bg-yellow-400/15 text-yellow-700 dark:text-yellow-300',
  VALIDATED: 'bg-green-400/15 text-green-700 dark:text-green-300',
  REJECTED: 'bg-red-400/15 text-red-600 dark:text-red-400'
};

export default function DraftList({ token, sources }: Props) {
  const processed = sources.filter((s) => s.status === 'PROCESSED');
  const [selectedSourceId, setSelectedSourceId] = useState('');
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('GENERATED');
  const [drafts, setDrafts] = useState<GeneratedDraft[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [actionLoading, setActionLoading] = useState<Record<string, boolean>>({});

  useEffect(() => {
    if (!selectedSourceId) { setDrafts([]); return; }
    setLoading(true);
    setError('');
    getDrafts(token, selectedSourceId, statusFilter || undefined)
      .then(setDrafts)
      .catch(() => { setError('Error al cargar los borradores.'); setDrafts([]); })
      .finally(() => setLoading(false));
  }, [selectedSourceId, statusFilter, token]);

  async function handleApprove(draftId: string) {
    setActionLoading((prev) => ({ ...prev, [draftId]: true }));
    try {
      const updated = await approveDraft(token, draftId);
      setDrafts((prev) => prev.map((d) => d.id === draftId ? updated : d));
    } catch (err: any) {
      alert(err?.body?.message || 'Error al aprobar la pregunta.');
    } finally {
      setActionLoading((prev) => ({ ...prev, [draftId]: false }));
    }
  }

  async function handleReject(draftId: string) {
    setActionLoading((prev) => ({ ...prev, [draftId]: true }));
    try {
      const updated = await rejectDraft(token, draftId);
      setDrafts((prev) => prev.map((d) => d.id === draftId ? updated : d));
    } catch (err: any) {
      alert(err?.body?.message || 'Error al rechazar la pregunta.');
    } finally {
      setActionLoading((prev) => ({ ...prev, [draftId]: false }));
    }
  }

  const labelCls = 'block text-sm font-medium text-text/70 mb-1';
  const inputCls = 'w-full rounded-xl border border-secondary/25 bg-secondary/5 px-3 py-2 text-sm focus:outline-none focus:border-primary/60 focus:ring-1 focus:ring-primary/30';

  return (
    <div className="space-y-5">
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className={labelCls}>Filtrar por documento</label>
          <select value={selectedSourceId} onChange={(e) => setSelectedSourceId(e.target.value)} className={inputCls}>
            <option value="">— Selecciona un documento —</option>
            {processed.map((s) => (
              <option key={s.id} value={s.id}>{s.name}</option>
            ))}
          </select>
        </div>
        <div>
          <label className={labelCls}>Estado</label>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as StatusFilter)}
            className={inputCls}
          >
            <option value="">Todos</option>
            <option value="GENERATED">Pendientes</option>
            <option value="VALIDATED">Aprobadas</option>
            <option value="REJECTED">Rechazadas</option>
          </select>
        </div>
      </div>

      {loading && <p className="text-sm text-text/50 text-center py-6">Cargando borradores…</p>}
      {error && <p role="alert" className="text-sm text-red-500">{error}</p>}

      {!loading && !error && selectedSourceId && drafts.length === 0 && (
        <div className="text-center py-6 space-y-2">
          <Inbox className="w-8 h-8 mx-auto text-text/25" />
          <p className="text-sm text-text/50">No hay borradores para este filtro.</p>
        </div>
      )}

      {!loading && drafts.length > 0 && (
        <div className="space-y-3">
          <p className="text-sm text-text/50">{drafts.length} borrador{drafts.length !== 1 ? 'es' : ''}</p>
          {drafts.map((draft, i) => (
            <div
              key={draft.id}
              className={`rounded-xl border p-4 space-y-2 transition-opacity ${
                draft.status === 'REJECTED' ? 'opacity-50 border-secondary/15' : 'border-secondary/20 bg-secondary/5'
              }`}
            >
              <div className="flex items-start justify-between gap-3">
                <p className="text-sm font-semibold leading-snug">
                  <span className="text-primary font-bold mr-1">{i + 1}.</span>
                  {draft.statement}
                </p>
                <div className="flex flex-col items-end gap-1 shrink-0">
                  <span className="text-xs px-2 py-0.5 rounded-full bg-primary/10 text-primary font-medium">
                    {DIFFICULTY_LABEL[draft.difficulty] ?? draft.difficulty}
                  </span>
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${STATUS_COLOR[draft.status] ?? ''}`}>
                    {STATUS_LABEL[draft.status] ?? draft.status}
                  </span>
                </div>
              </div>

              <ul className="space-y-1">
                {draft.answers.map((ans, ai) => (
                  <li key={ai} className={`text-xs px-3 py-1 rounded-lg flex items-center gap-2 ${
                    ai === draft.correctIndex
                      ? 'bg-green-400/15 text-green-700 dark:text-green-300 font-medium'
                      : 'text-text/60'
                  }`}>
                    <span className={`w-4 h-4 rounded-full flex items-center justify-center text-[10px] font-bold shrink-0 ${
                      ai === draft.correctIndex ? 'bg-green-500 text-white' : 'bg-secondary/20 text-text/40'
                    }`}>
                      {String.fromCharCode(65 + ai)}
                    </span>
                    {ans}
                  </li>
                ))}
              </ul>

              {draft.hint && (
                <p className="text-xs text-primary/70 italic">Pista: {draft.hint}</p>
              )}
              {draft.reference && (
                <p className="text-xs text-primary/70 font-medium">{draft.reference}</p>
              )}

              {/* Approve / Reject actions — only for GENERATED drafts */}
              {draft.status === 'GENERATED' && (
                <div className="flex gap-2 pt-1">
                  <button
                    type="button"
                    onClick={() => handleApprove(draft.id)}
                    disabled={actionLoading[draft.id]}
                    className="flex-1 py-1.5 rounded-lg bg-green-500/15 text-green-700 dark:text-green-300 text-xs font-semibold hover:bg-green-500/25 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
                  >
                    {actionLoading[draft.id] ? '…' : '✓ Aprobar'}
                  </button>
                  <button
                    type="button"
                    onClick={() => handleReject(draft.id)}
                    disabled={actionLoading[draft.id]}
                    className="flex-1 py-1.5 rounded-lg bg-red-500/10 text-red-600 dark:text-red-400 text-xs font-semibold hover:bg-red-500/20 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
                  >
                    {actionLoading[draft.id] ? '…' : '✗ Rechazar'}
                  </button>
                </div>
              )}

              {draft.status === 'VALIDATED' && (
                <p className="text-xs text-green-600 dark:text-green-400 pt-1">
                  ✓ Añadida al banco de preguntas
                </p>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
