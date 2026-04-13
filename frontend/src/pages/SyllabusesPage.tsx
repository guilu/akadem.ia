import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { BookOpen, Plus } from 'flowbite-react-icons/outline';
import { getSyllabuses } from '../api/syllabusApi';
import type { Syllabus } from '../types';
import { ROUTES } from '../constants/routes';

export default function SyllabusesPage() {
  const [syllabuses, setSyllabuses] = useState<Syllabus[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    setLoading(true);
    setError('');
    getSyllabuses()
      .then(setSyllabuses)
      .catch(() => setError('No se pudieron cargar los temarios.'))
      .finally(() => setLoading(false));
  }, []);

  return (
    <section className="mb-8">
      <div className="py-[1.5rem] mb-6">
        <h1 className="text-3xl font-extrabold tracking-tight">
          Elige tu{' '}
          <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">
            temario
          </span>
        </h1>
        <p className="text-text/55 text-sm mt-1">Selecciona un temario para explorar sus materias.</p>
      </div>

      {loading && (
        <div className="text-sm text-text/55">Cargando temarios...</div>
      )}

      {!loading && error && (
        <div className="rounded-xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm text-red-400">{error}</div>
      )}

      {!loading && !error && syllabuses.length === 0 && (
        <div className="border border-secondary/25 rounded-2xl px-5 py-10 text-center space-y-4">
          <div className="w-12 h-12 rounded-2xl bg-primary/10 text-primary flex items-center justify-center mx-auto">
            <BookOpen className="w-6 h-6" />
          </div>
          <div>
            <p className="font-bold text-base mb-1">No hay temarios disponibles</p>
            <p className="text-sm text-text/55">Aún no hay temarios. Pide a un administrador que cree uno.</p>
          </div>
        </div>
      )}

      {!loading && !error && syllabuses.length > 0 && (
        <div className="grid sm:grid-cols-2 gap-4">
          {syllabuses.map((s) => (
            <article
              key={s.id}
              className="group border border-secondary/25 rounded-2xl p-6 transition-all hover:border-primary/40 hover:shadow-lg hover:shadow-primary/5 hover:-translate-y-0.5"
            >
              <div className="w-10 h-10 rounded-xl bg-primary/10 text-primary flex items-center justify-center mb-4 transition-colors group-hover:bg-primary/15">
                <BookOpen className="w-5 h-5" />
              </div>
              <div className="text-lg font-bold mb-1">{s.name}</div>
              {s.description && (
                <div className="text-sm text-text/55 mb-5 leading-relaxed">{s.description}</div>
              )}
              {s.visibility && (
                <div className="text-xs text-text/40 mb-4">
                  {s.visibility === 'GLOBAL' ? 'Público' : 'Privado'}
                </div>
              )}
              <Link
                className="btn btn-primary rounded-full px-5 py-2 text-sm shadow-sm shadow-primary/15 inline-flex items-center gap-2"
                to={ROUTES.syllabusSubjects(s.id)}
              >
                <Plus className="w-4 h-4" />
                Ver materias
              </Link>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
