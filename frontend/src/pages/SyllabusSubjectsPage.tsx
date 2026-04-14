import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { BookOpen, Plus } from 'flowbite-react-icons/outline';
import { getSubjectsInSyllabus } from '../api/syllabusApi';
import type { Subject } from '../types';
import { ROUTES } from '../constants/routes';

export default function SyllabusSubjectsPage() {
  const { syllabusId } = useParams<{ syllabusId: string }>();
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!syllabusId) return;
    setLoading(true);
    setError('');
    getSubjectsInSyllabus(syllabusId)
      .then(setSubjects)
      .catch(() => setError('No se pudieron cargar las materias.'))
      .finally(() => setLoading(false));
  }, [syllabusId]);

  return (
    <section className="mb-8">
      <div className="py-[1.5rem] mb-6">
        <Link
          to={ROUTES.syllabuses}
          className="text-sm text-text/55 hover:text-text transition-colors mb-2 inline-block"
        >
          ← Volver a temarios
        </Link>
        <h1 className="text-3xl font-extrabold tracking-tight">
          Elige tu{' '}
          <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">
            materia
          </span>
        </h1>
        <p className="text-text/55 text-sm mt-1">Selecciona una materia para configurar tu examen.</p>
      </div>

      {loading && (
        <div className="text-sm text-text/55">Cargando materias...</div>
      )}

      {!loading && error && (
        <div className="rounded-xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm text-red-400">{error}</div>
      )}

      {!loading && !error && subjects.length === 0 && (
        <div className="border border-secondary/25 rounded-2xl px-5 py-10 text-center space-y-4">
          <div className="w-12 h-12 rounded-2xl bg-primary/10 text-primary flex items-center justify-center mx-auto">
            <BookOpen className="w-6 h-6" />
          </div>
          <div>
            <p className="font-bold text-base mb-1">No hay materias en este temario</p>
            <p className="text-sm text-text/55">Aún no hay materias. Añade materias desde el panel de gestión.</p>
          </div>
          <Link
            to={ROUTES.manage}
            className="btn btn-primary rounded-full px-6 py-2 text-sm shadow-sm shadow-primary/15 inline-flex items-center gap-2"
          >
            <Plus className="w-4 h-4" />
            Ir a Gestionar
          </Link>
        </div>
      )}

      {!loading && !error && subjects.length > 0 && (
        <div className="grid sm:grid-cols-2 gap-4">
          {subjects.map((s) => (
            <article
              key={s.id}
              className="group border border-secondary/25 rounded-2xl bg-card p-6 transition-all hover:border-primary/40 hover:shadow-lg hover:shadow-primary/5 hover:-translate-y-0.5"
            >
              <div className="w-10 h-10 rounded-xl bg-primary/10 text-primary flex items-center justify-center mb-4 transition-colors group-hover:bg-primary/15">
                <Plus className="w-5 h-5" />
              </div>
              <div className="text-lg font-bold mb-1">{s.name}</div>
              {s.description && (
                <div className="text-sm text-text/55 mb-5 leading-relaxed">{s.description}</div>
              )}
              <Link
                className="btn btn-primary rounded-full px-5 py-2 text-sm shadow-sm shadow-primary/15 inline-flex items-center gap-2"
                to={ROUTES.subjectBuilder(s.id)}
              >
                <Plus className="w-4 h-4" />
                Crear examen
              </Link>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
