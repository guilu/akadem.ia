import { useEffect, useRef, useState } from 'react';
import { getSubjects, getUnitsForSubject, importFlashcards } from '../../api';

type Subject = { id: string; name: string; description?: string };
type Unit = { id: string; name: string };
type ImportResult = { imported: number; skipped: number; errors: string[] };

type Props = {
  token: string;
  onClose: () => void;
  onImported: () => void;
};

export default function FlashcardImportModal({ token, onClose, onImported }: Props) {
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [units, setUnits] = useState<Unit[]>([]);
  const [subjectId, setSubjectId] = useState('');
  const [unitId, setUnitId] = useState('');
  const [format, setFormat] = useState<'csv' | 'json'>('csv');
  const [loading, setLoading] = useState(false);
  const [loadingSubjects, setLoadingSubjects] = useState(true);
  const [loadingUnits, setLoadingUnits] = useState(false);
  const [result, setResult] = useState<ImportResult | null>(null);
  const [error, setError] = useState('');
  const fileRef = useRef<HTMLInputElement>(null);

  // Load all subjects on mount
  useEffect(() => {
    setLoadingSubjects(true);
    getSubjects(token)
      .then((data) => {
        setSubjects(data ?? []);
        if (data && data.length > 0) setSubjectId(data[0].id);
      })
      .catch(() => setError('No se pudieron cargar las materias.'))
      .finally(() => setLoadingSubjects(false));
  }, [token]);

  // Load units when subject changes
  useEffect(() => {
    if (!subjectId) { setUnits([]); setUnitId(''); return; }
    setLoadingUnits(true);
    setUnitId('');
    getUnitsForSubject(token, subjectId)
      .then((data) => {
        setUnits(data ?? []);
        if (data && data.length > 0) setUnitId(data[0].id);
      })
      .catch(() => setUnits([]))
      .finally(() => setLoadingUnits(false));
  }, [subjectId, token]);

  const handleSubmit = async () => {
    const file = fileRef.current?.files?.[0];
    if (!file) { setError('Selecciona un archivo para importar.'); return; }
    if (!unitId) { setError('Selecciona una unidad.'); return; }

    setLoading(true);
    setError('');
    setResult(null);

    try {
      const content = await file.text();
      const data = await importFlashcards(token, unitId, format, content);
      setResult(data);
      if (data.imported > 0) onImported();
    } catch (e: any) {
      setError(e.message ?? 'Error al importar.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div className="w-full max-w-md rounded-2xl bg-bg border border-secondary/25 p-6 space-y-5 shadow-xl">
        <div className="flex items-center justify-between">
          <h2 className="font-bold text-lg">Importar flashcards</h2>
          <button type="button" onClick={onClose} className="text-text/40 hover:text-text transition-colors text-xl leading-none">✕</button>
        </div>

        {/* Subject selector */}
        <div className="space-y-1">
          <label className="text-sm font-medium text-text/70">Materia</label>
          <select
            value={subjectId}
            onChange={(e) => setSubjectId(e.target.value)}
            disabled={loadingSubjects}
            className="w-full rounded-xl border border-secondary/30 bg-bg px-3 py-2 text-sm focus:outline-none focus:border-primary/50 disabled:opacity-50"
          >
            {loadingSubjects ? (
              <option>Cargando…</option>
            ) : subjects.map((s) => (
              <option key={s.id} value={s.id}>{s.name}</option>
            ))}
          </select>
        </div>

        {/* Unit selector */}
        <div className="space-y-1">
          <label className="text-sm font-medium text-text/70">Mazo (unidad destino)</label>
          <select
            value={unitId}
            onChange={(e) => setUnitId(e.target.value)}
            disabled={loadingUnits || units.length === 0}
            className="w-full rounded-xl border border-secondary/30 bg-bg px-3 py-2 text-sm focus:outline-none focus:border-primary/50 disabled:opacity-50"
          >
            {loadingUnits ? (
              <option>Cargando…</option>
            ) : units.length === 0 ? (
              <option value="">Sin unidades</option>
            ) : units.map((u) => (
              <option key={u.id} value={u.id}>{u.name}</option>
            ))}
          </select>
        </div>

        {/* Format selector */}
        <div className="space-y-1">
          <label className="text-sm font-medium text-text/70">Formato</label>
          <div className="flex gap-3">
            {(['csv', 'json'] as const).map((f) => (
              <button
                key={f}
                type="button"
                onClick={() => setFormat(f)}
                className={`flex-1 py-2 rounded-xl border text-sm font-medium transition-colors ${
                  format === f
                    ? 'border-primary bg-primary/10 text-primary'
                    : 'border-secondary/30 text-text/60 hover:border-secondary/60'
                }`}
              >
                {f.toUpperCase()}
              </button>
            ))}
          </div>
          <p className="text-xs text-text/40 pt-1">
            {format === 'csv'
              ? 'Columnas: front,back — primera fila puede ser cabecera.'
              : 'Array JSON: [{"front":"...","back":"..."}]'}
          </p>
        </div>

        {/* File input */}
        <div className="space-y-1">
          <label className="text-sm font-medium text-text/70">Archivo</label>
          <input
            ref={fileRef}
            type="file"
            accept={format === 'csv' ? '.csv,text/csv,text/plain' : '.json,application/json'}
            className="w-full text-sm text-text/70 file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:bg-secondary/20 file:text-sm file:font-medium file:text-text/70 hover:file:bg-secondary/30 file:transition-colors cursor-pointer"
          />
        </div>

        {error && <p className="text-sm text-red-400">{error}</p>}

        {result && (
          <div className={`rounded-xl px-4 py-3 text-sm space-y-1 ${
            result.imported > 0
              ? 'bg-lime-500/10 border border-lime-500/25 text-lime-400'
              : 'bg-secondary/10 border border-secondary/25 text-text/60'
          }`}>
            <p className="font-medium">{result.imported} importadas · {result.skipped} omitidas</p>
            {result.errors.length > 0 && (
              <ul className="text-xs text-red-400 space-y-0.5 mt-1">
                {result.errors.map((e, i) => <li key={i}>• {e}</li>)}
              </ul>
            )}
          </div>
        )}

        <div className="flex gap-3 pt-1">
          {result ? (
            <button type="button" onClick={onClose} className="flex-1 py-2 rounded-xl bg-primary text-white text-sm font-medium hover:bg-primary/90 transition-colors">
              Cerrar
            </button>
          ) : (
            <>
              <button type="button" onClick={onClose} className="flex-1 py-2 rounded-xl border border-secondary/30 text-sm text-text/60 hover:border-secondary/60 transition-colors">
                Cancelar
              </button>
              <button
                type="button"
                onClick={handleSubmit}
                disabled={loading || !unitId}
                className="flex-1 py-2 rounded-xl bg-primary text-white text-sm font-medium hover:bg-primary/90 disabled:opacity-50 transition-colors"
              >
                {loading ? 'Importando…' : 'Importar'}
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
