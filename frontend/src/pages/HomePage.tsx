import { Link } from 'react-router-dom';

export default function HomePage({ isAuthed, activeAttemptId }: { isAuthed: boolean; activeAttemptId?: string }) {
  return (
    <section className="mt-6">
      <div className="text-center">
        <h1 className="text-4xl font-bold mb-3">Prepárate con exámenes reales</h1>
        <p className="text-text/70 max-w-2xl mx-auto">
          Practica con tests cronometrados, sigue tu progreso y mejora tus resultados.
        </p>
        <div className="mt-6 flex flex-col sm:flex-row gap-3 justify-center">
          {isAuthed && (
            <>
              <Link className="btn btn-primary" to="/subjects">
                Crear examen
              </Link>
              {activeAttemptId && (
                <Link className="btn btn-secondary" to={`/exams/attempts/${activeAttemptId}`}>
                  Reanudar examen
                </Link>
              )}
            </>
          )}
          {!isAuthed && (
            <div className="flex gap-2 justify-center">
              <Link className="px-3 py-2 rounded-full border border-slate-600 text-sm" to="/login">Login</Link>
              <Link className="px-3 py-2 rounded-full border border-slate-600 text-sm" to="/register">Register</Link>
            </div>
          )}
        </div>
      </div>

      <div className="mt-10 grid gap-4 sm:grid-cols-3">
        <div className="border border-slate-700 rounded-xl p-4">
          <div className="text-2xl font-bold">+200</div>
          <div className="text-sm text-text/70">Preguntas por materia</div>
        </div>
        <div className="border border-slate-700 rounded-xl p-4">
          <div className="text-2xl font-bold">Exámenes</div>
          <div className="text-sm text-text/70">Con tiempo real</div>
        </div>
        <div className="border border-slate-700 rounded-xl p-4">
          <div className="text-2xl font-bold">Progreso</div>
          <div className="text-sm text-text/70">Resultados inmediatos</div>
        </div>
      </div>
    </section>
  );
}
