import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiAuthJson, apiBase } from '../api';
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
  const [units, setUnits] = useState<UnitSummary[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>('');
  const [globalQueue, setGlobalQueue] = useState<GlobalQueue | null>(null);
  const [globalLoading, setGlobalLoading] = useState(true);

  const token = localStorage.getItem('ak_token') || '';

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

  return (
    <div className="space-y-6">
      <header className="space-y-3">
        <div className="py-[1.5rem]">
          <h1 className="text-3xl font-extrabold tracking-tight">
            Flash
            <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">
              cards
            </span>
          </h1>
          <p className="text-text/55 text-sm mt-1">Repasa por unidades con repetición espaciada.</p>
        </div>
        <FlashcardsTabs active="examinar" onTab={(tab) => {
          if (tab === 'estudio') navigate('/flashcards/study');
          if (tab === 'historial') navigate('/flashcards/history');
        }} />
      </header>

      {/* ── Global queue strip ── */}
      {globalLoading ? (
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
      ) : null}

      <section className="space-y-4">
        <SearchInput value={search} onChange={setSearch} />
        <UnitList loading={loading} error={error} units={filteredUnits} />
      </section>
    </div>
  );
}
