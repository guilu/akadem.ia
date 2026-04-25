import { useState } from 'react';
import { Link } from 'react-router-dom';
import { UserAdd, ArrowRightToBracket } from 'flowbite-react-icons/outline';
import { ROUTES } from '../constants/routes';
import PaymentModal from '../components/PaymentModal';

const BookStoriesIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-7 h-7">
    <path strokeLinecap="round" strokeLinejoin="round" d="M12 6.042A8.967 8.967 0 006 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987 8.987 0 016 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 016-2.292c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0018 18a8.967 8.967 0 00-6 2.292m0-14.25v14.25" />
  </svg>
);

const QuizIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-7 h-7">
    <path strokeLinecap="round" strokeLinejoin="round" d="M9.879 7.519c1.171-1.025 3.071-1.025 4.242 0 1.172 1.025 1.172 2.687 0 3.712-.203.179-.43.326-.67.442-.745.361-1.45.999-1.45 1.827v.75M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9 5.25h.008v.008H12v-.008z" />
  </svg>
);

const AccountTreeIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-7 h-7">
    <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 3v11.25A2.25 2.25 0 006 16.5h2.25M3.75 3h-1.5m1.5 0h16.5m0 0h1.5m-1.5 0v11.25A2.25 2.25 0 0118 16.5h-2.25m-7.5 0h7.5m-7.5 0l-1 3m8.5-3l1 3m0 0l.5 1.5m-.5-1.5h-9.5m0 0l-.5 1.5" />
  </svg>
);

const StarFilledIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5">
    <path fillRule="evenodd" d="M10.788 3.21c.448-1.077 1.976-1.077 2.424 0l2.082 5.006 5.404.434c1.164.093 1.636 1.545.749 2.305l-4.117 3.527 1.257 5.273c.271 1.136-.964 2.033-1.96 1.425L12 18.354 7.373 21.18c-.996.608-2.231-.29-1.96-1.425l1.257-5.273-4.117-3.527c-.887-.76-.415-2.212.749-2.305l5.404-.434 2.082-5.005z" clipRule="evenodd" />
  </svg>
);

const ChevronRightIcon = ({ className = 'w-5 h-5 shrink-0' }: { className?: string }) => (
  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className={className}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
  </svg>
);

const VerifiedShieldIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-10 h-10">
    <path fillRule="evenodd" d="M8.603 3.799A4.49 4.49 0 0112 2.25c1.357 0 2.573.6 3.397 1.549a4.49 4.49 0 013.498 1.307 4.491 4.491 0 011.307 3.497A4.49 4.49 0 0121.75 12a4.49 4.49 0 01-1.549 3.397 4.491 4.491 0 01-1.307 3.497 4.491 4.491 0 01-3.497 1.307A4.49 4.49 0 0112 21.75a4.49 4.49 0 01-3.397-1.549 4.491 4.491 0 01-3.497-1.307 4.491 4.491 0 01-1.307-3.497A4.49 4.49 0 012.25 12c0-1.357.6-2.573 1.549-3.397a4.49 4.49 0 011.307-3.497 4.49 4.49 0 013.497-1.307zm7.007 6.387a.75.75 0 10-1.22-.872l-3.236 4.53L9.53 12.22a.75.75 0 00-1.06 1.06l2.25 2.25a.75.75 0 001.14-.094l3.75-5.25z" clipRule="evenodd" />
  </svg>
);

const CheckAllIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-5 h-5">
    <path strokeLinecap="round" strokeLinejoin="round" d="M4.5 12.75l6 6 9-13.5" />
  </svg>
);

const CloudDownloadIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
    <path strokeLinecap="round" strokeLinejoin="round" d="M12 9.75v6.75m0 0l-3-3m3 3l3-3m-8.25 6a4.5 4.5 0 01-1.41-8.775 5.25 5.25 0 0110.233-2.33 3 3 0 013.758 3.848A3.752 3.752 0 0118 19.5H6.75z" />
  </svg>
);

const UpdateIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
    <path strokeLinecap="round" strokeLinejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99" />
  </svg>
);

const SUBJECTS = [
  {
    n: '01', title: 'Tema 1 - Constitución Española I',
    units: ['Princípios fundamentales y valores superiores', 'Derechos y libertades fundamentales', 'Garantías y suspensión de derechos', 'Reforma constitucional'],
  },
  {
    n: '02', title: 'Tema 2 - Constitución Española II',
    units: ['La Corona: funciones del Rey', 'Las Cortes Generales: composición y funciones', 'El Gobierno y la Administración', 'El Tribunal Constitucional y el Poder Judicial'],
  },
  {
    n: '03', title: 'Tema 3: El Estatuto de Autonomía de la Comunitat Valenciana',
    units: ['Estructura y contenido del Estatuto', 'Competencias de la Generalitat', 'Instituciones: Les Corts, el Consell, el Síndic', 'Organización territorial valenciana'],
  },
  {
    n: '04', title: 'Tema 4: La Ley del Consell (I)',
    units: ['El President de la Generalitat', 'El Consell: composición y funcionamiento', 'Las Consellerias: organización', 'Órganos superiores y directivos'],
  },
  {
    n: '05', title: 'Tema 5: La Ley del Consell (II)',
    units: ['Administración territorial de la Generalitat', 'Sector público instrumental', 'Relaciones interadministrativas', 'Potestad normativa del Consell'],
  },
  {
    n: '06', title: 'Tema 6: Igualdad, LGTBI y violencia sobre la mujer',
    units: ['Ley de Igualdad entre mujeres y hombres', 'Ley integral contra la violencia sobre la mujer', 'Ley de igualdad LGTBI de la Comunitat Valenciana', 'Planes de igualdad en la Administración'],
  },
  {
    n: '07', title: 'Tema 7: Transparencia y buen gobierno',
    units: ['Ley de Transparencia (19/2013)', 'Ley valenciana de transparencia (2/2015)', 'Derecho de acceso a la información pública', 'Publicidad activa y buen gobierno'],
  },
  {
    n: '08', title: 'Tema 8: Procedimiento administrativo común',
    units: ['Ley 39/2015: ámbito y principios', 'Los interesados: capacidad y representación', 'Fases del procedimiento administrativo', 'Plazos, cómputo de términos y silencio'],
  },
  {
    n: '09', title: 'Tema 9: Actos administrativos y recursos',
    units: ['Requisitos y eficacia del acto administrativo', 'Nulidad y anulabilidad', 'Recurso de alzada y reposición', 'Recurso extraordinario de revisión'],
  },
  {
    n: '10', title: 'Tema 10: Función pública valenciana',
    units: ['Ley de Función Pública Valenciana (4/2021)', 'Clases de personal al servicio de la Generalitat', 'Acceso al empleo público: sistemas selectivos', 'Situaciones administrativas y provisión de puestos'],
  },
  {
    n: '11', title: 'Tema 11: Condiciones de trabajo en la GVA',
    units: ['Jornada, horarios y permisos', 'Vacaciones y licencias', 'Régimen retributivo', 'Régimen disciplinario'],
  },
  {
    n: '12', title: 'Tema 12: Atención a la ciudadanía y registro',
    units: ['Derechos de los ciudadanos ante la Administración', 'Registro de entrada y salida', 'Quejas, sugerencias y reclamaciones', 'Atención presencial y telemática'],
  },
  {
    n: '13', title: 'Tema 13: Control de acceso y protección de datos',
    units: ['RGPD: principios y bases de legitimación', 'Derechos ARCO-POL del interesado', 'Control de acceso a edificios públicos', 'Videovigilancia y seguridad de datos'],
  },
  {
    n: '14', title: 'Tema 14: Prevención de riesgos laborales',
    units: ['Ley 31/1995 de PRL: conceptos básicos', 'Derechos y obligaciones de los trabajadores', 'Plan de prevención de la GVA', 'Riesgos específicos del puesto de subalterno'],
  },
  {
    n: '15', title: 'Tema 15: Seguridad digital en la GVA',
    units: ['Esquema Nacional de Seguridad (ENS)', 'Política de seguridad de la información de la GVA', 'Uso seguro de dispositivos y contraseñas', 'Amenazas, incidentes y protocolos de actuación'],
  },
];

