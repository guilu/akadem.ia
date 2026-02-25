import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiAuthJson, apiBase } from '../api';
import FlashcardsTabs from '../components/flashcards/FlashcardsTabs';
import SearchInput from '../components/flashcards/SearchInput';
import UnitList from '../components/flashcards/UnitList';

export type UnitSummary = {
  unitId: string;
  unitName: string;
  totalCards: number;
  newCards: number;
  dueCards: number;
};

export default function FlashcardsPage(){
  const navigate = useNavigate();
  const [units, setUnits] = useState<UnitSummary[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>('');

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

  const filteredUnits = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return units;
    return units.filter((u) => u.unitName.toLowerCase().includes(q));
  }, [units, search]);

  return (
    <div className="space-y-6">
      <header className="space-y-2">
        <h1 className="text-3xl font-bold">Flashcards</h1>
        <FlashcardsTabs active="examinar" onTab={(tab) => {
          if (tab === 'estudio') navigate('/flashcards/study');
          if (tab === 'historial') navigate('/flashcards/history');
        }} />
      </header>

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
