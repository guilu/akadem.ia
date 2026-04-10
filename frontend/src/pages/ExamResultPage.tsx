import { Navigate, Link } from 'react-router-dom';
import { ArrowLeft, ArrowsRepeat } from 'flowbite-react-icons/outline';
import type { ExamResult } from '../types';
import { ROUTES } from '../constants/routes';

export default function ExamResultPage({ result, penaltyRatio = 3 }: { result: ExamResult | null; penaltyRatio?: number }) {
  if (!result) return <Navigate to={ROUTES.subjects} replace />;

  const score10 = result.total === 0 ? 0 : (result.net / result.total) * 10;

  const scoreColor = score10 < 5 ? 'text-red-500' : score10 <= 6 ? 'text-orange-400' : score10 <= 8 ? 'text-yellow-500' : 'text-lime-500';
  const scoreBorder = score10 < 5 ? 'border-red-400/30 bg-red-400/5' : score10 <= 6 ? 'border-orange-400/30 bg-orange-400/5' : score10 <= 8 ? 'border-yellow-400/30 bg-yellow-400/5' : 'border-lime-400/30 bg-lime-400/5';

  const stats = [
    { label: 'Total preguntas', value: result.total },
    { label: 'Correctas', value: result.correct, color: 'text-lime-500' },
    { label: 'Incorrectas', value: result.wrong, color: 'text-red-400' },
    { label: 'Penalización', value: result.penalty, color: 'text-orange-400' },
    { label: 'Score neto', value: result.net },
    { label: 'Porcentaje', value: `${result.percentage.toFixed(1)}%` },
  ];

  return (
    <div className="max-w-xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-extrabold tracking-tight mb-1">Resultados</h1>
        <p className="text-text/55 text-sm">Cada {penaltyRatio} {penaltyRatio === 1 ? 'fallo resta' : 'fallos restan'} 1 acierto.</p>
      </div>

      {/* ── Score card ── */}
      <div className={`border ${scoreBorder} rounded-2xl p-8 text-center mb-5`}>
        <div className="text-xs text-text/50 uppercase tracking-wide font-semibold mb-3">Nota final</div>
        <div className={`text-7xl font-extrabold tabular-nums mb-1 ${scoreColor}`}>
          {score10.toFixed(2)}
        </div>
        <div className="text-text/40 text-sm">sobre 10</div>
      </div>

      {/* ── Stats grid ── */}
      <div className="border border-secondary/25 rounded-2xl p-6 mb-5">
        <dl className="grid grid-cols-2 sm:grid-cols-3 gap-5">
          {stats.map(({ label, value, color }) => (
            <div key={label}>
              <dt className="text-xs text-text/50 mb-1">{label}</dt>
              <dd className={`text-2xl font-extrabold tabular-nums ${color || ''}`}>{value}</dd>
            </div>
          ))}
        </dl>
      </div>

      {/* ── Actions ── */}
      <div className="flex gap-3">
        <Link
          to={ROUTES.subjects}
          className="btn btn-primary rounded-full px-6 py-2.5 text-sm shadow-lg shadow-primary/20 flex items-center gap-2"
        >
          <ArrowsRepeat className="w-4 h-4" />
          Nuevo examen
        </Link>
        <Link
          to={ROUTES.home}
          className="btn btn-outline rounded-full px-6 py-2.5 text-sm flex items-center gap-2"
        >
          <ArrowLeft className="w-4 h-4" />
          Inicio
        </Link>
      </div>
    </div>
  );
}
