import { useEffect, useState } from 'react';
import { Minus, Plus, Play } from 'flowbite-react-icons/outline';
import { apiBase, apiAuthJson } from '../api';
import type { UnitAvailability } from '../types';
import { Select, TextInput } from 'flowbite-react';

export default function ExamBuilder({ subjectId, onStart, onUnauthorized }:{ subjectId: string; onStart: (cfg:{ unitCounts: Record<string, number>, minutes: number, difficulty?: 'EASY' | 'MEDIUM' | 'HARD' })=>void; onUnauthorized: ()=>void }){
  const [units, setUnits] = useState<UnitAvailability[]>([]);
  const [rules, setRules] = useState<Record<string, number>>({});
  const [time, setTime] = useState(20);
  const [difficulty, setDifficulty] = useState<'EASY' | 'MEDIUM' | 'HARD' | 'ALL'>('ALL');
  const [availabilityLoading, setAvailabilityLoading] = useState(false);

  useEffect(() => {
    const token = localStorage.getItem('ak_token') || '';
    const diffQuery = difficulty === 'ALL' ? '' : `&difficulty=${difficulty}`;
    setAvailabilityLoading(true);
    apiAuthJson<UnitAvailability[]>(`${apiBase}/api/units/availability?subjectId=${subjectId}${diffQuery}`, token)
      .then(us => {
        const mapped = us.map(u => ({ id: u.id, name: u.name, available: Number(u.available) || 0 }));
        setUnits(mapped);
      })
      .catch(err => {
        if (err?.status === 401) onUnauthorized();
      })
      .finally(() => setAvailabilityLoading(false));
  }, [subjectId, apiBase, difficulty]);

  function setCount(id: string, count: number){
    const unit = units.find(u => u.id === id);
    const max = unit ? unit.available : count;
    const safe = Math.max(0, Math.min(count, max));
    setRules(prev => ({ ...prev, [id]: safe }));
  }
  function total(){ return Object.values(rules).reduce((a,b)=>a+(b||0),0); }
  const totalAvailable = units.reduce((sum, u) => sum + (u.available || 0), 0);

  return (
    <div className="max-w-3xl mx-auto p-4">
      <h1 className="text-2xl font-bold mb-4">Configura tu examen</h1>

      <div className="grid gap-3 mb-4">
        <div className="grid grid-cols-2 items-center gap-3">
          <label>Dificultad</label>
          <div className="flex items-center gap-2">
            <Select
              value={difficulty}
              onChange={e=>setDifficulty(e.target.value as 'EASY' | 'MEDIUM' | 'HARD' | 'ALL')}
            >
              <option value="ALL">Todas</option>
              <option value="EASY">Fácil</option>
              <option value="MEDIUM">Media</option>
              <option value="HARD">Difícil</option>
            </Select>
            {availabilityLoading && <span className="text-xs text-text/60">Actualizando...</span>}
          </div>
        </div>
        <div className="grid grid-cols-2 items-center gap-3">
          <label>Tiempo total (min)</label>
          <TextInput type="number" min={1} value={time} onChange={e=>setTime(Number(e.target.value))}
            className="w-24"/>
        </div>
      </div>

      <div className="grid gap-3">
        {units.map(u => {
          const disabled = (u.available || 0) === 0;
          const current = rules[u.id] || 0;
          return (
            <div key={u.id} className={`flex items-center justify-between border rounded-xl p-3 ${disabled ? 'border-secondary/30 opacity-60' : 'border-slate-600'}`}>
              <div>
                <div className="font-semibold">{u.name}</div>
                <div className="text-sm text-slate-400">Disponibles: {u.available}</div>
              </div>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setCount(u.id, current - 1)}
                  disabled={disabled}
                  className="btn btn-secondary w-8 h-8 flex items-center justify-center"
                  aria-label="disminuir">
                  <Minus className="w-4 h-4" />
                </button>
                <TextInput
                  type="number"
                  min={0}
                  max={u.available}
                  value={disabled ? 0 : current}
                  onChange={e => setCount(u.id, Number(e.target.value))}
                  className="w-16 text-center"
                  disabled={disabled}
                />
                <button
                  onClick={() => setCount(u.id, current + 1)}
                  disabled={disabled}
                  className="btn btn-secondary w-8 h-8 flex items-center justify-center"
                  aria-label="aumentar">
                  <Plus className="w-4 h-4" />
                </button>
              </div>
            </div>
          );
        })}
      </div>

      <div className="mt-4 flex items-center justify-between">
        <div>Total preguntas: <strong>{total()}</strong></div>
        <button
          onClick={()=>onStart({ unitCounts: rules, minutes: time, difficulty: difficulty === 'ALL' ? undefined : difficulty })}
          className="btn btn-primary flex items-center gap-2"
          disabled={totalAvailable === 0}
        >
          <Play className="w-4 h-4" />
          Empezar
        </button>
      </div>
    </div>
  );
}
