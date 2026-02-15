import { useEffect, useState } from 'react';
import { apiBase, apiAuthJson } from '../api';
import type { UnitAvailability } from '../types';

export default function ExamBuilder({ subjectId, onStart, onUnauthorized }:{ subjectId: string; onStart: (cfg:{ unitCounts: Record<string, number>, minutes: number })=>void; onUnauthorized: ()=>void }){
  const [units, setUnits] = useState<Unit[]>([]);
  const [rules, setRules] = useState<Record<string, number>>({});
  const [time, setTime] = useState(20);

  useEffect(() => {
    const token = localStorage.getItem('ak_token') || '';
    apiAuthJson<UnitAvailability[]>(`${apiBase}/api/units/availability?subjectId=${subjectId}`, token)
      .then(us => {
        const mapped = us.map(u => ({ id: u.id, name: u.name, available: Number(u.available) || 0 }));
        setUnits(mapped);
      })
      .catch(err => {
        if (err?.status === 401) onUnauthorized();
        setUnits([]);
      });
  }, [subjectId, apiBase]);

  function setCount(id: string, count: number){
    const unit = units.find(u => u.id === id);
    const max = unit ? unit.available : count;
    const safe = Math.max(0, Math.min(count, max));
    setRules(prev => ({ ...prev, [id]: safe }));
  }
  function total(){ return Object.values(rules).reduce((a,b)=>a+(b||0),0); }

  return (
    <div className="max-w-3xl mx-auto p-4">
      <h1 className="text-2xl font-bold mb-4">Configura tu examen</h1>
      <div className="grid gap-3">
        {units.map(u => (
          <div key={u.id} className="flex items-center justify-between border border-slate-600 rounded-xl p-3">
            <div>
              <div className="font-semibold">{u.name}</div>
              <div className="text-sm text-slate-400">Disponibles: {u.available}</div>
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setCount(u.id, (rules[u.id] || 0) - 1)}
                className="w-8 h-8 rounded bg-slate-800 border border-slate-600"
                aria-label="disminuir">
                −
              </button>
              <input
                type="number"
                min={0}
                max={u.available}
                value={rules[u.id] || 0}
                onChange={e => setCount(u.id, Number(e.target.value))}
                className="w-16 text-center bg-slate-900 border border-slate-700 rounded px-2 py-1"
              />
              <button
                onClick={() => setCount(u.id, (rules[u.id] || 0) + 1)}
                className="w-8 h-8 rounded bg-slate-800 border border-slate-600"
                aria-label="aumentar">
                +
              </button>
            </div>
          </div>
        ))}
      </div>

      <div className="mt-4 flex items-center gap-3">
        <label>Tiempo total (min)</label>
        <input type="number" min={1} value={time} onChange={e=>setTime(Number(e.target.value))}
          className="w-24 bg-slate-900 border border-slate-700 rounded px-2 py-1"/>
      </div>

      <div className="mt-4 flex items-center justify-between">
        <div>Total preguntas: <strong>{total()}</strong></div>
        <button onClick={()=>onStart({ unitCounts: rules, minutes: time })} className="px-4 py-2 rounded-xl bg-indigo-600">Empezar</button>
      </div>
    </div>
  );
}
