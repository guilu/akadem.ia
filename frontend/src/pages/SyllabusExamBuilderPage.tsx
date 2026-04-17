import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import {
  AdjustmentsHorizontal, Play, Lightbulb, BookOpen, CheckCircle,
} from 'flowbite-react-icons/outline';
import { getSyllabus, getSubjectsInSyllabus, startExamFromSyllabus } from '../api/syllabusApi';
import type { Syllabus, Subject } from '../types';
import { ROUTES } from '../constants/routes';

const inp =
  'bg-white/50 dark:bg-[#24394c] border border-secondary/30 rounded-xl px-4 py-2.5 text-sm text-text placeholder:text-text/35 focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary/50 transition-colors';

export default function SyllabusExamBuilderPage({ onUnauthorized }: { onUnauthorized: () => void }) {
  const { syllabusId } = useParams<{ syllabusId: string }>();
  const navigate = useNavigate();

  const [syllabus, setSyllabus] = useState<Syllabus | null>(null);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [count, setCount] = useState(20);
  const [minutes, setMinutes] = useState(20);
  const [difficulty, setDifficulty] = useState<'EASY' | 'MEDIUM' | 'HARD' | 'ALL'>('ALL');

  const [starting, setStarting] = useState(false);
  const [startError, setStartError] = useState('');

  useEffect(() => {
    if (!syllabusId) return;
    setLoading(true);
    setError('');
    Promise.all([
      getSyllabus(syllabusId),
      getSubjectsInSyllabus(syllabusId),
    ])
      .then(([syl, subs]) => {
        setSyllabus(syl);
        setSubjects(subs);
        setSelectedIds(new Set(subs.map((s) => s.id)));
      })
      .catch((err: unknown) => {
        if (err && typeof err === 'object' && 'status' in err && (err as { status: number }).status === 401) {
          onUnauthorized();
          return;
        }
        setError('No se pudieron cargar los datos del temario.');
      })
      .finally(() => setLoading(false));
  }, [syllabusId]);

  function toggleSubject(id: string) {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  function toggleAll() {
    if (selectedIds.size === subjects.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(subjects.map((s) => s.id)));
    }
  }

  async function handleStart() {
    if (!syllabusId || selectedIds.size === 0) return;
    setStarting(true);
    setStartError('');
    try {
      const data = await startExamFromSyllabus({
        syllabusId,
        subjectIds: Array.from(selectedIds),
        count,
        minutes,
        difficulty: difficulty === 'ALL' ? undefined : difficulty,
      });
      sessionStorage.setItem('akdmia.activeAttemptId', data.attemptId);
      navigate(ROUTES.examAttempt(data.attemptId));
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'status' in err && (err as { status: number }).status === 401) {
        onUnauthorized();
        return;
      }
      setStartError('No se pudo iniciar el examen. Inténtalo de nuevo.');
    } finally {
      setStarting(false);
    }
  }

  const allSelected = subjects.length > 0 && selectedIds.size === subjects.length;
  const noneSelected = selectedIds.size === 0;

  return (
    <div className="max-w-8xl mx-auto pb-32">

      {/* ── Hero ── */}
      <div className="mb-12">
        <Link
          to={syllabusId ? ROUTES.syllabusSubjects(syllabusId) : ROUTES.syllabuses}
          className="text-sm text-text/55 hover:text-text transition-colors mb-3 inline-block"
        >
          ← Volver a temas
        </Link>
        <h1 className="text-4xl font-extrabold tracking-tight mb-2">
          Examen desde{' '}
          <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">
            {syllabus ? syllabus.name : 'temario'}
          </span>
        </h1>
        <p className="text-text/55 max-w-2xl">
          Selecciona los temas que quieres incluir, configura el tiempo y la dificultad, y empieza tu simulacro.
        </p>
      </div>

      {loading && (
        <div className="text-sm text-text/55">Cargando temario...</div>
      )}

      {!loading && error && (
        <div className="rounded-xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm text-red-400">{error}</div>
      )}

      {!loading && !error && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">

          {/* ── Left column: config ── */}
          <div className="lg:col-span-4 space-y-6">
            <div className="border border-secondary/25 rounded-3xl bg-card p-8 flex flex-col gap-6">
              <div className="flex items-center gap-3">
                <AdjustmentsHorizontal className="w-5 h-5 text-primary" />
                <h2 className="text-xl font-bold">Configuración</h2>
              </div>

              {/* Difficulty */}
              <div className="space-y-2">
                <label className="text-sm font-semibold text-text/55">Dificultad</label>
                <select
                  className={inp + ' w-full'}
                  value={difficulty}
                  onChange={(e) => setDifficulty(e.target.value as 'EASY' | 'MEDIUM' | 'HARD' | 'ALL')}
                >
                  <option value="ALL">Todas</option>
                  <option value="EASY">Fácil</option>
                  <option value="MEDIUM">Media</option>
                  <option value="HARD">Difícil</option>
                </select>
              </div>

              {/* Minutes */}
              <div className="space-y-2">
                <label className="text-sm font-semibold text-text/55">Tiempo total (minutos)</label>
                <div className="flex items-center gap-2">
                  <svg className="w-4 h-4 text-text/40 shrink-0" fill="currentColor" viewBox="0 0 20 20">
                    <path
                      fillRule="evenodd"
                      d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z"
                      clipRule="evenodd"
                    />
                  </svg>
                  <input
                    type="number"
                    min={1}
                    value={minutes}
                    onChange={(e) => setMinutes(Math.max(1, Number(e.target.value)))}
                    className={inp + ' flex-1'}
                    placeholder="Ej. 60"
                  />
                </div>
              </div>

              {/* Question count */}
              <div className="space-y-2">
                <label className="text-sm font-semibold text-text/55">Número de preguntas</label>
                <input
                  type="number"
                  min={1}
                  value={count}
                  onChange={(e) => setCount(Math.max(1, Number(e.target.value)))}
                  className={inp + ' w-full'}
                  placeholder="Ej. 20"
                />
              </div>

              {startError && (
                <div className="rounded-xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm text-red-400">
                  {startError}
                </div>
              )}

              <button
                onClick={handleStart}
                disabled={noneSelected || starting}
                className="btn btn-primary w-full mt-2 py-4 rounded-3xl font-bold text-lg flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {starting ? (
                  <span className="animate-pulse">Iniciando...</span>
                ) : (
                  <>
                    <Play className="w-5 h-5" />
                    Empezar examen
                  </>
                )}
              </button>
            </div>

            {/* Tip */}
            <div className="bg-primary/10 rounded-3xl p-6 border border-primary/20">
              <div className="flex items-start gap-4">
                <Lightbulb className="w-5 h-5 text-primary shrink-0 mt-0.5" />
                <div>
                  <p className="text-primary font-bold mb-1">Tip de estudio</p>
                  <p className="text-sm text-text/60">
                    Combinar varios temas en un mismo examen te ayuda a detectar conexiones entre conceptos
                    y a prepararte mejor para el examen real.
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* ── Right column: subject selector ── */}
          <div className="lg:col-span-8">
            <div className="border border-secondary/25 rounded-3xl bg-card p-8">
              <div className="flex items-center justify-between gap-3 mb-6">
                <div className="flex items-center gap-3">
                  <BookOpen className="w-5 h-5 text-primary" />
                  <h2 className="text-xl font-bold">Seleccionar temas</h2>
                </div>
                {subjects.length > 0 && (
                  <button
                    onClick={toggleAll}
                    className="text-sm font-semibold text-primary hover:text-primary/80 transition-colors"
                  >
                    {allSelected ? 'Deseleccionar todos' : 'Seleccionar todos'}
                  </button>
                )}
              </div>

              {subjects.length === 0 && (
                <div className="text-center py-10 space-y-3">
                  <div className="w-12 h-12 rounded-2xl bg-primary/10 text-primary flex items-center justify-center mx-auto">
                    <BookOpen className="w-6 h-6" />
                  </div>
                  <p className="font-bold text-base">No hay temas en este temario</p>
                  <p className="text-sm text-text/55">Añade temas desde el panel de gestión.</p>
                </div>
              )}

              <div className="space-y-3">
                {subjects.map((s, i) => {
                  const checked = selectedIds.has(s.id);
                  return (
                    <label
                      key={s.id}
                      className={`flex items-center gap-4 p-5 border rounded-2xl cursor-pointer transition-all ${
                        checked
                          ? 'border-primary/40 bg-primary/5'
                          : 'border-secondary/20 hover:border-primary/25'
                      }`}
                    >
                      <input
                        type="checkbox"
                        className="sr-only"
                        checked={checked}
                        onChange={() => toggleSubject(s.id)}
                      />
                      {/* Custom checkbox */}
                      <div
                        className={`w-5 h-5 rounded-md border-2 shrink-0 flex items-center justify-center transition-all ${
                          checked ? 'border-primary bg-primary' : 'border-secondary/40'
                        }`}
                      >
                        {checked && (
                          <CheckCircle className="w-3.5 h-3.5 text-bg" />
                        )}
                      </div>

                      <div className="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center shrink-0">
                        <span className="text-primary font-bold text-xs">{i + 1}</span>
                      </div>

                      <div className="flex-1 min-w-0">
                        <div className="font-bold text-sm">{s.name}</div>
                        {s.description && (
                          <div className="text-xs text-text/50 mt-0.5 truncate">{s.description}</div>
                        )}
                      </div>
                    </label>
                  );
                })}
              </div>

              {subjects.length > 0 && (
                <div className="mt-6 pt-4 border-t border-secondary/15 flex items-center justify-between text-sm text-text/55">
                  <span>
                    <strong className="text-text">{selectedIds.size}</strong> de{' '}
                    <strong className="text-text">{subjects.length}</strong> temas seleccionados
                  </span>
                  {noneSelected && (
                    <span className="text-amber-500 font-semibold">Selecciona al menos un tema</span>
                  )}
                </div>
              )}
            </div>
          </div>

        </div>
      )}
    </div>
  );
}
