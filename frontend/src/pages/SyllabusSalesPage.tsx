import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { BookOpen, ArrowsRepeat, UserAdd, ArrowRightToBracket } from 'flowbite-react-icons/outline';
import { getSyllabuses } from '../api/syllabusApi';
import type { Syllabus } from '../types';
import { ROUTES } from '../constants/routes';

const CheckIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
  </svg>
);

const ShieldIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
  </svg>
);

const UsersIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
  </svg>
);

const FEATURES = [
  { icon: <BookOpen className="w-5 h-5" />, label: 'Temas completos y revisados' },
  { icon: <ArrowsRepeat className="w-5 h-5" />, label: `Actualizado ${new Date().getFullYear()}` },
  { icon: <CheckIcon />, label: 'Acceso inmediato al material' },
];

const TRUST_ITEMS = [
  {
    icon: <ShieldIcon />,
    title: 'Garantía de Actualización',
    desc: 'Si el temario cambia en 6 meses, te enviamos la actualización gratis.',
  },
  {
    icon: <UsersIcon />,
    title: 'Comunidad de Confianza',
    desc: 'Más de 2.000 opositores ya confían en nuestro método.',
  },
];

const TAGS = ['#Oposiciones2024', '#ExitoAcademico', '#TemariosPremium', '#EstudioZen', '#GarantiaTotal'];

