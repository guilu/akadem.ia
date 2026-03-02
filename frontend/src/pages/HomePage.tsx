import { Link } from 'react-router-dom';
import { Play, ArrowsRepeat, ArrowRightToBracket, UserAdd, BookOpen } from 'flowbite-react-icons/outline';
import { ROUTES } from '../constants/routes';

const TimerIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
  </svg>
);

const ChartIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
  </svg>
);

const features = [
  {
    icon: <TimerIcon />,
    title: 'Simulacros cronometrados',
    desc: 'Reproduce las condiciones exactas del examen. Entrena la presión del tiempo real antes del gran día.',
  },
  {
    icon: <ChartIcon />,
    title: 'Seguimiento de progreso',
    desc: 'Nota, tiempo y evolución guardados automáticamente. Observa tu mejora semana a semana.',
  },
  {
    icon: <ArrowsRepeat className="w-6 h-6" />,
    title: 'Reanuda cuando quieras',
    desc: 'Empieza hoy, continúa mañana. Tu progreso se guarda en cada sesión sin perder nada.',
  },
  {
    icon: <BookOpen className="w-6 h-6" />,
    title: 'Flashcards inteligentes',
    desc: 'Repasa conceptos clave con tarjetas organizadas por tema para fijar el conocimiento a largo plazo.',
  },
];

const steps = [
  {
    n: '01',
    title: 'Regístrate gratis',
    desc: 'Crea tu cuenta en segundos y accede a todos los temas de la Constitución española.',
  },
  {
    n: '02',
    title: 'Elige tu examen',
    desc: 'Selecciona temas, número de preguntas y dificultad. Tú controlas el entrenamiento.',
  },
  {
    n: '03',
    title: 'Analiza y mejora',
    desc: 'Revisa tus resultados, identifica puntos débiles y vuelve a intentarlo hasta dominarlo.',
  },
];

const stats = [
  { value: '169', label: 'Artículos cubiertos' },
  { value: '500+', label: 'Preguntas disponibles' },
  { value: '3', label: 'Niveles de dificultad' },
];

