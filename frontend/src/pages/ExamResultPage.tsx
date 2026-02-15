import { Navigate, Link } from 'react-router-dom';
import type { ExamResult } from '../types';

export default function ExamResultPage({ result }: { result: ExamResult | null }) {
  if (!result) return <Navigate to="/subjects" replace />;
  return (
    <div className="max-w-xl mx-auto p-4 border border-slate-700 rounded-xl">
      <h2 className="text-2xl font-bold mb-3">Resultados</h2>
      <p>Correctas: <strong>{result.correct}</strong> / {result.total}</p>
      <p>Porcentaje: <strong>{result.percentage.toFixed(1)}%</strong></p>
      <div className="mt-4">
        <Link className="px-3 py-2 rounded bg-indigo-600 inline-flex" to="/subjects">Volver a asignaturas</Link>
      </div>
    </div>
  );
}
