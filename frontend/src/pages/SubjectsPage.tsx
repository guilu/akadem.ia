import { Link } from 'react-router-dom';
import type { Subject } from '../types';

export default function SubjectsPage({ subjects }: { subjects: Subject[] }) {
  return (
    <section className="mb-8">
      <h2 className="text-xl font-semibold mb-2">Asignaturas</h2>
      <div className="grid sm:grid-cols-2 gap-4">
        {subjects.map(s => (
          <div key={s.id} className="border border-slate-700 rounded-xl p-4">
            <div className="text-lg font-semibold">{s.name}</div>
            <div className="text-sm text-slate-400">{s.description}</div>
            <div className="mt-3">
              <Link className="px-3 py-2 rounded-xl bg-cyan-600 inline-flex" to={`/subjects/${s.id}/builder`}>
                Crear simulacro
              </Link>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
