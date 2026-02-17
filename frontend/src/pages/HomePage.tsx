import { Link } from 'react-router-dom';

export default function HomePage({ isAuthed, activeAttemptId }: { isAuthed: boolean; activeAttemptId?: string }) {
  return (
    <section className="mt-10">
      <div className="text-center max-w-3xl mx-auto">
        <h1 className="text-5xl sm:text-6xl font-extrabold leading-tight">
          Prepárate con <span className="bg-gradient-to-r from-red-500 to-yellow-400 bg-clip-text text-transparent">exámenes</span> reales
        </h1>
        <p className="text-text/70 mt-4">
          Practica con tests cronometrados, sigue tu progreso y mejora tus resultados.
        </p>
        <div className="mt-8 flex flex-col sm:flex-row gap-3 justify-center">
          {isAuthed && (
            <>
              <Link className="btn btn-primary rounded-full px-8 py-3 text-base" to="/subjects">
                Crear examen
              </Link>
              {activeAttemptId && (
                <Link className="btn btn-secondary rounded-full px-8 py-3 text-base" to={`/exams/attempts/${activeAttemptId}`}>
                  Reanudar examen
                </Link>
              )}
            </>
          )}
          {!isAuthed && (
            <>
              <Link className="btn btn-secondary rounded-full px-8 py-3 text-base" to="/register">Register</Link>
              <Link className="btn btn-primary rounded-full px-8 py-3 text-base" to="/login">Login</Link>
            </>
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
