import UnitCard from './UnitCard';
import type { UnitSummary } from '../../pages/FlashcardsPage';

type Props = {
  units: UnitSummary[];
  loading: boolean;
  error: string;
  onUnitClick?: (unit: UnitSummary) => void;
  onExport?: (unit: UnitSummary, format: 'csv' | 'json') => void;
  onImport?: () => void;
};

export default function UnitList({ units, loading, error, onUnitClick, onExport, onImport }: Props) {
  if (loading) {
    return (
      <div className="space-y-3">
        {[1, 2, 3].map((i) => (
          <div key={i} className="h-20 rounded-2xl border border-secondary/15 bg-secondary/5 animate-pulse" />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm text-red-400">
        {error}
      </div>
    );
  }

  if (!units.length) {
    return (
      <div className="border border-secondary/25 rounded-2xl px-5 py-8 text-center space-y-4">
        <p className="text-sm text-text/55">No hay unidades disponibles.</p>
        {onImport && (
          <button
            type="button"
            onClick={onImport}
            className="btn btn-primary rounded-full px-6 py-2 text-sm shadow-sm shadow-primary/15"
          >
            Importar flashcards
          </button>
        )}
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {units.map((unit) => (
        <UnitCard
          key={unit.unitId}
          unit={unit}
          onClick={onUnitClick ? () => onUnitClick(unit) : undefined}
          onExport={onExport ? (fmt) => onExport(unit, fmt) : undefined}
        />
      ))}
    </div>
  );
}
