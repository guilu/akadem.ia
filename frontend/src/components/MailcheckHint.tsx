interface MailcheckHintProps {
  suggestion: string | null;
  onAccept: (corrected: string) => void;
}

export default function MailcheckHint({ suggestion, onAccept }: MailcheckHintProps) {
  if (!suggestion) return null;

  return (
    <p className="text-sm text-secondary mt-1">
      ¿Quisiste decir{' '}
      <button
        type="button"
        onClick={() => onAccept(suggestion)}
        className="text-primary font-semibold hover:underline"
      >
        {suggestion}
      </button>
      ?
    </p>
  );
}
