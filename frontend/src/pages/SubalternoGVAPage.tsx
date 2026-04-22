import { useState } from 'react';
import { Link } from 'react-router-dom';
import { UserAdd, ArrowRightToBracket } from 'flowbite-react-icons/outline';
import { ROUTES } from '../constants/routes';

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

const ChevronRightIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-5 h-5 shrink-0">
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

const ALL_TOPICS = [
  { n: '01', title: 'La Constitución Española de 1978', desc: 'Principios rectores, derechos y deberes fundamentales.' },
  { n: '02', title: "L'Estatut d'Autonomia de la Comunitat Valenciana", desc: 'Les Corts, el President y el Consell.' },
  { n: '03', title: 'La Ley de la Función Pública Valenciana', desc: 'Personal al servicio de las administraciones públicas.' },
  { n: '04', title: 'Funciones de Subalterno', desc: 'Notificaciones, traslados, manejo de máquinas y vigilancia.' },
  { n: '05', title: 'La Generalitat Valenciana', desc: 'Organización, competencias y estructura orgánica.' },
  { n: '06', title: 'Régimen jurídico de las Administraciones Públicas', desc: 'Ley 39/2015 y Ley 40/2015 sobre el sector público.' },
  { n: '07', title: 'El procedimiento administrativo común', desc: 'Fases, plazos, recursos y actos administrativos.' },
  { n: '08', title: 'Prevención de riesgos laborales', desc: 'Obligaciones, medidas de protección y plan de emergencias.' },
  { n: '09', title: 'Protección de datos personales (RGPD)', desc: 'Reglamento europeo y Ley Orgánica 3/2018.' },
  { n: '10', title: 'Igualdad efectiva de mujeres y hombres', desc: 'Ley Orgánica 3/2007 y perspectiva de género en la administración.' },
  { n: '11', title: 'Medidas de seguridad y emergencias', desc: 'Planes de evacuación y actuación ante emergencias.' },
  { n: '12', title: 'Registro, correspondencia y archivo', desc: 'Gestión documental, registro de entrada y salida.' },
  { n: '13', title: 'Edificios e instalaciones de la GVA', desc: 'Distribución de espacios, accesos y control de llaves.' },
  { n: '14', title: 'Servicios de reprografía y mensajería', desc: 'Equipos de impresión, copias y distribución interna de documentos.' },
  { n: '15', title: 'Atención ciudadana y protocolo', desc: 'Comunicación, acogida y normas de conducta institucional.' },
  { n: '16', title: 'Informática básica de usuario', desc: 'Herramientas ofimáticas y correo electrónico institucional.' },
  { n: '17', title: "Valencià: nivell bàsic", desc: "Vocabulari i frases d'ús habitual a l'administració." },
  { n: '18', title: 'Traslado de materiales y equipos', desc: 'Técnicas de carga, manejo y transporte seguro.' },
  { n: '19', title: 'Vigilancia de accesos y espacios', desc: 'Control de entradas, identificación y rondas de revisión.' },
];

const INITIAL_VISIBLE = 4;

