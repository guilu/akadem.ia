import { useEffect, useState } from 'react';
import { getDrafts } from '../../api';
import type { SourceDocument, GeneratedDraft } from '../../types';

interface Props {
  token: string;
  sources: SourceDocument[];
}

const DIFFICULTY_LABEL: Record<string, string> = {
  EASY: 'Fácil', MEDIUM: 'Media', HARD: 'Difícil'
};

export default function DraftList({ token, sources }: Props) {
  const processed = sources.filter((s) => s.status === 'PROCESSED');
  const [selectedSourceId, setSelectedSourceId] = useState('');
  const [drafts, setDrafts] = useState<GeneratedDraft[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!selectedSourceId) { setDrafts([]); return; }
    setLoading(true);
    setError('');
    getDrafts(token, selectedSourceId)
      .then(setDrafts)
      .catch(() => { setError('Error al cargar los borradores.'); setDrafts([]); })
      .finally(() => setLoading(false));
  }, [selectedSourceId, token]);

  const labelCls = 'block text-sm font-medium text-text/70 mb-1';
  const inputCls = 'w-full rounded-xl border border-secondary/25 bg-secondary/5 px-3 py-2 text-sm focus:outline-none focus:border-primary/60 focus:ring-1 focus:ring-primary/30';

  return (
    <div className="space-y-5">
      <div>
        <label className={labelCls}>Filtrar por documento</label>
        <select value={selectedSourceId} onChange={(e) => setSelectedSourceId(e.target.value)} className={inputCls}>
          <option value="">— Selecciona un documento —</option>
          {processed.map((s) => (
            <option key={s.id} value={s.id}>{s.fileName}</option>
          ))}
        </select>
      </div>

      {loading && <p className="text-sm text-text/50 text-center py-6">Cargando borradores…</p>}
      {error && <p role="alert" className="text-sm text-red-500">{error}</p>}

      {!loading && !error && selectedSourceId && drafts.length === 0 && (
        <p className="text-sm text-text/50 text-center py-6">No hay borradores para este documento.</p>
      )}

      {!loading && drafts.length > 0 && (
        <div className="space-y-3">
          <p className="text-sm text-text/50">{drafts.length} borrador{drafts.length !== 1 ? 'es' : ''}</p>
          {drafts.map((draft, i) => (
            <div key={draft.id} className="rounded-xl border border-secondary/20 bg-secondary/5 p-4 space-y-2">
              <div className="flex items-start justify-between gap-3">
                <p className="text-sm font-semibold leading-snug">
                  <span className="text-primary font-bold mr-1">{i + 1}.</span>
                  {draft.statement}
                </p>
                <div className="flex flex-col items-end gap-1 shrink-0">
                  <span className="text-xs px-2 py-0.5 rounded-full bg-primary/10 text-primary font-medium">
                    {DIFFICULTY_LABEL[draft.difficulty] ?? draft.difficulty}
                  </span>
                  <span className="text-xs text-text/40">{draft.topic}</span>
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
              {draft.reference && (
                <p className="text-xs text-primary/70 font-medium pt-1">{draft.reference}</p>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