const AVATARS = [
  { initials: 'ML', bg: 'bg-primary', text: 'text-on-primary' },
  { initials: 'CR', bg: 'bg-tertiary', text: 'text-on-tertiary' },
  { initials: 'PG', bg: 'bg-secondary', text: 'text-on-secondary' },
];

const GiftIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-4 h-4">
    <path strokeLinecap="round" strokeLinejoin="round" d="M21 11.25v8.25a1.5 1.5 0 01-1.5 1.5H5.25a1.5 1.5 0 01-1.5-1.5v-8.25M12 4.875A2.625 2.625 0 1 0 9.375 7.5H12m0-2.625V7.5m0-2.625A2.625 2.625 0 1 1 14.625 7.5H12m0 0V21m-8.625-9.75h18c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125h-18c-.621 0-1.125.504-1.125 1.125v1.5c0 .621.504 1.125 1.125 1.125z" />
  </svg>
);

const MailIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5 shrink-0 text-secondary">
    <path strokeLinecap="round" strokeLinejoin="round" d="M21.75 6.75v10.5a2.25 2.25 0 01-2.25 2.25h-15a2.25 2.25 0 01-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25m19.5 0v.243a2.25 2.25 0 01-1.07 1.916l-7.5 4.615a2.25 2.25 0 01-2.36 0L3.32 8.91a2.25 2.25 0 01-1.07-1.916V6.75" />
  </svg>
);

const DownloadIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-5 h-5">
    <path strokeLinecap="round" strokeLinejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3" />
  </svg>
);