export default function SubalternoGVAPage() {
  const [showAll, setShowAll] = useState(false);
  const visibleTopics = showAll ? ALL_TOPICS : ALL_TOPICS.slice(0, INITIAL_VISIBLE);
  const remainingCount = ALL_TOPICS.length - INITIAL_VISIBLE;

  return (
    <div className="bg-surface text-on-surface min-h-screen">
      <main className="max-w-7xl mx-auto px-6 lg:px-8 pt-12 pb-32">

        {/* Hero */}
        <section className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center mb-24">
          <div className="lg:col-span-7 space-y-8">
            <div className="inline-flex items-center space-x-2 bg-primary/10 px-4 py-2 rounded-full">
              <span className="w-2 h-2 rounded-full bg-primary animate-pulse" />
              <span className="text-primary font-semibold text-xs tracking-widest uppercase">Actualizado 2026</span>
            </div>
            <h1 className="text-5xl md:text-6xl lg:text-7xl font-extrabold tracking-tighter leading-tight">
              Temario{' '}
              <span className="text-primary italic">Subalterno GVA</span>
            </h1>
            <p className="text-xl text-on-surface-variant max-w-xl leading-relaxed">
              Domina las oposiciones de la Generalitat Valenciana con el material más completo y estructurado bajo nuestra filosofía de Aprendizaje Consciente.
            </p>
            <div className="flex flex-wrap items-center gap-6">
              <div className="flex flex-col">
                <span className="text-4xl font-bold text-on-surface">25€</span>
                <span className="text-sm text-on-surface-variant line-through decoration-primary/40">45€ Pago único</span>
              </div>
              <Link
                to={ROUTES.register}
                className="bg-gradient-to-br from-primary to-primary-container text-on-primary px-10 py-5 rounded-xl font-bold text-lg hover:opacity-90 transition-all active:scale-95 shadow-lg shadow-primary/10 inline-flex items-center gap-3"
              >
                <UserAdd className="w-5 h-5" />
                Comprar ahora
              </Link>
            </div>
          </div>

          <div className="lg:col-span-5 relative">
            <div className="aspect-[4/5] rounded-[3rem] overflow-hidden shadow-2xl transform lg:rotate-3">
              <img
                className="w-full h-full object-cover"
                src="https://lh3.googleusercontent.com/aida-public/AB6AXuApIE-f_XlFhUAfTJgP8mLERGL4K1xYY0iflEZCcQbFPhR4yJBn7bnpPbI2HYrFnfy2GWCcbR96-88lmb3cJkLo73VGzJwmPP4TlPsrM8Oaoa5x3OucTz0K7d2tlfzsPGyHUOic6-p_OYw7DIhLvDfuaiIdK7EXVzI28Mbg0fR9HXhGBFwmg0UXUNAbL3J3xkmExqvrLJETxPQkoyQSEk-14lLhkDXQn8t72q04aCiLltfBLbxqShj50Pzivh7uSMlcM2dkEk50VGA"
                alt="Material de estudio Subalterno GVA 2026"
              />
            </div>
            <div className="absolute -bottom-6 -left-6 bg-surface p-6 rounded-3xl shadow-[0_32px_48px_-12px_rgba(27,28,28,0.08)] border border-outline-variant/10 max-w-[200px]">
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
                <img
                  alt="Opositor"
                  className="w-10 h-10 rounded-full border-2 border-surface object-cover"
                  src="https://lh3.googleusercontent.com/aida-public/AB6AXuC9-vChmrg-6MqRDlZV5INvD6mLanj4-5lOxIofUayeiT8KlGesl-aqjIASjzcflPeigZcQ9JF1q_15_XEJOD6TL4fkPvpK9WntWZmeEXvziCVQH8Q8GmaZYi2x4s8ldf6N0h_1vUyzSDkI4pxLlmRsNEm533Qd5cki_w6BqxOE6vQEv8fUcnlb3kbJwH2H3gBah8mF7C-x2f5rvRIeFGk2RbNX198_SRMwS-z_Eb6A7rro_BjiWUf0HB2Bw8z5fMlBqc1Z4bhN4OE"
                />
                <img
                  alt="Opositor"
                  className="w-10 h-10 rounded-full border-2 border-surface object-cover"
                  src="https://lh3.googleusercontent.com/aida-public/AB6AXuBuiQ12PbPkOz1DoVT0GRCSpTzABGHZqisKYf7kkc4iJCR2WGBu5K3talGp93MfjZ-zTQlG0V3XclM8KtBbIH6mU-IO7r-M6FWDLukzi2ps0fL-zMa2a7owwgvwJxsM9nAPyGOkwXuxW-r-vO4EayvGCwpaR86SRUbmvP3o3ChHSoMuRER_ZqYpirM8kz7srTimJ0DEZ-FnRLnpNYNJiaBhd9TZMDucXHq0aq--m1y2ePa_A4c6RbrMqFju-AGBEcDTc_JJF404z6I"
                />
                <img
                  alt="Opositor"
                  className="w-10 h-10 rounded-full border-2 border-surface object-cover"
                  src="https://lh3.googleusercontent.com/aida-public/AB6AXuBr_Lf0It7p417MHr-sI8VCG_nP7S49oXoOWRKppJQU10EkKb4B0h5pBjLGAvMKR3OIENr2JBcADca3GYWrNF5tQcbOPS9iUNIYSuUv9fdAoKVL2CLtF4Nw_Tlpp8N1BA8RjkTN9tlg2YC6HGJtNKDQi6blDwwQx4I6w5cUXQRzpXeEmFPP08rNfpH6TPnkttzdf2P_mOcN_jc0TcZwraJEM1pkstCctGNKTniTC-BlUtBxOQzbsdZMrhx7lmPeMrKdJ725Rry3ic0"
                />
              </div>
              <span className="text-sm font-medium text-on-surface-variant">+450 estudiantes hoy</span>
            </div>
          </div>

          <div className="lg:col-span-8 space-y-4">
            {visibleTopics.map(topic => (
              <div
                key={topic.n}
                className="bg-surface-container-lowest p-6 rounded-2xl flex items-center justify-between group cursor-default hover:bg-surface-container-low transition-all"
              >
                <div className="flex items-center space-x-6">
                  <span className="text-2xl font-black text-outline-variant group-hover:text-primary transition-colors w-8 shrink-0">
                    {topic.n}
                  </span>
                  <div>
                    <h4 className="font-bold text-lg">{topic.title}</h4>
                    <p className="text-sm text-on-surface-variant">{topic.desc}</p>
                  </div>
                </div>
                <span className="text-outline-variant group-hover:text-primary transition-all ml-4">
                  <ChevronRightIcon />
                </span>
              </div>
            ))}

            {!showAll && (
              <button
                onClick={() => setShowAll(true)}
                className="w-full py-4 text-primary font-bold text-sm tracking-widest uppercase hover:underline"
              >
                Ver los {remainingCount} temas restantes
              </button>
            )}
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
                <span>Actualizaciones 2026</span>
              </div>
            </div>
            <div className="flex flex-wrap gap-4 justify-center">
              <Link
                to={ROUTES.register}
                className="bg-gradient-to-br from-primary to-primary-container text-on-primary px-10 py-4 rounded-xl font-bold text-lg hover:opacity-90 transition-all active:scale-95 shadow-lg shadow-primary/10 inline-flex items-center gap-3"
              >
                <UserAdd className="w-5 h-5" />
                Comprar ahora — 25€
              </Link>
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
    </div>
  );
}
