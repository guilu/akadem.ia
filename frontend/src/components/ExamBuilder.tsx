import { useEffect, useState } from 'react';
import {
  Minus, Plus, Play, AdjustmentsHorizontal, Shuffle,
  ClipboardList, Lightbulb, ChevronLeft, ChevronRight,
} from 'flowbite-react-icons/outline';
import { apiBase, apiJson } from '../api';
import type { UnitAvailability } from '../types';

const inp = 'bg-white/50 dark:bg-[#24394c] border border-secondary/30 rounded-xl px-4 py-2.5 text-sm text-text placeholder:text-text/35 focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary/50 transition-colors';

export default function ExamBuilder({ subjectId, onStart, onStartRandom, onUnauthorized }: {
  subjectId: string;
  onStart: (cfg: { unitCounts: Record<string, number>; minutes: number; difficulty?: 'EASY' | 'MEDIUM' | 'HARD' }) => void;
  onStartRandom: (cfg: { subjectId: string; count: number; minutes: number; difficulty?: 'EASY' | 'MEDIUM' | 'HARD' }) => void;
  onUnauthorized: () => void;
}) {
  const [units, setUnits] = useState<UnitAvailability[]>([]);
  const [rules, setRules] = useState<Record<string, number>>({});
  const [time, setTime] = useState(20);
  const [difficulty, setDifficulty] = useState<'EASY' | 'MEDIUM' | 'HARD' | 'ALL'>('ALL');
  const [availabilityLoading, setAvailabilityLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [randomCount, setRandomCount] = useState(10);
  const PAGE_SIZE = 6;

  useEffect(() => {
    const diffQuery = difficulty === 'ALL' ? '' : `&difficulty=${difficulty}`;
    setAvailabilityLoading(true);
    apiJson<UnitAvailability[]>(`${apiBase}/api/units/availability?subjectId=${subjectId}${diffQuery}`)
      .then(us => { setUnits(us.map(u => ({ id: u.id, name: u.name, available: Number(u.available) || 0 }))); setPage(0); })
      .catch((err: unknown) => { if (err && typeof err === 'object' && 'status' in err && (err as { status: number }).status === 401) onUnauthorized(); })
      .finally(() => setAvailabilityLoading(false));
  }, [subjectId, difficulty]);

  const totalPages = Math.ceil(units.length / PAGE_SIZE);
  const pagedUnits = units.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);

  function setCount(id: string, count: number) {
    const unit = units.find(u => u.id === id);
    const max = unit ? unit.available : count;
    setRules(prev => ({ ...prev, [id]: Math.max(0, Math.min(count, max)) }));
  }
  function total() { return Object.values(rules).reduce((a, b) => a + (b || 0), 0); }
  function clearRules() { setRules({}); }
  const totalAvailable = units.reduce((sum, u) => sum + (u.available || 0), 0);

  return (
    <div className="max-w-8xl mx-auto pb-32">

      {/* ── Hero ── */}
      <div className="mb-12">
        <h1 className="text-4xl font-extrabold tracking-tight mb-2">
          Configura tu{' '}
          <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">
            examen
          </span>
        </h1>
        <p className="text-text/55 max-w-2xl">
          Diseña una sesión de estudio personalizada ajustando la dificultad, el tiempo y los temas
          específicos para optimizar tu rendimiento académico.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">

        {/* ── Left column: config + tip ── */}
        <div className="lg:col-span-4 space-y-6">
          <div className="border border-secondary/25 rounded-3xl bg-card p-8 flex flex-col gap-6">
            <div className="flex items-center gap-3">
              <AdjustmentsHorizontal className="w-5 h-5 text-primary" />
              <h2 className="text-xl font-bold">Configuración General</h2>
            </div>

            {/* Difficulty */}
            <div className="space-y-2">
              <label className="text-sm font-semibold text-text/55">
                Dificultad
                {availabilityLoading && <span className="ml-1.5 text-text/35 font-normal">· actualizando…</span>}
              </label>
              <select
                className={inp + ' w-full'}
                value={difficulty}
                onChange={e => setDifficulty(e.target.value as 'EASY' | 'MEDIUM' | 'HARD' | 'ALL')}
              >
                <option value="ALL">Todas</option>
                <option value="EASY">Fácil</option>
                <option value="MEDIUM">Media</option>
                <option value="HARD">Difícil</option>
              </select>
            </div>

            {/* Time */}
            <div className="space-y-2">
              <label className="text-sm font-semibold text-text/55">Tiempo total en min</label>
              <div className="flex items-center gap-2">
                <svg className="w-4 h-4 text-text/40 shrink-0" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clipRule="evenodd" />
                </svg>
              <input
                type="number"
                min={1}
                value={time}
                onChange={e => setTime(Number(e.target.value))}
                className={inp + ' flex-1'}
                placeholder="Ej. 60"
              />
              </div>
            </div>

            {/* Random count */}
            <div className="space-y-2">
              <label className="text-sm font-semibold text-text/55">
                Preguntas aleatorias{' '}
                <span className="text-text/35 font-normal">(máx. {totalAvailable})</span>
              </label>
              <div className="flex items-center gap-2">
                <Shuffle className="w-4 h-4 text-text/40 shrink-0" />
                <input
                  type="number"
                  min={1}
                  max={totalAvailable || 1}
                  value={randomCount}
                  onChange={e => setRandomCount(Math.max(1, Math.min(Number(e.target.value), totalAvailable || 1)))}
                  disabled={totalAvailable === 0}
                  className={inp + ' flex-1 disabled:opacity-40'}
                />
              </div>
            </div>

            <button
              onClick={() => onStartRandom({ subjectId, count: randomCount, minutes: time, difficulty: difficulty === 'ALL' ? undefined : difficulty })}
              disabled={totalAvailable === 0 || randomCount < 1}
              className="btn btn-primary w-full mt-2 py-4 rounded-3xl font-bold text-lg flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Play className="w-6 h-6" />
              Examen aleatorio
            </button>
          </div>

          {/* Tip card */}
          <div className="bg-primary/10 rounded-3xl p-6 border border-primary/20">
            <div className="flex items-start gap-4">
              <Lightbulb className="w-5 h-5 text-primary shrink-0 mt-0.5" />
              <div>
                <p className="text-primary font-bold mb-1">Tip de estudio</p>
                <p className="text-sm text-text/60">
                  Configurar exámenes con tiempo reducido ayuda a simular la presión real del examen
                  y mejora tu gestión del tiempo.
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* ── Right column: units ── */}
        <div className="lg:col-span-8">
          <div className="border border-secondary/25 rounded-3xl bg-card p-8 mb-20">
            <div className="flex items-center gap-3 mb-8">
              <ClipboardList className="w-5 h-5 text-primary" />
              <h2 className="text-xl font-bold">Preguntas por unidad</h2>
            </div>

            <div className="space-y-4">
              {pagedUnits.map((u, i) => {
                const disabled = (u.available || 0) === 0;
                const current = rules[u.id] || 0;
                return (
                  <div
                    key={u.id}
                    className={`flex flex-col sm:flex-row sm:items-center sm:justify-between p-6 border rounded-2xl transition-all gap-4 ${
                      disabled
                        ? 'border-secondary/10 opacity-50'
                        : 'border-secondary/20 hover:border-primary/30'
                    }`}
                  >
                    <div className="flex items-center gap-4">
                      <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center shrink-0">
                        <span className="text-primary font-bold text-sm">{page * PAGE_SIZE + i + 1}</span>
                      </div>
                      <div>
                        <h3 className="font-bold text-sm">{u.name}</h3>
                        <p className="text-xs font-semibold text-text/45 uppercase tracking-wider mt-0.5">
                          {u.available} disponibles
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-3 border border-secondary/15 p-2 rounded-2xl self-center sm:self-auto">
                      <button
                        onClick={() => setCount(u.id, current - 1)}
                        disabled={disabled || current === 0}
                        className="w-8 h-8 rounded-lg flex items-center justify-center border border-secondary/25 hover:border-primary/50 hover:text-primary transition-all disabled:opacity-40 disabled:cursor-not-allowed"
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
                        className="w-[3.75rem] text-center bg-white/50 dark:bg-[#24394c] border border-secondary/30 rounded-lg py-1 font-bold text-base focus:outline-none focus:ring-2 focus:ring-primary/30 disabled:opacity-40"
                      />
                      <button
                        onClick={() => setCount(u.id, current + 1)}
                        disabled={disabled || current >= (u.available || 0)}
                        className="w-8 h-8 rounded-lg flex items-center justify-center border border-secondary/25 hover:border-primary/50 hover:text-primary transition-all disabled:opacity-40 disabled:cursor-not-allowed"
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
              <div className="mt-8 flex items-center justify-center gap-4">
                <button
                  onClick={() => setPage(p => p - 1)}
                  disabled={page === 0}
                  className="flex items-center gap-2 px-3 sm:px-6 py-2 rounded-xl font-semibold text-text/55 hover:bg-secondary/10 transition-all disabled:opacity-30 disabled:cursor-not-allowed"
                >
                  <ChevronLeft className="w-6 h-6" />
                  <span className="hidden sm:inline">Anterior</span>
                </button>
                <div className="flex gap-2">
                  {(() => {
                    const WINDOW = 4;
                    const start = Math.max(0, Math.min(page - 1, totalPages - WINDOW));
                    const end = Math.min(start + WINDOW, totalPages);
                    return Array.from({ length: end - start }, (_, i) => start + i).map(i => (
                      <button
                        key={i}
                        onClick={() => setPage(i)}
                        className={`w-10 h-10 rounded-lg flex items-center justify-center font-bold transition-all ${
                          i === page ? 'bg-primary text-bg' : 'hover:bg-secondary/15'
                        }`}
                      >
                        {i + 1}
                      </button>
                    ));
                  })()}
                </div>
                <button
                  onClick={() => setPage(p => p + 1)}
                  disabled={page >= totalPages - 1}
                  className="flex items-center gap-2 px-3 sm:px-6 py-2 rounded-xl font-semibold text-text/55 hover:bg-secondary/10 transition-all disabled:opacity-30 disabled:cursor-not-allowed"
                >
                  <span className="hidden sm:inline">Siguiente</span>
                  <ChevronRight className="w-6 h-6" />
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* ── Sticky bottom bar ── */}
      <div className="fixed bottom-0 left-0 right-0 z-50 bg-bg/90 backdrop-blur-2xl border-t border-secondary/20 px-8 py-6">
        <div className="max-w-8xl mx-auto flex flex-col md:flex-row items-center justify-between gap-6">
          <div className="flex items-center gap-8">
            <div className="flex flex-col">
              <span className="text-xs font-bold uppercase tracking-widest text-text/40 mb-1">
                Resumen de selección
              </span>
              <div className="flex items-baseline gap-2">
                <span className="text-3xl font-black text-primary">{total()}</span>
                <span className="text-sm font-semibold text-text/55">Total seleccionadas</span>
              </div>
            </div>
            <div className="h-12 w-px bg-secondary/20 hidden md:block" />
            <div className="hidden md:flex items-center gap-4">
              <div className="flex -space-x-4">
                <div className="w-12 h-12 rounded-full border-2 border-bg bg-primary flex items-center justify-center z-0">
                  <svg className="w-5 h-5 text-bg" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clipRule="evenodd" />
                  </svg>
                </div>
                <div className="w-12 h-12 rounded-full border-2 border-bg bg-secondary/40 dark:bg-[#1F2E40] flex items-center justify-center text-sm font-bold z-10">
                  {total()}m
                </div>
              </div>
              <p className="text-sm font-medium text-text/55">Estimación de tiempo</p>
            </div>
          </div>

          <div className="flex items-center gap-4 w-full md:w-auto">
            <button
              onClick={clearRules}
              className="flex-1 md:flex-none px-8 py-4 rounded-3xl font-bold text-text/55 hover:bg-secondary/15 transition-all"
            >
              Limpiar
            </button>
            <button
              onClick={() => onStart({ unitCounts: rules, minutes: time, difficulty: difficulty === 'ALL' ? undefined : difficulty })}
              disabled={totalAvailable === 0 || total() === 0}
              className="flex-[2] md:flex-none btn btn-primary px-12 py-4 rounded-3xl font-extrabold text-xl shadow-lg shadow-primary/20 flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <span className="sm:hidden">Empezar</span>
              <span className="hidden sm:inline">Empezar examen</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
