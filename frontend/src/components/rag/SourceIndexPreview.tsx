import { useState } from 'react';
import { confirmIndex } from '../../api';
import type { IndexPreview, SourceDocument, ApprovedUnit } from '../../types';

interface Props {
  preview: IndexPreview;
  onConfirmed: (doc: SourceDocument) => void;
  onCancel: () => void;
}

export default function SourceIndexPreview({ preview, onConfirmed, onCancel }: Props) {
  const [units, setUnits] = useState<ApprovedUnit[]>(
    preview.detectedUnits.map((u) => ({ headingKey: u.headingKey, name: u.name, description: '' }))
  );
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState('');

  function updateName(index: number, name: string) {
    setUnits((prev) => prev.map((u, i) => i === index ? { ...u, name } : u));
  }

  function removeUnit(index: number) {
    setUnits((prev) => prev.filter((_, i) => i !== index));
  }

  async function handleConfirm() {
    const valid = units.filter((u) => u.name.trim());
    if (valid.length === 0) {
      setError('Añade al menos una unidad para continuar.');
      return;
    }
    setError('');
    setConfirming(true);
    try {
      const result = await confirmIndex(preview.document.id, valid);
      onConfirmed(result.document);
    } catch (err: unknown) {
      const e = err as { body?: { message?: string } };
      setError(e?.body?.message || 'Error al confirmar el índice.');
    } finally {
      setConfirming(false);
    }
  }

  const inputCls = 'flex-1 rounded-lg border border-secondary/25 bg-secondary/5 px-3 py-1.5 text-sm focus:outline-none focus:border-primary/60 focus:ring-1 focus:ring-primary/30';

  return (
    <div className="space-y-5">
      <div>
        <h3 className="text-sm font-semibold text-text">
          Temas detectados en <span className="text-primary">{preview.document.name}</span>
        </h3>
        <p className="text-xs text-text/50 mt-1">
          Revisa y edita los nombres. Solo los temas confirmados se guardarán como unidades.
        </p>
      </div>

      {preview.detectedUnits.length === 0 && (
        <p className="text-sm text-yellow-600">
          No se detectaron temas estructurados en este documento. El documento se procesará sin unidades.
        </p>
      )}

      <ul className="space-y-2">
        {units.map((unit, i) => {
          const original = preview.detectedUnits.find((d) => d.headingKey === unit.headingKey);
          return (
            <li key={unit.headingKey} className="flex items-center gap-2">
              <span className="text-xs text-text/40 w-5 text-right shrink-0">{i + 1}.</span>
              <input
                type="text"
                value={unit.name}
                onChange={(e) => updateName(i, e.target.value)}
                className={inputCls}
                placeholder="Nombre de la unidad"
              />
              {original && (
                <span className="text-xs text-text/40 shrink-0">
                  ({original.chunkCount} fragmento{original.chunkCount !== 1 ? 's' : ''})
                </span>
              )}
              <button
                type="button"
                onClick={() => removeUnit(i)}
                className="text-text/30 hover:text-red-500 transition-colors text-lg leading-none shrink-0"
                title="Eliminar unidad"
              >
                ×
              </button>
            </li>
          );
        })}
      </ul>

      {error && <p role="alert" className="text-sm text-red-500">{error}</p>}

      <div className="flex gap-3">
        <button
          type="button"
          onClick={handleConfirm}
          disabled={confirming}
          className="flex-1 py-2.5 rounded-xl bg-primary text-white font-semibold text-sm hover:opacity-90 transition-opacity disabled:opacity-40 disabled:cursor-not-allowed"
        >
          {confirming ? 'Guardando…' : `Confirmar ${units.filter(u => u.name.trim()).length} unidad${units.filter(u => u.name.trim()).length !== 1 ? 'es' : ''}`}
        </button>
        <button
          type="button"
          onClick={onCancel}
          disabled={confirming}
          className="px-4 py-2.5 rounded-xl border border-secondary/25 text-sm text-text/60 hover:text-text hover:bg-secondary/5 transition-colors disabled:opacity-40"
        >
          Cancelar
        </button>
      </div>
    </div>
  );
}
