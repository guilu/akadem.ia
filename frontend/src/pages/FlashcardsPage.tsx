import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { apiBase } from '../api';
import { apiAuthJson } from '../api';
import FlashcardImportModal from '../components/flashcards/FlashcardImportModal';
import FlashcardsTabs from '../components/flashcards/FlashcardsTabs';
import SearchInput from '../components/flashcards/SearchInput';
import UnitList from '../components/flashcards/UnitList';

export type UnitSummary = {
  unitId: string;
  unitName: string;
  newCount: number;
  reviewCount: number;
  dueCount?: number;
};

type GlobalQueue = { new: number; due: number; learning: number };

export default function FlashcardsPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const mode = searchParams.get('mode') === 'study' ? 'estudio' : 'examinar';

  const [units, setUnits] = useState<UnitSummary[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>('');
  const [globalQueue, setGlobalQueue] = useState<GlobalQueue | null>(null);
  const [globalLoading, setGlobalLoading] = useState(true);
  const [showImport, setShowImport] = useState(false);

  const token = localStorage.getItem('ak_token') || '';

  const loadUnits = () => {
    setLoading(true);
    setError('');
    apiAuthJson<UnitSummary[]>(`${apiBase}/api/flashcards/units/summary`, token)
      .then((data) => setUnits(data || []))
      .catch(() => setError('No se pudieron cargar las unidades.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setError('');
    apiAuthJson<UnitSummary[]>(`${apiBase}/api/flashcards/units/summary`, token)
      .then((data) => { if (mounted) setUnits(data || []); })
      .catch(() => { if (mounted) setError('No se pudieron cargar las unidades.'); })
      .finally(() => { if (mounted) setLoading(false); });
    return () => { mounted = false; };
  }, [token]);

  useEffect(() => {
    let mounted = true;
    setGlobalLoading(true);
    apiAuthJson<GlobalQueue>(`${apiBase}/api/flashcards/study/queue`, token)
      .then((data) => { if (mounted) setGlobalQueue(data || null); })
      .catch(() => {})
      .finally(() => { if (mounted) setGlobalLoading(false); });
    return () => { mounted = false; };
  }, [token]);

  const filteredUnits = useMemo(() => {
    const q = search.trim().toLowerCase();
    return q ? units.filter((u) => u.unitName.toLowerCase().includes(q)) : units;
  }, [units, search]);

  const totalPending = globalQueue ? globalQueue.new + globalQueue.due + globalQueue.learning : 0;

  const handleUnitClick = (unit: UnitSummary) => {
    if (mode === 'estudio') {
      navigate(`/flashcards/study?unitId=${unit.unitId}`);
    } else {
      navigate(`/flashcards/examine?unitId=${unit.unitId}`);
    }
  };

  const handleExport = async (unit: UnitSummary, format: 'csv' | 'json') => {
    try {
      const res = await fetch(
        `${apiBase}/api/flashcards/export?unitId=${unit.unitId}&format=${format}`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      if (!res.ok) return;
      const content = await res.text();
      const blob = new Blob([content], {
        type: format === 'json' ? 'application/json' : 'text/csv',
      });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = `${unit.unitName}.${format}`;
      link.click();
      URL.revokeObjectURL(link.href);
    } catch {
      // silently ignore — browser will show nothing
    }
  };

  return (
    <div className="space-y-6">
      <header className="space-y-3">
        <div className="py-[1.5rem] flex items-start justify-between">
          <div>
            <h1 className="text-3xl font-extrabold tracking-tight">
              Flash
              <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">
                cards
              </span>
            </h1>
            <p className="text-text/55 text-sm mt-1">Repasa por unidades con repetición espaciada.</p>
          </div>
          <button
            type="button"
            onClick={() => setShowImport(true)}
            className="mt-1 flex items-center gap-1.5 px-3 py-2 rounded-xl border border-secondary/30 text-sm text-text/60 hover:border-primary/40 hover:text-text transition-colors"
          >
            <span>⬆</span> Importar
          </button>
        </div>
        <FlashcardsTabs active={mode} onTab={(tab) => {
          if (tab === 'estudio') navigate('/flashcards?mode=study');
          if (tab === 'examinar') navigate('/flashcards');
          if (tab === 'historial') navigate('/flashcards/history');
        }} />
      </header>

      {/* Global queue strip — only in study mode */}
      {mode === 'estudio' && (
        globalLoading ? (
          <div className="h-12 rounded-2xl border border-secondary/15 bg-secondary/5 animate-pulse" />
        ) : globalQueue && totalPending === 0 ? (
          <div className="border border-secondary/25 rounded-2xl px-5 py-3 text-center text-sm text-text/60">
            Nada pendiente hoy 🎉
          </div>
        ) : globalQueue ? (
          <div className="border border-secondary/25 rounded-2xl px-5 py-3 flex items-center justify-around text-sm font-medium">
            <span className="text-lime-500">🆕 {globalQueue.new} nuevas</span>
            <span className="text-accent">⚡ {globalQueue.learning} aprendiendo</span>
            <span className="text-primary">🔁 {globalQueue.due} pendientes</span>
          </div>
        ) : null
      )}

      <section className="space-y-4">
        <SearchInput value={search} onChange={setSearch} />
        <UnitList
          loading={loading}
          error={error}
          units={filteredUnits}
          onUnitClick={handleUnitClick}
          onExport={handleExport}
        />
      </section>

      {showImport && (
        <FlashcardImportModal
          units={units}
          token={token}
          onClose={() => setShowImport(false)}
          onImported={loadUnits}
        />
      )}
    </div>
  );
}