export default function SubalternoGVAPage() {
  const [openIndex, setOpenIndex] = useState<number | null>(null);
  const [paymentOpen, setPaymentOpen] = useState(false);
  const [sampleEmail, setSampleEmail] = useState('');
  const [sampleSubmitted, setSampleSubmitted] = useState(false);

  return (
    <div className="bg-surface text-on-surface min-h-screen">
      <main className="max-w-7xl mx-auto px-6 lg:px-8 pt-12 pb-32">

        {/* Hero */}
        <section className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center mb-24">
          <div className="lg:col-span-8 space-y-8">
            <div className="inline-flex items-center space-x-2 bg-primary/10 px-4 py-2 rounded-full">
              <span className="w-2 h-2 rounded-full bg-primary animate-pulse" />
              <span className="text-primary font-semibold text-xs tracking-widest uppercase">Actualizado {new Date().getFullYear()}</span>
            </div>
            <h1 className="text-5xl md:text-6xl lg:text-7xl font-extrabold tracking-tighter leading-tight">
              Temario<br />
              <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent italic">Subalterno GVA</span>
            </h1>
            <p className="text-xl text-on-surface-variant max-w-xl leading-relaxed">
              Domina las oposiciones de la Generalitat Valenciana con el material más completo y estructurado bajo nuestra filosofía de Aprendizaje Consciente.
            </p>
            <div className="flex flex-wrap items-center justify-center lg:justify-start gap-6">
              <div className="flex flex-col">
                <div className="flex items-baseline gap-2">
                  <span className="text-4xl font-bold text-primary">15€</span>
                  <span className="text-base text-on-surface-variant line-through">39€</span>
                </div>
                <span className="text-sm text-on-surface-variant">Pago único · Sin suscripción</span>
              </div>
              <div className="flex flex-col items-start gap-2">
                <button
                  onClick={() => setPaymentOpen(true)}
                  className="btn btn-primary px-10 py-5 rounded-xl font-bold text-lg shadow-lg shadow-primary/10 inline-flex items-center gap-3"
                >
                  <UserAdd className="w-5 h-5" />
                  Comprar ahora
                </button>
                <span className="text-xs text-on-surface-variant/70 pl-1">Pago seguro con Stripe · Acceso inmediato</span>
              </div>
            </div>
          </div>

          <div className="lg:col-span-4 relative">
            <div className="aspect-[4/5] rounded-[3rem] overflow-hidden shadow-2xl transform lg:rotate-3">
              <img
                className="w-full h-full object-cover dark:hidden"
                src="/assets/subalterno-gva-light.png"
                alt="Material de estudio Subalterno GVA 2026"
              />
              <img
                className="w-full h-full object-cover hidden dark:block"
                src="/assets/subalterno-gva-dark.png"
                alt="Material de estudio Subalterno GVA 2026"
              />
            </div>
            <div className="absolute -bottom-6 -left-6 bg-card p-6 rounded-3xl shadow-[0_32px_48px_-12px_rgba(27,28,28,0.08)] border border-outline-variant/10 max-w-[200px]">
              <div className="flex items-center space-x-3 mb-2">
                <span className="text-tertiary"><StarFilledIcon /></span>
                <span className="font-bold">4.9/5</span>
              </div>
              <p className="text-xs text-on-surface-variant leading-snug">
                Calificación media de más de 2.000 opositores este año.
              </p>
            </div>
          </div>
        </section>

        {/* Features Bento Grid */}
        <section className="mb-32">
          <h2 className="text-3xl font-bold mb-12 tracking-tight">Qué incluye tu preparación</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            <div className="bg-surface-container-low p-10 rounded-[2.5rem] flex flex-col justify-between group hover:bg-surface-container-high transition-colors">
              <div className="bg-surface-container-highest w-14 h-14 rounded-2xl flex items-center justify-center mb-6">
                <span className="text-primary"><BookStoriesIcon /></span>
              </div>
              <div>
                <h3 className="text-xl font-bold mb-3">Temas actualizados</h3>
                <p className="text-on-surface-variant leading-relaxed">
                  Contenido revisado semanalmente según las últimas publicaciones del DOGV.
                </p>
              </div>
            </div>

            <div className="bg-primary/5 p-10 rounded-[2.5rem] flex flex-col justify-between border border-primary/10">
              <div className="bg-primary text-on-primary w-14 h-14 rounded-2xl flex items-center justify-center mb-6">
                <QuizIcon />
              </div>
              <div>
                <h3 className="text-xl font-bold mb-3">Test ilimitados</h3>
                <p className="text-on-surface-variant leading-relaxed">
                  Generador inteligente de exámenes por temas o simulacros reales cronometrados.
                </p>
              </div>
            </div>

            <div className="bg-surface-container-low p-10 rounded-[2.5rem] flex flex-col justify-between group hover:bg-surface-container-high transition-colors">
              <div className="bg-surface-container-highest w-14 h-14 rounded-2xl flex items-center justify-center mb-6">
                <span className="text-primary"><AccountTreeIcon /></span>
              </div>
              <div>
                <h3 className="text-xl font-bold mb-3">Esquemas Visuales</h3>
                <p className="text-on-surface-variant leading-relaxed">
                  Mapas conceptuales de alta densidad para memorizar jerarquías administrativas.
                </p>
              </div>
            </div>
          </div>
        </section>

        {/* Syllabus List */}
        <section className="mb-32 grid grid-cols-1 lg:grid-cols-12 gap-16">
          <div className="lg:col-span-4">
            <h2 className="text-4xl font-bold tracking-tight mb-6">Temario completo</h2>
            <p className="text-on-surface-variant text-lg leading-relaxed">
              Estructurado en Bloque General y Bloque Específico, diseñado para una lectura ágil y enfocada.
            </p>
            <div className="mt-8 flex items-center space-x-4">
              <div className="flex -space-x-3">
                {AVATARS.map((a) => (
                  <div
                    key={a.initials}
                    className={`w-10 h-10 rounded-full border-2 border-card ${a.bg} ${a.text} flex items-center justify-center text-xs font-bold shrink-0`}
                  >
                    {a.initials}
                  </div>
                ))}
              </div>
              <span className="text-sm font-medium text-on-surface-variant">+450 estudiantes hoy</span>
            </div>
          </div>

          <div className="lg:col-span-8 space-y-3">
            {SUBJECTS.map((subject, i) => (
              <div key={subject.n} className="bg-surface-container-lowest rounded-2xl overflow-hidden">
                <button
                  onClick={() => setOpenIndex(openIndex === i ? null : i)}
                  className="w-full p-6 flex items-center justify-between hover:bg-surface-container-low transition-all text-left"
                >
                  <div className="flex items-center space-x-6">
                    <span className={`text-2xl font-black transition-colors w-8 shrink-0 ${openIndex === i ? 'text-primary' : 'text-outline-variant'}`}>
                      {subject.n}
                    </span>
                    <h4 className="font-bold text-lg">{subject.title}</h4>
                  </div>
                  <ChevronRightIcon
                    className={`w-5 h-5 shrink-0 ml-4 transition-transform ${openIndex === i ? 'rotate-90 text-primary' : 'text-outline-variant'}`}
                  />
                </button>
                {openIndex === i && (
                  <div className="px-6 pb-6 border-t border-outline-variant/10">
                    <ul className="mt-4 space-y-2">
                      {subject.units.map((unit) => (
                        <li key={unit} className="flex items-start gap-3 text-sm text-on-surface-variant">
                          <span className="text-primary mt-0.5 shrink-0">•</span>
                          {unit}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            ))}
          </div>
        </section>

        {/* Free Sample Section */}
        <section className="mb-32 bg-card border border-secondary/20 rounded-[3rem] p-12 lg:p-16 relative overflow-hidden">
          <div className="pointer-events-none absolute -top-20 -right-20 w-72 h-72 rounded-full bg-primary/5 blur-3xl" />
          <div className="pointer-events-none absolute -bottom-20 -left-20 w-72 h-72 rounded-full bg-accent/5 blur-3xl" />
          <div className="relative max-w-xl">
            <div className="inline-flex items-center gap-2 bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400 px-4 py-2 rounded-full text-sm font-bold mb-8">
              <GiftIcon />
              Gratis
            </div>

            <h2 className="text-4xl md:text-5xl font-extrabold tracking-tighter mb-6 text-text">
              No te fíes.{' '}
              <em className="bg-gradient-to-r from-primary to-accent bg-clip-text text-transparent not-italic">Léelo</em>{' '}
              y decide.
            </h2>

            <p className="text-text/65 text-lg leading-relaxed mb-4">
              El Tema 1 del temario de Subalterno GVA es tuyo gratis. La Constitución Española. Sin correo comercial, sin trampa.
            </p>
            <p className="text-text/65 text-lg leading-relaxed mb-8">
              Si el material te convence, ya sabes dónde está el resto. Si no, no compres. Así de sencillo.
            </p>

            <ul className="space-y-3 mb-10">
              {[
                'Tema 1 completo en PDF',
                'Sin compromiso de compra',
              ].map(item => (
                <li key={item} className="flex items-center gap-3 text-sm font-semibold text-text/80">
                  <span className="text-green-500 shrink-0"><CheckAllIcon /></span>
                  {item}
                </li>
              ))}
            </ul>

            {sampleSubmitted ? (
              <div className="flex items-center gap-3 bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300 px-6 py-4 rounded-2xl text-sm font-semibold">
                <span className="text-green-500"><CheckAllIcon /></span>
                ¡Revisa tu correo! Te hemos enviado el Tema 1.
              </div>
            ) : (
              <form
                onSubmit={e => { e.preventDefault(); setSampleSubmitted(true); }}
                className="flex flex-col sm:flex-row gap-3"
              >
                <div className="flex items-center gap-3 flex-1 bg-bg border border-secondary/30 rounded-2xl px-4 py-3 focus-within:border-primary/50 transition-colors">
                  <MailIcon />
                  <input
                    type="email"
                    required
                    value={sampleEmail}
                    onChange={e => setSampleEmail(e.target.value)}
                    placeholder="Tu email"
                    className="flex-1 bg-transparent text-text placeholder:text-secondary text-sm outline-none"
                  />
                </div>
                <button
                  type="submit"
                  className="btn btn-primary rounded-2xl px-6 py-3 font-bold text-sm inline-flex items-center justify-center gap-2 whitespace-nowrap shadow-lg shadow-primary/20"
                >
                  <DownloadIcon />
                  Empezar gratis
                </button>
              </form>
            )}

            <p className="text-xs text-secondary mt-4">
              Te enviamos el PDF y punto. Sin spam, sin secuencia de 47 emails.
            </p>
          </div>
        </section>

        {/* Trust Section */}
        <section className="bg-secondary-container/30 rounded-[3rem] p-12 lg:p-20 text-center relative overflow-hidden">
          <div className="relative z-10 max-w-3xl mx-auto">
            <div className="w-20 h-20 bg-surface rounded-full flex items-center justify-center mx-auto mb-8 shadow-xl shadow-secondary/5">
              <span className="text-primary"><VerifiedShieldIcon /></span>
            </div>
            <h2 className="text-3xl md:text-4xl font-bold mb-6">Garantía de Satisfacción 100%</h2>
            <p className="text-lg text-on-surface-variant mb-10 leading-relaxed">
              Si el contenido no se adapta a tus necesidades o hay un cambio legislativo importante en los primeros 15 días,
              actualizamos tu material gratis o te devolvemos tu inversión. Sin preguntas.
            </p>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-12">
              <div className="flex items-center justify-center space-x-3 text-sm font-semibold">
                <span className="text-primary"><CheckAllIcon /></span>
                <span>Pago seguro SSL</span>
              </div>
              <div className="flex items-center justify-center space-x-3 text-sm font-semibold">
                <span className="text-primary"><CloudDownloadIcon /></span>
                <span>Descarga inmediata</span>
              </div>
              <div className="flex items-center justify-center space-x-3 text-sm font-semibold">
                <span className="text-primary"><UpdateIcon /></span>
                <span>Actualizaciones {new Date().getFullYear()}</span>
              </div>
            </div>
            <div className="flex flex-wrap gap-4 justify-center">
              <button
                onClick={() => setPaymentOpen(true)}
                className="btn btn-primary px-10 py-4 rounded-xl font-bold text-lg shadow-lg shadow-primary/10 inline-flex items-center gap-3"
              >
                <UserAdd className="w-5 h-5" />
                Comprar ahora — 15€
              </button>
              <Link
                to={ROUTES.login}
                className="border border-outline-variant px-10 py-4 rounded-xl font-bold text-lg hover:bg-surface-container-low transition-all inline-flex items-center gap-3 text-on-surface"
              >
                <ArrowRightToBracket className="w-5 h-5" />
                Ya tengo cuenta
              </Link>
            </div>
          </div>
          <div className="absolute -top-24 -right-24 w-64 h-64 bg-primary/5 rounded-full blur-3xl" />
          <div className="absolute -bottom-24 -left-24 w-64 h-64 bg-tertiary/5 rounded-full blur-3xl" />
        </section>
      </main>
      <PaymentModal isOpen={paymentOpen} onClose={() => setPaymentOpen(false)} />
    </div>
  );
}
