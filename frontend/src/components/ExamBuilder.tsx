import { useEffect, useState } from 'react';
import { Minus, Plus, Play } from 'flowbite-react-icons/outline';
import { apiBase, apiAuthJson } from '../api';
import type { UnitAvailability } from '../types';

const inp = 'bg-white/50 dark:bg-[#24394c] border border-secondary/30 rounded-xl px-4 py-2.5 text-sm text-text placeholder:text-text/35 focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary/50 transition-colors';

export default function ExamBuilder({ subjectId, onStart, onUnauthorized }: {
  subjectId: string;
  onStart: (cfg: { unitCounts: Record<string, number>; minutes: number; difficulty?: 'EASY' | 'MEDIUM' | 'HARD' }) => void;
  onUnauthorized: () => void;
}) {
  const [units, setUnits] = useState<UnitAvailability[]>([]);
  const [rules, setRules] = useState<Record<string, number>>({});
  const [time, setTime] = useState(20);
  const [difficulty, setDifficulty] = useState<'EASY' | 'MEDIUM' | 'HARD' | 'ALL'>('ALL');
  const [availabilityLoading, setAvailabilityLoading] = useState(false);
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 10;

  useEffect(() => {
    const token = localStorage.getItem('ak_token') || '';
    const diffQuery = difficulty === 'ALL' ? '' : `&difficulty=${difficulty}`;
    setAvailabilityLoading(true);
    apiAuthJson<UnitAvailability[]>(`${apiBase}/api/units/availability?subjectId=${subjectId}${diffQuery}`, token)
      .then(us => { setUnits(us.map(u => ({ id: u.id, name: u.name, available: Number(u.available) || 0 }))); setPage(0); })
      .catch(err => { if (err?.status === 401) onUnauthorized(); })
      .finally(() => setAvailabilityLoading(false));
  }, [subjectId, apiBase, difficulty]);

  const totalPages = Math.ceil(units.length / PAGE_SIZE);
  const pagedUnits = units.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);

  function setCount(id: string, count: number) {
    const unit = units.find(u => u.id === id);
    const max = unit ? unit.available : count;
    setRules(prev => ({ ...prev, [id]: Math.max(0, Math.min(count, max)) }));
  }
  function total() { return Object.values(rules).reduce((a, b) => a + (b || 0), 0); }
  const totalAvailable = units.reduce((sum, u) => sum + (u.available || 0), 0);

  return (
    <div className="max-w-3xl mx-auto">

      <div className="mb-8">
        <h1 className="text-3xl font-extrabold tracking-tight mb-1">
          Configura tu{' '}
          <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">
            examen
          </span>
        </h1>
        <p className="text-text/55 text-sm">Selecciona las unidades y el número de preguntas de cada una.</p>
      </div>

      {/* ── General config ── */}
      <div className="border border-secondary/25 rounded-2xl p-6 mb-5">
        <div className="text-xs text-text/50 uppercase tracking-wide font-semibold mb-4">Configuración general</div>
        <div className="grid sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-text/70 mb-1.5">Dificultad</label>
            <div className="flex items-center gap-2">
              <select
                className={inp + ' flex-1'}
                value={difficulty}
                onChange={e => setDifficulty(e.target.value as 'EASY' | 'MEDIUM' | 'HARD' | 'ALL')}
              >
                <option value="ALL">Todas</option>
                <option value="EASY">Fácil</option>
                <option value="MEDIUM">Media</option>
                <option value="HARD">Difícil</option>
              </select>
              {availabilityLoading && <span className="text-xs text-text/50 shrink-0">Actualizando...</span>}
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-text/70 mb-1.5">Tiempo total (min)</label>
            <input
              type="number"
              min={1}
              value={time}
              onChange={e => setTime(Number(e.target.value))}
              className={inp + ' w-28'}
            />
          </div>
        </div>
      </div>

      {/* ── Units ── */}
      <div className="border border-secondary/25 rounded-2xl p-6 mb-5">
        <div className="text-xs text-text/50 uppercase tracking-wide font-semibold mb-4">Preguntas por unidad</div>
        <div className="grid gap-3">
          {pagedUnits.map(u => {
            const disabled = (u.available || 0) === 0;
            const current = rules[u.id] || 0;
            return (
              <div
                key={u.id}
                className={`flex items-center justify-between rounded-xl px-4 py-3 border transition-colors ${
                  disabled ? 'border-secondary/15 opacity-50' : 'border-secondary/25'
                }`}
              >
                <div>
                  <div className="font-semibold text-sm">{u.name}</div>
                  <div className="text-xs text-text/45 mt-0.5">{u.available} disponibles</div>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setCount(u.id, current - 1)}
                    disabled={disabled || current === 0}
                    className="w-8 h-8 rounded-full border border-secondary/30 flex items-center justify-center hover:border-secondary/60 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
                    aria-label="Disminuir"
                  >
                    <Minus className="w-3.5 h-3.5" />
                  </button>
                  <input
                    type="number"
                    min={0}
                    max={u.available}
                    value={disabled ? 0 : current}
                    onChange={e => setCount(u.id, Number(e.target.value))}
                    disabled={disabled}
                    className="w-14 text-center bg-white/50 dark:bg-[#24394c] border border-secondary/30 rounded-lg py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/30 disabled:opacity-40"
                  />
                  <button
                    onClick={() => setCount(u.id, current + 1)}
                    disabled={disabled || current >= (u.available || 0)}
                    className="w-8 h-8 rounded-full border border-secondary/30 flex items-center justify-center hover:border-secondary/60 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
                    aria-label="Aumentar"
                  >
                    <Plus className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
        {totalPages > 1 && (
          <div className="flex items-center justify-between mt-4 pt-4 border-t border-secondary/20">
            <button
              onClick={() => setPage(p => p - 1)}
              disabled={page === 0}
              className="px-3 py-1.5 rounded-lg border border-secondary/30 text-sm hover:border-secondary/60 transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
            >
              ← Anterior
            </button>
            <span className="text-xs text-text/50">
              Página {page + 1} de {totalPages}
            </span>
            <button
              onClick={() => setPage(p => p + 1)}
              disabled={page >= totalPages - 1}
              className="px-3 py-1.5 rounded-lg border border-secondary/30 text-sm hover:border-secondary/60 transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
            >
              Siguiente →
            </button>
          </div>
        )}
      </div>

      {/* ── Footer ── */}
      <div className="flex items-center justify-between">
        <div className="text-sm text-text/60">
          Total seleccionadas: <span className="text-text font-bold text-base">{total()}</span>
        </div>
        <button
          onClick={() => onStart({ unitCounts: rules, minutes: time, difficulty: difficulty === 'ALL' ? undefined : difficulty })}
          disabled={totalAvailable === 0 || total() === 0}
          className="btn btn-primary rounded-full px-8 py-3 text-base shadow-lg shadow-primary/20 flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <Play className="w-5 h-5" />
          Empezar examen
        </button>
      </div>
    </div>
  );
}
