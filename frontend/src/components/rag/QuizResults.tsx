import type { GeneratedDraft, GenerateQuizResponse } from '../../types';

interface Props {
  result: GenerateQuizResponse;
  onReset: () => void;
}

const DIFFICULTY_LABEL: Record<string, string> = {
  EASY: 'Fácil', MEDIUM: 'Media', HARD: 'Difícil'
};

function DraftCard({ draft, index }: { draft: GeneratedDraft; index: number }) {
  return (
    <div className="rounded-xl border border-secondary/20 bg-secondary/5 p-4 space-y-3">
      <div className="flex items-start justify-between gap-3">
        <p className="text-sm font-semibold leading-snug">
          <span className="text-primary font-bold mr-1">{index + 1}.</span>
          {draft.statement}
        </p>
        <span className="shrink-0 text-xs px-2 py-0.5 rounded-full bg-primary/10 text-primary font-medium">
          {DIFFICULTY_LABEL[draft.difficulty] ?? draft.difficulty}
        </span>
      </div>

      <ul className="space-y-1">
        {draft.answers.map((ans, i) => (
          <li
            key={i}
            className={`flex items-center gap-2 text-sm px-3 py-1.5 rounded-lg ${
              i === draft.correctIndex
                ? 'bg-green-400/15 text-green-700 dark:text-green-300 font-medium'
                : 'text-text/65'
            }`}
          >
            <span className={`w-5 h-5 rounded-full flex items-center justify-center text-xs font-bold shrink-0 ${
              i === draft.correctIndex
                ? 'bg-green-500 text-white'
                : 'bg-secondary/20 text-text/50'
            }`}>
              {String.fromCharCode(65 + i)}
            </span>
            {ans}
          </li>
        ))}
      </ul>

      {draft.explanation && (
        <p className="text-xs text-text/50 italic border-t border-secondary/15 pt-2">
          <span className="font-medium not-italic text-text/60">Explicación:</span> {draft.explanation}
        </p>
      )}
      {draft.hint && (
        <p className="text-xs text-text/50">
          <span className="font-medium text-text/60">Pista:</span> {draft.hint}
        </p>
      )}
      {draft.reference && (
        <p className="text-xs text-primary/70 font-medium">{draft.reference}</p>
      )}
    </div>
  );
}

export default function QuizResults({ result, onReset }: Props) {
  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-lg font-bold">{result.generated} pregunta{result.generated !== 1 ? 's' : ''} generada{result.generated !== 1 ? 's' : ''}</p>
          <p className="text-sm text-text/50">Las respuestas correctas se muestran en verde.</p>
        </div>
        <button
          onClick={onReset}
          className="text-sm px-4 py-2 rounded-xl border border-secondary/25 hover:bg-secondary/10 transition-colors"
        >
          Nueva generación
        </button>
      </div>

      {result.questions.length === 0 ? (
        <p className="text-sm text-text/50 text-center py-8">
          El modelo no generó preguntas válidas. Prueba con un tema más específico o diferente dificultad.
        </p>
      ) : (
        <div className="space-y-4">
          {result.questions.map((draft, i) => (
            <DraftCard key={draft.id ?? i} draft={draft} index={i} />
          ))}
        </div>
      )}
    </div>
  );
}
