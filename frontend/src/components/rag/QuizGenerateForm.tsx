import { useEffect, useState } from 'react';
import { getUnitsForSubject } from '../../api';
import type { Subject, SourceDocument, AdminUnit, GenerateQuizCommand } from '../../types';

interface Props {
  token: string;
  sources: SourceDocument[];
  subjects: Subject[];
  onGenerate: (cmd: GenerateQuizCommand) => void;
  loading: boolean;
}

const DIFFICULTIES = [
  { value: 'EASY', label: 'Fácil' },
  { value: 'MEDIUM', label: 'Media' },
  { value: 'HARD', label: 'Difícil' }
] as const;

export default function QuizGenerateForm({ token, sources, subjects, onGenerate, loading }: Props) {
  const processed = sources.filter((s) => s.status === 'PROCESSED');

  const [sourceId, setSourceId] = useState('');
  const [topic, setTopic] = useState('');
  const [difficulty, setDifficulty] = useState<'EASY' | 'MEDIUM' | 'HARD'>('MEDIUM');
  const [questionCount, setQuestionCount] = useState(5);
  const [includeHints, setIncludeHints] = useState(false);
  const [storeAsDraft, setStoreAsDraft] = useState(true);
  const [subjectId, setSubjectId] = useState('');
  const [unitId, setUnitId] = useState('');
  const [units, setUnits] = useState<AdminUnit[]>([]);
  const [unitsLoading, setUnitsLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!subjectId) { setUnits([]); setUnitId(''); return; }
    setUnitsLoading(true);
    getUnitsForSubject(token, subjectId)
      .then((u) => { setUnits(u); setUnitId(''); })
      .catch(() => setUnits([]))
      .finally(() => setUnitsLoading(false));
  }, [subjectId, token]);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!topic.trim()) { setError('Introduce el tema de las preguntas.'); return; }
    if (!sourceId) { setError('Selecciona un documento fuente.'); return; }
    if (questionCount < 1 || questionCount > 20) { setError('El número de preguntas debe estar entre 1 y 20.'); return; }
    setError('');
    onGenerate({
      sourceId,
      unitId: unitId || null,
      topic: topic.trim(),
      difficulty,
      questionCount,
      includeHints,
      storeAsDraft
    });
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
          <p className="text-xs text-yellow-600 mt-1">No hay documentos procesados. Súbelos en la pestaña Fuentes.</p>
        )}
      </div>

      <div>
        <label htmlFor="rag-topic" className={labelCls}>Tema *</label>
        <input
          id="rag-topic"
          type="text"
          value={topic}
          onChange={(e) => setTopic(e.target.value)}
          placeholder="Ej: La Corona, Derechos Fundamentales…"
          className={inputCls}
          required
        />
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

      {subjects.length > 0 && (
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className={labelCls}>Asignatura (opcional)</label>
            <select value={subjectId} onChange={(e) => setSubjectId(e.target.value)} className={inputCls}>
              <option value="">— Sin asignatura —</option>
              {subjects.map((s) => (
                <option key={s.id} value={s.id}>{s.name}</option>
              ))}
            </select>
          </div>
          <div>
            <label className={labelCls}>Unidad (opcional)</label>
            <select value={unitId} onChange={(e) => setUnitId(e.target.value)} className={inputCls} disabled={!subjectId || unitsLoading}>
              <option value="">— Sin unidad —</option>
              {units.map((u) => (
                <option key={u.id} value={u.id}>{u.name}</option>
              ))}
            </select>
          </div>
        </div>
      )}

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
