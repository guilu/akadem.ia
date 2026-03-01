import UnitCard from './UnitCard';
import type { UnitSummary } from '../../pages/FlashcardsPage';

export default function UnitList({
  units,
  loading,
  error,
}: {
  units: UnitSummary[];
  loading: boolean;
  error: string;
}) {
  if (loading) {
    return (
      <div className="space-y-3">
        {[1, 2, 3].map((i) => (
          <div key={i} className="h-20 rounded-2xl bg-gray-100/80 dark:bg-slate-800 animate-pulse" />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
        {error}
      </div>
    );
  }

  if (!units.length) {
    return (
      <div className="rounded-2xl border border-gray-200 bg-white/80 px-4 py-6 text-center dark:border-slate-700 dark:bg-slate-800/50">
        <p className="text-sm font-medium text-slate-700 dark:text-slate-300">No hay unidades con tarjetas</p>
        <p className="mt-1 text-sm text-slate-500">Ve a <strong>Ajustes</strong> para añadir flashcards a tus unidades.</p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {units.map((unit) => (
        <UnitCard key={unit.unitId} unit={unit} />
      ))}
    </div>
  );
}
