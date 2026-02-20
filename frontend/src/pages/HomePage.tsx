import { Link } from 'react-router-dom';

export default function HomePage({ isAuthed, activeAttemptId }: { isAuthed: boolean; activeAttemptId?: string }) {
  return (
    <section className="mt-6">
      <div className="grid max-w-screen-2xl px-4 py-8 mx-auto lg:gap-8 xl:gap-0 lg:py-16 lg:grid-cols-12">
        <div className="mr-auto place-self-center lg:col-span-5 text-center lg:text-left">
          <h1 className="max-w-2xl mb-4 text-5xl sm:text-6xl font-extrabold leading-tight">
            Aprueba la <span className="bg-gradient-to-r from-red-500 to-yellow-400 bg-clip-text text-transparent">Constitución</span> sin improvisar.
          </h1>
          <p className="max-w-2xl mb-6 text-text/70 md:text-lg lg:text-xl">
            Entrena con exámenes reales, controla el tiempo y convierte cada intento en puntos el día de la oposición.
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
        <div className="hidden lg:mt-0 lg:col-span-7 lg:flex lg:justify-end">
          <img
            src="/assets/landing/constitution-books.png"
            alt="Constitución española"
            className="w-[110%] max-w-none h-auto rounded-2xl object-cover"
          />
        </div>
      </div>

      <div className="mt-6 grid gap-4 sm:grid-cols-3 max-w-screen-xl mx-auto px-4">
        <div className="border border-gray-400 rounded-xl p-4">
          <div className="text-2xl font-bold">Simulacros cronometrados</div>
          <div className="text-sm text-text/70">Entrena exactamente como el día del examen.</div>
        </div>
        <div className="border border-gray-400 rounded-xl p-4">
          <div className="text-2xl font-bold">Resultados que importan</div>
          <div className="text-sm text-text/70">Nota, tiempo y evolución guardados automáticamente.</div>
        </div>
        <div className="border border-gray-400 rounded-xl p-4">
          <div className="text-2xl font-bold">Reanuda cuando quieras</div>
          <div className="text-sm text-text/70">Empieza hoy. Continúa mañana. Sin perder nada.</div>
        </div>
      </div>
    </section>
  );
}
