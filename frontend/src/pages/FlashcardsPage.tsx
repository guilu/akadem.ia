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

export default function FlashcardsPage(){
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
      .then((data) => {
        if (!mounted) return;
        setUnits(data || []);
      })
      .catch(() => {
        if (!mounted) return;
        setError('No se pudieron cargar las unidades.');
      })
      .finally(() => {
        if (!mounted) return;
        setLoading(false);
      });
    return () => { mounted = false; };
  }, [token]);

  useEffect(() => {
    let mounted = true;
    setGlobalLoading(true);
    apiAuthJson<GlobalQueue>(`${apiBase}/api/flashcards/study/queue`, token)
      .then((data) => {
        if (!mounted) return;
        setGlobalQueue(data || null);
      })
      .catch(() => { /* silencioso, no bloquea la lista de unidades */ })
      .finally(() => {
        if (!mounted) return;
        setGlobalLoading(false);
      });
    return () => { mounted = false; };
  }, [token]);

  const filteredUnits = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return units;
    return units.filter((u) => u.unitName.toLowerCase().includes(q));
  }, [units, search]);

  const totalPending = globalQueue ? globalQueue.new + globalQueue.due + globalQueue.learning : 0;

  return (
    <div className="space-y-6">
      <header className="space-y-2">
        <h1 className="text-3xl font-bold">Flashcards</h1>
        <FlashcardsTabs active="examinar" onTab={(tab) => {
          if (tab === 'estudio') navigate('/flashcards/study');
          if (tab === 'historial') navigate('/flashcards/history');
        }} />
      </header>

      {globalLoading ? (
        <div className="animate-pulse h-12 rounded-2xl bg-white/60 dark:bg-slate-800/60" />
      ) : globalQueue && totalPending === 0 ? (
        <div className="rounded-2xl bg-white dark:bg-slate-900 px-4 py-3 shadow text-center text-sm text-secondary">
          Nada pendiente hoy 🎉
        </div>
      ) : globalQueue ? (
        <div className="flex items-center justify-around rounded-2xl bg-white dark:bg-slate-900 px-4 py-3 shadow text-sm font-medium">
          <span className="text-emerald-600">🆕 {globalQueue.new} nuevas</span>
          <span className="text-amber-500">⚡ {globalQueue.learning} aprendiendo</span>
          <span className="text-blue-500">🔁 {globalQueue.due} pendientes</span>
        </div>
      ) : null}

      <section className="space-y-4">
        <SearchInput value={search} onChange={setSearch} />
        <UnitList
          loading={loading}
          error={error}
          units={filteredUnits}
        />
      </section>
    </div>
  );
}
