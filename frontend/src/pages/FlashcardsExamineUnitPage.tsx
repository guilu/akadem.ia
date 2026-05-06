import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowLeft, ArrowRight, Inbox } from 'flowbite-react-icons/outline';
import { apiJson, apiBase } from '../api';

type Flashcard = {
  id: string;
  unitId: string;
  front: string;
  back: string;
};

export default function FlashcardsExamineUnitPage() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const unitId = params.get('unitId');

  const [cards, setCards] = useState<Flashcard[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [index, setIndex] = useState(0);
  const [flipped, setFlipped] = useState(false);
  useEffect(() => {
    if (!unitId) { setError('Falta el unitId.'); setLoading(false); return; }
    let mounted = true;
    apiJson<Flashcard[]>(`${apiBase}/api/flashcards?unitId=${unitId}`)
      .then((data) => { if (mounted) setCards(data || []); })
      .catch(() => { if (mounted) setError('No se pudieron cargar las tarjetas.'); })
      .finally(() => { if (mounted) setLoading(false); });
    return () => { mounted = false; };
  }, [unitId]);

  const card = cards[index];
  const total = cards.length;

  const prev = () => { setIndex((i) => Math.max(0, i - 1)); setFlipped(false); };
  const next = () => { setIndex((i) => Math.min(total - 1, i + 1)); setFlipped(false); };

  if (loading) {
    return (
      <div className="space-y-5 max-w-2xl mx-auto animate-pulse">
        <div className="flex items-center justify-between">
          <div className="h-9 w-9 rounded-full bg-secondary/20" />
          <div className="h-5 w-40 rounded-full bg-secondary/20" />
          <div className="w-9" />
        </div>
        <div className="space-y-1.5">
          <div className="h-3 w-1/3 rounded-full bg-secondary/20" />
          <div className="h-1.5 w-full rounded-full bg-secondary/20" />
        </div>
        <div className="border border-secondary/15 rounded-2xl p-7 min-h-[45vh] bg-secondary/5" />
      </div>
    );
  }

  if (error) return (
    <div className="space-y-4">
      <div className="rounded-xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm text-red-400">{error}</div>
      <button className="btn btn-primary rounded-full px-5 py-2 text-sm" onClick={() => navigate('/flashcards')}>Volver</button>
    </div>
  );

  if (!card) return (
    <div className="space-y-4 max-w-2xl mx-auto">
      <div className="border border-secondary/25 rounded-2xl px-5 py-12 text-center">
        <Inbox className="w-12 h-12 mx-auto mb-3 text-text/25" />
        <p className="text-text/55 text-sm">No hay tarjetas en esta unidad.</p>
      </div>
      <div className="flex items-center gap-3 flex-wrap">
        <button
          className="btn btn-outline rounded-full px-5 py-2 text-sm"
          onClick={() => navigate('/flashcards')}
        >
          Volver
        </button>
        <button
          className="btn btn-primary rounded-full px-5 py-2 text-sm shadow-sm shadow-primary/15"
          onClick={() => navigate('/flashcards')}
        >
          Importar flashcards
        </button>
      </div>
    </div>
  );

  const progressPct = Math.round(((index + 1) / total) * 100);

  return (
    <div className="space-y-5 max-w-2xl mx-auto">

      {/* Header */}
      <header className="flex items-center justify-between">
        <button
          onClick={() => navigate('/flashcards')}
          className="btn btn-outline h-9 w-9 rounded-full p-0 flex items-center justify-center"
          aria-label="Volver"
        >
          <ArrowLeft className="h-6 w-6" />
        </button>
        <h1 className="text-xl font-extrabold tracking-tight">Examinar <span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">Flashcards</span></h1>
        <div className="w-9" />
      </header>

      {/* Progress */}
      <div className="space-y-1.5">
        <div className="flex items-center justify-between text-xs text-text/50">
          <span>{flipped ? 'Respuesta' : 'Pregunta'}</span>
          <span className="font-medium">{index + 1} / {total}</span>
        </div>
        <div className="h-1.5 w-full rounded-full bg-secondary/20 overflow-hidden">
          <div
            className="h-full bg-primary rounded-full transition-all duration-300"
            style={{ width: `${progressPct}%` }}
          />
        </div>
      </div>

      {/* Card – tap to flip */}
      <div className="relative cursor-pointer" onClick={() => setFlipped((f) => !f)}>
        <div className="absolute -left-1.5 -bottom-1.5 h-full w-full rounded-2xl bg-white dark:bg-card brightness-90 dark:brightness-75" />
        <div className="relative border border-secondary/25 rounded-2xl p-7 min-h-[45vh] max-h-[45vh] flex flex-col bg-white dark:bg-card select-none">
          <div className="flex-1 overflow-y-auto min-h-0 flex items-center">
            <p className="text-xl font-semibold leading-relaxed w-full">
              {flipped ? card.back : card.front}
            </p>
          </div>
          <p className="pt-6 text-xs text-text/35 text-center w-full shrink-0">
            {flipped ? 'Toca para ver la pregunta' : 'Toca para ver la respuesta'}
          </p>
        </div>
      </div>

      {/* Navigation */}
      <div className="flex items-center justify-between gap-3">
        <button
          onClick={prev}
          disabled={index === 0}
          className="btn btn-outline rounded-full px-5 py-2 text-sm disabled:opacity-30 flex items-center gap-2"
        >
          <ArrowLeft className="w-6 h-6" />
          Anterior
        </button>
        <button
          onClick={next}
          disabled={index === total - 1}
          className="btn btn-outline rounded-full px-5 py-2 text-sm disabled:opacity-30 flex items-center gap-2"
        >
          Siguiente
          <ArrowRight className="w-6 h-6" />
        </button>
      </div>

      {/* Study CTA */}
      <div className="border border-secondary/15 rounded-2xl px-5 py-4 flex items-center justify-between gap-4">
        <p className="text-sm text-text/55">¿Listo para practicar con repetición espaciada?</p>
        <button
          className="shrink-0 btn btn-primary rounded-full px-4 py-2 text-sm shadow-sm shadow-primary/15"
          onClick={() => navigate(`/flashcards/study?unitId=${unitId}`)}
        >
          Estudiar
        </button>
      </div>
    </div>
  );
}
