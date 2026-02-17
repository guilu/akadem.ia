import { Link } from 'react-router-dom';

export default function HomePage({ isAuthed, activeAttemptId }: { isAuthed: boolean; activeAttemptId?: string }) {
  return (
    <section className="mt-6">
      <div className="grid max-w-screen-xl px-4 py-8 mx-auto lg:gap-8 xl:gap-0 lg:py-16 lg:grid-cols-12">
        <div className="mr-auto place-self-center lg:col-span-6 text-center lg:text-left">
          <h1 className="max-w-2xl mb-4 text-5xl sm:text-6xl font-extrabold leading-tight">
            Prepárate con <span className="bg-gradient-to-r from-red-500 to-yellow-400 bg-clip-text text-transparent">exámenes</span> reales
          </h1>
          <p className="max-w-2xl mb-6 text-text/70 md:text-lg lg:text-xl">
            Practica con tests cronometrados, sigue tu progreso y mejora tus resultados.
          </p>
          <div className="flex flex-col sm:flex-row gap-3 justify-center lg:justify-start">
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
                <Link className="btn btn-primary rounded-full px-8 py-3 text-base" to="/login">Login</Link>
                <Link className="btn btn-secondary rounded-full px-8 py-3 text-base" to="/register">Register</Link>
              </>
            )}
          </div>
        </div>
        <div className="hidden lg:mt-0 lg:col-span-6 lg:flex">
          <img
            src="/assets/landing/constitution-books.png"
            alt="Constitución española"
            className="w-full h-auto rounded-2xl object-cover"
          />
        </div>
      </div>

      <div className="mt-6 grid gap-4 sm:grid-cols-3 max-w-screen-xl mx-auto px-4">
        <div className="border border-gray-400 rounded-xl p-4">
          <div className="text-2xl font-bold">+200</div>
          <div className="text-sm text-text/70">Preguntas por materia</div>
        </div>
        <div className="border border-gray-400 rounded-xl p-4">
          <div className="text-2xl font-bold">Exámenes</div>
          <div className="text-sm text-text/70">Con tiempo real</div>
        </div>
        <div className="border border-gray-400 rounded-xl p-4">
          <div className="text-2xl font-bold">Progreso</div>
          <div className="text-sm text-text/70">Resultados inmediatos</div>
        </div>
      </div>
    </section>
  );
}
