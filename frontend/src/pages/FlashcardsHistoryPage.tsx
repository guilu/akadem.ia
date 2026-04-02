import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getFlashcardsHistory } from '../api';
import FlashcardsTabs from '../components/flashcards/FlashcardsTabs';

type HistoryItem = {
  id: string;
  flashcardId: string;
  front: string | null;
  back: string | null;
  grade: 'AGAIN' | 'GOOD' | 'EASY';
  reviewedAt: string;
  intervalBefore: number | null;
  intervalAfter: number | null;
};

const GRADE_STYLE = {
  AGAIN: { label: 'Again', cls: 'text-red-400 bg-red-400/10 border border-red-400/30' },
  GOOD:  { label: 'Good',  cls: 'text-yellow-500 bg-yellow-400/10 border border-yellow-400/30' },
  EASY:  { label: 'Easy',  cls: 'text-lime-500 bg-lime-400/10 border border-lime-400/30' },
};

export default function FlashcardsHistoryPage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<HistoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const token = localStorage.getItem('ak_token') || '';

  useEffect(() => {
    let mounted = true;
    getFlashcardsHistory<HistoryItem[]>(token, 50)
      .then((data) => { if (mounted) setItems(data || []); })
      .catch(() => { if (mounted) setError('No se pudo cargar el historial.'); })
      .finally(() => { if (mounted) setLoading(false); });
    return () => { mounted = false; };
  }, [token]);

  return (
    <div className="space-y-6">
      <header className="space-y-3">
        <div className="py-[1.5rem]">
          <h1 className="text-3xl font-extrabold tracking-tight">
            Flash<span className="bg-gradient-to-r from-primary via-accent to-primary bg-clip-text text-transparent">cards</span>
          </h1>
          <p className="text-text/55 text-sm mt-1">Repasa por unidades con repetición espaciada.</p>
        </div>
        <FlashcardsTabs active="historial" onTab={(tab) => {
          if (tab === 'examinar') navigate('/flashcards');
          if (tab === 'estudio') navigate('/flashcards?mode=study');
        }} />
      </header>

      {loading ? (
        <div className="space-y-3">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-16 rounded-2xl border border-secondary/15 bg-secondary/5 animate-pulse" />
          ))}
        </div>
      ) : error ? (
        <div className="rounded-xl border border-red-400/30 bg-red-400/10 px-4 py-3 text-sm text-red-400">{error}</div>
      ) : items.length === 0 ? (
        <div className="border border-secondary/25 rounded-2xl px-5 py-12 text-center">
          <div className="text-4xl mb-3">📭</div>
          <p className="text-text/55 text-sm">Aún no has revisado ninguna tarjeta.</p>
          <button
            className="mt-5 btn btn-primary rounded-full px-6 py-2 text-sm shadow-sm shadow-primary/15"
            onClick={() => navigate('/flashcards?mode=study')}
          >
            Empezar a estudiar
          </button>
        </div>
      ) : (
        <div className="space-y-2">
          {items.map((item) => {
            const g = GRADE_STYLE[item.grade];
            const date = new Date(item.reviewedAt);
            return (
              <div key={item.id} className="border border-secondary/25 rounded-2xl p-4 flex items-start gap-3">
                <span className={`shrink-0 rounded-lg px-2.5 py-1 text-xs font-bold ${g.cls}`}>{g.label}</span>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium truncate">{item.front ?? '(sin texto)'}</p>
                  {item.intervalAfter != null && (
                    <p className="text-xs text-text/40 mt-0.5">
                      Intervalo: {item.intervalAfter}d
                      {item.intervalBefore != null && item.intervalBefore !== item.intervalAfter && (
                        <span className="ml-1 opacity-60">← {item.intervalBefore}d</span>
                      )}
                    </p>
                  )}
                </div>
                <div className="shrink-0 text-right text-xs text-text/40 tabular-nums">
                  <div>{date.toLocaleDateString('es-ES', { day: 'numeric', month: 'short' })}</div>
                  <div>{date.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })}</div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