export default function HomePage({ isAuthed, activeAttemptId }: { isAuthed: boolean; activeAttemptId?: string }) {
  return (
    <div>
      {/* ── Hero ── */}
      <section className="relative overflow-hidden px-6 py-24 sm:py-32">
        <div className="pointer-events-none absolute inset-0 bg-gradient-to-br from-primary/10 via-transparent to-accent/15" />
        <div className="pointer-events-none absolute -top-32 left-1/2 -translate-x-1/2 w-[600px] h-[600px] rounded-full bg-primary/5 blur-3xl" />
        <div className="relative max-w-6xl mx-auto flex flex-col lg:flex-row items-center gap-12 lg:gap-16">

          {/* Text */}
          <div className="flex-1 text-center lg:text-left">
            <div className="inline-flex items-center gap-2 mb-8 px-4 py-2 rounded-full border border-primary/30 bg-primary/10 text-primary text-sm font-semibold tracking-wide">
              <span aria-hidden>★</span>
              <span>Preparación inteligente para opositores</span>
            </div>

            <h1 className="text-5xl sm:text-6xl lg:text-7xl font-extrabold leading-[1.1] mb-6 tracking-tight">
              Aprueba la{' '}
              <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">
                Constitución
              </span>
              <br />sin improvisar
            </h1>

            <p className="text-lg sm:text-xl text-text/65 mb-10 max-w-xl mx-auto lg:mx-0 leading-relaxed">
              Simulacros cronometrados, flashcards y análisis de progreso, todo en una plataforma diseñada para convertir tu esfuerzo en resultados.
            </p>

            <div className="flex flex-wrap gap-3 justify-center lg:justify-start">
              {isAuthed ? (
                <>
                  <Link
                    className="btn btn-primary rounded-full px-8 py-3 text-base shadow-lg shadow-primary/20 flex items-center gap-2"
                    to={ROUTES.subjects}
                  >
                    <Play className="w-5 h-5" />
                    Crear examen
                  </Link>
                  <Link
                    className="btn btn-secondary rounded-full px-8 py-3 text-base flex items-center gap-2"
                    to={ROUTES.flashcards}
                  >
                    <BookOpen className="w-5 h-5" />
                    Estudiar flashcards
                  </Link>
                  {activeAttemptId && (
                    <Link
                      className="btn btn-outline rounded-full px-8 py-3 text-base flex items-center gap-2"
                      to={ROUTES.examAttempt(activeAttemptId)}
                    >
                      <ArrowsRepeat className="w-5 h-5" />
                      Reanudar examen
                    </Link>
                  )}
                </>
              ) : (
                <>
                  <Link
                    className="btn btn-primary rounded-full px-8 py-3 text-base shadow-lg shadow-primary/20 flex items-center gap-2"
                    to={ROUTES.register}
                  >
                    <UserAdd className="w-5 h-5" />
                    Empieza gratis
                  </Link>
                  <Link
                    className="btn btn-outline rounded-full px-8 py-3 text-base flex items-center gap-2"
                    to={ROUTES.login}
                  >
                    <ArrowRightToBracket className="w-5 h-5" />
                    Ya tengo cuenta
                  </Link>
                </>
              )}
            </div>
          </div>

          {/* Image — desktop only */}
          <div className="hidden lg:flex flex-1 justify-end">
            <img
              src="/assets/landing/constitution-books.png"
              alt="Libros de la Constitución española"
              className="w-full max-w-2xl drop-shadow-2xl"
            />
          </div>

        </div>
      </section>

      {/* ── Stats strip ── */}
      <section className="border-y border-secondary/20 bg-secondary/5 py-12 px-6">
        <dl className="max-w-2xl mx-auto grid grid-cols-3 gap-6 text-center">
          {stats.map(({ value, label }) => (
            <div key={label}>
              <dt className="text-4xl font-extrabold text-primary">{value}</dt>
              <dd className="mt-1 text-sm text-text/55 font-medium">{label}</dd>
            </div>
          ))}
        </dl>
      </section>

      {/* ── Features ── */}
      <section className="px-6 py-24">
        <div className="max-w-4xl mx-auto">
          <h2 className="text-3xl sm:text-4xl font-extrabold text-center mb-3 tracking-tight">
            Todo lo que necesitas para aprobar
          </h2>
          <p className="text-center text-text/55 mb-14 text-lg">Sin distracciones. Sin excusas.</p>

          <div className="grid sm:grid-cols-2 gap-5">
            {features.map(({ icon, title, desc }) => (
              <article
                key={title}
                className="group border border-secondary/25 rounded-2xl p-7 transition-all hover:border-primary/40 hover:shadow-lg hover:shadow-primary/5 hover:-translate-y-0.5"
              >
                <div className="w-12 h-12 rounded-xl bg-primary/10 text-primary flex items-center justify-center mb-5 transition-colors group-hover:bg-primary/15">
                  {icon}
                </div>
                <h3 className="text-lg font-bold mb-2">{title}</h3>
                <p className="text-sm text-text/55 leading-relaxed">{desc}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      {/* ── How it works ── */}
      <section className="border-y border-secondary/20 bg-secondary/5 px-6 py-24">
        <div className="max-w-2xl mx-auto">
          <h2 className="text-3xl sm:text-4xl font-extrabold text-center mb-3 tracking-tight">
            Cómo funciona
          </h2>
          <p className="text-center text-text/55 mb-14 text-lg">Tres pasos para empezar a prepararte hoy.</p>

          <ol className="space-y-10">
            {steps.map(({ n, title, desc }) => (
              <li key={n} className="flex gap-6 items-start">
                <span
                  aria-hidden
                  className="shrink-0 w-14 text-5xl font-extrabold text-primary/20 tabular-nums leading-none select-none"
                >
                  {n}
                </span>
                <div className="pt-1">
                  <h3 className="text-lg font-bold mb-1">{title}</h3>
                  <p className="text-sm text-text/55 leading-relaxed">{desc}</p>
                </div>
              </li>
            ))}
          </ol>
        </div>
      </section>

      {/* ── Final CTA (only for guests) ── */}
      {!isAuthed && (
        <section className="px-6 py-28 text-center">
          <div className="max-w-xl mx-auto">
            <h2 className="text-3xl sm:text-4xl font-extrabold mb-4 tracking-tight">
              ¿Listo para aprobar?
            </h2>
            <p className="text-text/55 mb-10 text-lg">
              Únete hoy y empieza a entrenar con exámenes reales. Sin tarjeta, sin compromiso.
            </p>
            <Link
              className="btn btn-primary rounded-full px-10 py-4 text-lg inline-flex items-center gap-2 shadow-xl shadow-primary/25"
              to={ROUTES.register}
            >
              <UserAdd className="w-5 h-5" />
              Crear mi cuenta gratis
            </Link>
          </div>
        </section>
      )}
    </div>
  );
}
