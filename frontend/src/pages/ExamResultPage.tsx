import { Navigate, Link } from 'react-router-dom';
import type { ExamResult } from '../types';

export default function ExamResultPage({ result }: { result: ExamResult | null }) {
  if (!result) return <Navigate to="/subjects" replace />;
  const score10 = result.total === 0 ? 0 : (result.net / result.total) * 10;

  return (
    <div className="max-w-xl mx-auto p-4 border border-slate-700 rounded-xl">
      <h2 className="text-2xl font-bold mb-3">Resultados</h2>

      <div className="mb-4 rounded-xl border border-secondary/40 bg-secondary/20 p-4 text-center">
        <div className="text-text/70 text-sm uppercase tracking-wide">Nota final</div>
        <div className={`text-5xl font-extrabold ${score10 >= 5 ? 'text-lime-400' : 'text-rose-400'}`}>{score10.toFixed(2)}</div>
        <div className="text-text/70 text-sm">sobre 10</div>
      </div>

      <div className="space-y-1">
        <p>Total preguntas: <strong>{result.total}</strong></p>
        <p>Correctas: <strong>{result.correct}</strong></p>
        <p>Incorrectas: <strong>{result.wrong}</strong></p>
        <p>Penalización: <strong>{result.penalty}</strong></p>
        <p>Score neto: <strong>{result.net}</strong></p>
        <p>Porcentaje neto: <strong>{result.percentage.toFixed(1)}%</strong></p>
      </div>
      <p className="text-slate-400 mt-3">Cada 3 fallos restan 1 acierto.</p>
      <div className="mt-4">
        <Link className="btn btn-secondary" to="/subjects">Volver a exámenes</Link>
      </div>
    </div>
  );
}