export default function SyllabusSalesPage() {
  const [syllabus, setSyllabus] = useState<Syllabus | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getSyllabuses()
      .then(list => setSyllabus(list[0] ?? null))
      .catch(() => setSyllabus(null))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div>
      {/* Hero */}
      <section className="relative overflow-hidden px-6 py-16 text-center">
        <div className="pointer-events-none absolute inset-0 bg-gradient-to-br from-primary/10 via-transparent to-accent/15" />
        <div className="pointer-events-none absolute -top-32 left-1/2 -translate-x-1/2 w-[600px] h-[600px] rounded-full bg-primary/5 blur-3xl" />
        <div className="relative max-w-3xl mx-auto">
          <div className="inline-flex items-center gap-2 mb-8 px-4 py-2 rounded-full border border-primary/30 bg-primary/10 text-primary text-sm font-semibold tracking-wide">
            <span aria-hidden>★</span>
            <span>Material oficial para opositores</span>
          </div>
          <h1 className="text-5xl sm:text-6xl font-extrabold tracking-tight mb-4">
            Consigue tu{' '}
            <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">
              Temario
            </span>
          </h1>
          <p className="text-text/65 text-lg leading-relaxed max-w-2xl mx-auto">
            Material de estudio de alta calidad, diseñado por expertos para maximizar tu rendimiento en el examen.
          </p>
        </div>
      </section>

      {/* Product card */}
      <section className="px-6 py-16">
        <div className="max-w-lg mx-auto">
          {loading && (
            <div className="border border-secondary/25 rounded-3xl bg-card p-10 animate-pulse">
              <div className="h-12 w-12 rounded-2xl bg-secondary/20 mb-6" />
              <div className="h-7 w-2/3 bg-secondary/20 rounded mb-3" />
              <div className="h-4 w-full bg-secondary/10 rounded mb-1" />
              <div className="h-4 w-4/5 bg-secondary/10 rounded mb-8" />
              <div className="space-y-3 mb-8">
                {[1, 2, 3].map(i => <div key={i} className="h-4 w-3/4 bg-secondary/10 rounded" />)}
              </div>
              <div className="h-12 w-full bg-secondary/10 rounded-xl" />
            </div>
          )}

          {!loading && syllabus && (
            <article className="group border border-secondary/25 rounded-3xl bg-card p-10 transition-all hover:border-primary/40 hover:shadow-2xl hover:shadow-primary/5">
              <div className="flex justify-between items-start mb-6">
                <div className="w-12 h-12 rounded-2xl bg-primary/10 text-primary flex items-center justify-center transition-colors group-hover:bg-primary/15">
                  <BookOpen className="w-6 h-6" />
                </div>
                <span className="text-xs font-bold px-3 py-1 rounded-full bg-primary text-white uppercase tracking-widest">
                  Best Seller
                </span>
              </div>

              <h3 className="text-2xl font-black tracking-tight mb-2">{syllabus.name}</h3>
              <p className="text-sm text-text/55 leading-relaxed mb-6">
                Consigue tu plaza fija en la Generalitat. Temas y preguntas de tipo examen. Solo necesitas la ESO. Todo el temario oficial de la oposición.
              </p>

              <ul className="space-y-4 mb-8">
                {FEATURES.map(f => (
                  <li key={f.label} className="flex items-center gap-3 text-sm text-text/65 font-medium">
                    <span className="text-primary shrink-0">{f.icon}</span>
                    {f.label}
                  </li>
                ))}
              </ul>

              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pt-4 border-t border-secondary/15">
                <div className="flex items-baseline gap-2">
                  <span className="text-3xl font-extrabold text-primary">15€</span>
                  <span className="text-sm text-text/45 line-through">39€</span>
                  <span className="text-sm font-bold text-green-600 dark:text-green-400 bg-green-100 dark:bg-green-900/30 px-3 py-1 rounded-full">Oferta</span>
                </div>
                <Link
                  to={ROUTES.subalternoGva}
                  className="btn btn-primary rounded-full px-8 py-4 text-base font-bold flex items-center justify-center gap-2 shadow-lg shadow-primary/20 w-full sm:w-auto"
                >
                  <UserAdd className="w-6 h-6" />
                  Ver temario
                </Link>
              </div>
            </article>
          )}

          {!loading && !syllabus && (
            <div className="border border-secondary/25 rounded-3xl bg-card p-10 text-center space-y-4">
              <div className="w-12 h-12 rounded-2xl bg-primary/10 text-primary flex items-center justify-center mx-auto">
                <BookOpen className="w-6 h-6" />
              </div>
              <p className="font-bold">Próximamente disponible</p>
              <p className="text-sm text-text/55">Regístrate para recibir novedades.</p>
              <Link to={ROUTES.register} className="btn btn-primary rounded-full px-6 py-2.5 text-sm inline-flex items-center gap-2 mt-2">
                <UserAdd className="w-6 h-6" />
                Crear cuenta
              </Link>
            </div>
          )}
        </div>
      </section>

      {/* Trust section */}
      <section className="border-y border-secondary/20 bg-secondary/5 px-6 py-16">
        <div className="max-w-4xl mx-auto flex flex-col md:flex-row items-center justify-between gap-12">
          <div className="max-w-sm text-center md:text-left">
            <h2 className="text-3xl font-extrabold mb-3 tracking-tight">
              Tu éxito es nuestra prioridad
            </h2>
            <p className="text-text/55 leading-relaxed">
              Estamos tan seguros de nuestro material que ofrecemos garantías exclusivas para tu tranquilidad.
            </p>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 w-full md:w-auto">
            {TRUST_ITEMS.map(item => (
              <div key={item.title} className="flex items-start gap-4">
                <div className="w-10 h-10 rounded-xl bg-primary/10 text-primary flex items-center justify-center shrink-0">
                  {item.icon}
                </div>
                <div>
                  <h4 className="font-bold mb-1">{item.title}</h4>
                  <p className="text-sm text-text/55 leading-relaxed">{item.desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Why us */}
      <section className="px-6 py-16">
        <div className="max-w-4xl mx-auto grid grid-cols-1 md:grid-cols-2 gap-12">
          <div>
            <p className="text-primary font-bold uppercase tracking-widest text-xs mb-3">¿Por qué elegirnos?</p>
            <p className="text-text/65 text-lg leading-relaxed">
              Nuestros temarios no son simples transcripciones de la ley. Son herramientas didácticas diseñadas para la
              retención a largo plazo, con jerarquías visuales y un lenguaje claro que facilita el estudio intensivo.
            </p>
          </div>
          <div className="flex flex-wrap gap-3 content-start">
            {TAGS.map(tag => (
              <span
                key={tag}
                className="px-4 py-2 bg-card border border-secondary/20 rounded-full text-sm font-semibold text-text/65"
              >
                {tag}
              </span>
            ))}
          </div>
        </div>
      </section>

      {/* Final CTA */}
      <section className="px-6 py-20 text-center border-t border-secondary/20">
        <div className="max-w-xl mx-auto">
          <h2 className="text-3xl sm:text-4xl font-extrabold mb-4 tracking-tight">
            ¿Listo para empezar?
          </h2>
          <p className="text-text/55 mb-8 text-lg">
            Crea tu cuenta y accede a todo el material de preparación hoy mismo.
          </p>
          <div className="flex flex-wrap gap-4 justify-center">
            <Link
              to={ROUTES.register}
              className="btn btn-primary rounded-full px-10 py-4 text-lg inline-flex items-center gap-2 shadow-xl shadow-primary/25"
            >
              <UserAdd className="w-6 h-6" />
              Crear mi cuenta gratis
            </Link>
            <Link
              to={ROUTES.login}
              className="btn btn-outline rounded-full px-10 py-4 text-lg inline-flex items-center gap-2"
            >
              <ArrowRightToBracket className="w-6 h-6" />
              Ya tengo cuenta
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
