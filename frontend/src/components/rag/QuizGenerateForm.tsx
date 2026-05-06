import { useEffect, useState } from 'react';
import { Inbox } from 'flowbite-react-icons/outline';
import { getUnitsForSubject } from '../../api';
import type { SourceDocument, GenerateQuizCommand } from '../../types';

interface Props {
  sources: SourceDocument[];
  subjectId: string;
  onGenerate: (cmd: GenerateQuizCommand) => void;
  loading: boolean;
}

const DIFFICULTIES = [
  { value: 'EASY', label: 'Fácil' },
  { value: 'MEDIUM', label: 'Media' },
  { value: 'HARD', label: 'Difícil' }
] as const;

export default function QuizGenerateForm({ sources, subjectId, onGenerate, loading }: Props) {
  const processed = sources.filter((s) => s.status === 'PROCESSED');

  const [sourceId, setSourceId] = useState('');
  const [unitId, setUnitId] = useState('');
  const [difficulty, setDifficulty] = useState<'EASY' | 'MEDIUM' | 'HARD'>('MEDIUM');
  const [questionCount, setQuestionCount] = useState(5);
  const [includeHints, setIncludeHints] = useState(false);
  const [storeAsDraft, setStoreAsDraft] = useState(true);
  const [units, setUnits] = useState<{ id: string; name: string }[]>([]);
  const [unitsLoading, setUnitsLoading] = useState(false);
  const [error, setError] = useState('');

  // Load units for the selected subject
  useEffect(() => {
    if (!subjectId) { setUnits([]); setUnitId(''); return; }
    setUnitsLoading(true);
    getUnitsForSubject(subjectId)
      .then((u) => { setUnits(u); setUnitId(''); })
      .catch(() => setUnits([]))
      .finally(() => setUnitsLoading(false));
  }, [subjectId]);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!sourceId) { setError('Selecciona un documento fuente.'); return; }
    if (!unitId) { setError('Selecciona una unidad temática.'); return; }
    if (questionCount < 1 || questionCount > 20) { setError('El número de preguntas debe estar entre 1 y 20.'); return; }
    setError('');
    onGenerate({ sourceId, unitId, difficulty, questionCount, includeHints, storeAsDraft });
  }

  const labelCls = 'block text-sm font-medium text-text/70 mb-1';
  const inputCls = 'w-full rounded-xl border border-secondary/25 bg-secondary/5 px-3 py-2 text-sm focus:outline-none focus:border-primary/60 focus:ring-1 focus:ring-primary/30';

  return (
    <form onSubmit={handleSubmit} noValidate className="space-y-5">
      <div>
        <label htmlFor="rag-source" className={labelCls}>Documento fuente *</label>
        <select id="rag-source" value={sourceId} onChange={(e) => setSourceId(e.target.value)} className={inputCls} required>
          <option value="">— Selecciona un documento —</option>
          {processed.map((s) => (
            <option key={s.id} value={s.id}>{s.name}</option>
          ))}
        </select>
        {processed.length === 0 && (
          <p className="text-xs text-yellow-600 mt-1 flex items-center gap-1">
            <Inbox className="w-3.5 h-3.5 shrink-0" />
            No hay documentos procesados. Súbelos en la pestaña Fuentes.
          </p>
        )}
      </div>

      <div>
        <label htmlFor="rag-unit" className={labelCls}>Unidad temática *</label>
        <select
          id="rag-unit"
          value={unitId}
          onChange={(e) => setUnitId(e.target.value)}
          className={inputCls}
          disabled={unitsLoading || units.length === 0}
          required
        >
          <option value="">— Selecciona una unidad —</option>
          {units.map((u) => (
            <option key={u.id} value={u.id}>{u.name}</option>
          ))}
        </select>
        {units.length === 0 && !unitsLoading && (
          <p className="text-xs text-yellow-600 mt-1 flex items-center gap-1">
            <Inbox className="w-3.5 h-3.5 shrink-0" />
            No hay unidades en esta asignatura. Indexa un documento primero.
          </p>
        )}
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label htmlFor="rag-difficulty" className={labelCls}>Dificultad</label>
          <select id="rag-difficulty" value={difficulty} onChange={(e) => setDifficulty(e.target.value as typeof difficulty)} className={inputCls}>
            {DIFFICULTIES.map((d) => (
              <option key={d.value} value={d.value}>{d.label}</option>
            ))}
          </select>
        </div>
        <div>
          <label className={labelCls}>Nº preguntas (1–20)</label>
          <input
            type="number"
            min={1}
            max={20}
            value={questionCount}
            onChange={(e) => setQuestionCount(Number(e.target.value))}
            className={inputCls}
          />
        </div>
      </div>

      <div className="flex flex-col gap-2">
        <label className="flex items-center gap-2 cursor-pointer text-sm text-text/70">
          <input
            type="checkbox"
            checked={includeHints}
            onChange={(e) => setIncludeHints(e.target.checked)}
            className="rounded border-secondary/30"
          />
          Incluir pistas (hints)
        </label>
        <label className="flex items-center gap-2 cursor-pointer text-sm text-text/70">
          <input
            type="checkbox"
            checked={storeAsDraft}
            onChange={(e) => setStoreAsDraft(e.target.checked)}
            className="rounded border-secondary/30"
          />
          Guardar como borradores
        </label>
      </div>

      {error && <p role="alert" className="text-sm text-red-500">{error}</p>}

      <button
        type="submit"
        disabled={loading || processed.length === 0}
        className="w-full py-2.5 rounded-xl bg-primary text-white font-semibold text-sm hover:opacity-90 transition-opacity disabled:opacity-40 disabled:cursor-not-allowed"
      >
        {loading ? 'Generando preguntas…' : 'Generar preguntas'}
      </button>
    </form>
  );
}
