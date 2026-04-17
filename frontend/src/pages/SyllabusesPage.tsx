import { useEffect, useState, useRef } from 'react';
import { Link } from 'react-router-dom';
import { BookOpen, Plus, ArrowsRepeat, ClipboardCheck, ChevronLeft, ChevronRight, Play } from 'flowbite-react-icons/outline';
import { getSyllabuses } from '../api/syllabusApi';
import { apiJson, apiBase } from '../api';
import type { Syllabus, ExamAttemptSummary } from '../types';
import { ROUTES } from '../constants/routes';
import { formatDuration } from '../utils/format';

export default function SyllabusesPage({ activeAttemptId, onUnauthorized, onViewResult, onResumeAttempt }: {
  activeAttemptId?: string;
  onUnauthorized: () => void;
  onViewResult: (attemptId: string) => void;
  onResumeAttempt: (attemptId: string) => void;
}) {
  const [syllabuses, setSyllabuses] = useState<Syllabus[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  const [history, setHistory] = useState<ExamAttemptSummary[]>([]);
  const [historyLoading, setHistoryLoading] = useState(true);
  const [historyError, setHistoryError] = useState('');
  const [showAll, setShowAll] = useState(false);
  const INITIAL_SHOW = 5;

  const carouselRef = useRef<HTMLDivElement>(null);
  const [activeDot, setActiveDot] = useState(0);

  const scroll = (direction: 'left' | 'right') => {
    if (carouselRef.current) {
      const { scrollLeft, clientWidth } = carouselRef.current;
      const scrollTo = direction === 'left' ? scrollLeft - clientWidth : scrollLeft + clientWidth;
      carouselRef.current.scrollTo({ left: scrollTo, behavior: 'smooth' });
    }
  };

  const handleScroll = () => {
    if (carouselRef.current) {
      const { scrollLeft, clientWidth } = carouselRef.current;
      const index = Math.round(scrollLeft / clientWidth);
      setActiveDot(index);
    }
  };

  useEffect(() => {
    setLoading(true);
    setError('');
    getSyllabuses()
      .then(setSyllabuses)
      .catch(() => setError('No se pudieron cargar los temarios.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    setHistoryLoading(true);
    setHistoryError('');
    apiJson<ExamAttemptSummary[]>(`${apiBase}/api/exams/attempts`)
      .then(setHistory)
      .catch((err: unknown) => {
        if (err && typeof err === 'object' && 'status' in err && (err as { status: number }).status === 401) onUnauthorized();
        setHistoryError('No se pudo cargar el historial.');
        setHistory([]);
      })
      .finally(() => setHistoryLoading(false));
  }, [apiBase, onUnauthorized]);

  return (
    <section className="mb-8">
      <div className="flex items-center justify-between mb-6">
        <div className="py-[1.5rem]">
          <h1 className="text-3xl font-extrabold tracking-tight">
            Elige tu{' '}
            <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">
              temario
            </span>
          </h1>
          <p className="text-text/55 text-sm mt-1">Selecciona un temario para explorar sus temas.</p>
        </div>
        {activeAttemptId && (
          <Link
            className="btn btn-outline rounded-full px-5 py-2.5 text-sm flex items-center gap-2"
            to={ROUTES.examAttempt(activeAttemptId)}
          >
            <ArrowsRepeat className="w-4 h-4" />
            Reanudar examen
          </Link>
        )}
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
        <div className="relative group mx-auto w-full">
          {/* Navigation Arrows */}
          <button
            onClick={() => scroll('left')}
            aria-label="Anterior"
            className="absolute -left-4 top-[40%] -translate-y-1/2 z-10 w-10 h-10 hidden md:flex items-center justify-center bg-card/80 backdrop-blur-sm border border-secondary/20 rounded-full text-text hover:border-primary transition-all opacity-0 group-hover:opacity-100"
          >
            <ChevronLeft className="w-6 h-6" />
          </button>
          <button
            onClick={() => scroll('right')}
            aria-label="Siguiente"
            className="absolute -right-4 top-[40%] -translate-y-1/2 z-10 w-10 h-10 hidden md:flex items-center justify-center bg-card/80 backdrop-blur-sm border border-secondary/20 rounded-full text-text hover:border-primary transition-all opacity-0 group-hover:opacity-100"
          >
            <ChevronRight className="w-6 h-6" />
          </button>

          {/* Carousel Container */}
          <div
            ref={carouselRef}
            onScroll={handleScroll}
            className="flex gap-6 overflow-x-auto pb-6 pt-2 snap-x snap-mandatory scroll-smooth no-scrollbar"
          >
            {syllabuses.map((s) => (
              <article
                key={s.id}
                className="min-w-[320px] md:min-w-[400px] snap-start group/card border border-secondary/25 rounded-3xl bg-card p-10 transition-all hover:border-primary/40 hover:shadow-2xl hover:shadow-primary/5"
              >
                <div className="w-12 h-12 rounded-2xl bg-primary/10 text-primary flex items-center justify-center mb-6 transition-colors group-hover/card:bg-primary/15">
                  <BookOpen className="w-6 h-6" />
                </div>

                <div className="space-y-3 mb-10">
                  <h3 className="text-2xl font-black tracking-tight">{s.name}</h3>
                  {s.description && (
                    <p className="text-sm text-text/55 leading-relaxed line-clamp-2">
                      {s.description}
                    </p>
                  )}
                </div>

                <div className="flex flex-col gap-3 mt-auto">
                  <div className="flex items-center gap-3">
                    <Link
                      className="btn btn-primary rounded-full px-6 py-2.5 text-sm font-bold shadow-lg shadow-primary/20 flex items-center gap-2 hover:scale-105 transition-transform"
                      to={ROUTES.syllabusSubjects(s.id)}
                    >
                      <Plus className="w-4 h-4" />
                      Ver temas
                    </Link>
                    <Link
                      className="btn btn-outline rounded-full px-6 py-2.5 text-sm font-bold flex items-center gap-2 hover:scale-105 transition-transform"
                      to={ROUTES.syllabusExamBuilder(s.id)}
                    >
                      <Play className="w-4 h-4" />
                      Crear examen
                    </Link>
                  </div>
                  {s.visibility && (
                    <div className="flex justify-end">
                      <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${s.visibility === 'GLOBAL' ? 'bg-blue-500/15 text-blue-600' : 'bg-secondary/20 text-text/60'}`}>
                        {s.visibility === 'GLOBAL' ? 'Público' : 'Privado'}
                      </span>
                    </div>
                  )}
                </div>
              </article>
            ))}
          </div>

          {/* Pagination Indicators */}
          <div className="flex justify-center items-center gap-2 mt-4">
            {syllabuses.map((_, i) => (
              <div
                key={i}
                className={`transition-all duration-300 rounded-full ${
                  activeDot === i
                    ? 'h-1.5 w-8 bg-primary'
                    : 'h-1.5 w-1.5 bg-primary/20'
                }`}
              />
            ))}
          </div>
        </div>
      )}

      {/* ── History ── */}
      <div className="mt-12">
        <h2 className="text-xl font-extrabold tracking-tight mb-4">Historial de exámenes</h2>

        {historyLoading && (
          <div className="text-sm text-text/55">Cargando historial...</div>
        )}

        {!historyLoading && historyError && (
          <div className="rounded-xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm text-red-400">{historyError}</div>
        )}

        {!historyLoading && !historyError && history.length === 0 && (
          <div className="border border-secondary/25 rounded-2xl p-6">
            <div className="text-base font-bold mb-1">Aún no tienes exámenes realizados</div>
            <div className="text-sm text-text/55">Empieza tu primer simulacro para ver tu progreso aquí.</div>
          </div>
        )}

        {!historyLoading && !historyError && history.length > 0 && (
          <div className="grid gap-3">
            {(showAll ? history : history.slice(0, INITIAL_SHOW)).map(h => {
              const finished = Boolean(h.finishedAt);
              const timeSpent = finished && h.startedAt && h.finishedAt
                ? Math.max(0, Math.floor((new Date(h.finishedAt).getTime() - new Date(h.startedAt).getTime()) / 1000))
                : 0;
              const pct = h.percent ?? 0;
              const scoreColorClass = pct < 50 ? 'text-red-400' : pct < 70 ? 'text-orange-400' : pct < 90 ? 'text-yellow-500' : 'text-lime-500';

              return (
                <div
                  key={h.attemptId}
                  className="border border-secondary/25 rounded-2xl px-5 py-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"
                >
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="font-bold text-sm">{h.subjectName || 'Materia'}</span>
                      <span className={`text-xs px-2 py-0.5 rounded-full border font-medium ${
                        finished
                          ? 'border-lime-400/30 bg-lime-400/10 text-lime-500'
                          : 'border-accent/30 bg-accent/10 text-accent'
                      }`}>
                        {finished ? 'Finalizado' : 'En curso'}
                      </span>
                    </div>
                    <div className="text-xs text-text/45 mb-2">{new Date(h.startedAt).toLocaleString()}</div>
                    <div className="flex flex-wrap gap-x-5 gap-y-1 text-xs text-text/60">
                      <span>Resultado: <strong className={`${scoreColorClass}`}>{(h.score ?? 0)} ({pct.toFixed(1)}%)</strong></span>
                      <span>Tiempo: <strong>{finished ? formatDuration(timeSpent) : formatDuration(h.totalTimeSeconds)}</strong></span>
                    </div>
                  </div>
                  <div>
                    {finished ? (
                      <button
                        className="btn btn-outline rounded-full px-5 py-2 text-sm flex items-center gap-2"
                        onClick={() => onViewResult(h.attemptId)}
                      >
                        <ClipboardCheck className="w-4 h-4" />
                        Ver resultados
                      </button>
                    ) : (
                      <button
                        className="btn btn-primary rounded-full px-5 py-2 text-sm shadow-sm shadow-primary/15 flex items-center gap-2"
                        onClick={() => onResumeAttempt(h.attemptId)}
                      >
                        <ArrowsRepeat className="w-4 h-4" />
                        Reanudar
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
            {history.length > INITIAL_SHOW && (
              <button
                className="btn btn-outline w-full"
                onClick={() => setShowAll(v => !v)}
              >
                {showAll ? 'Ver menos' : `Ver ${history.length - INITIAL_SHOW} más`}
              </button>
            )}
          </div>
        )}
      </div>
    </section>
  );
}
