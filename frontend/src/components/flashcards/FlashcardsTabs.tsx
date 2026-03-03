type Tab = 'estudio' | 'examinar' | 'historial';

export default function FlashcardsTabs({ active, onTab }: { active: Tab; onTab?: (tab: Tab) => void }) {
  const tabs: { key: Tab; label: string }[] = [
    { key: 'estudio', label: 'Estudio' },
    { key: 'examinar', label: 'Examinar' },
    { key: 'historial', label: 'Historial' },
  ];

  return (
    <div className="inline-flex bg-secondary/10 border border-secondary/25 rounded-full p-1">
      {tabs.map((tab) => (
        <button
          key={tab.key}
          onClick={() => onTab?.(tab.key)}
          className={`px-4 py-1.5 rounded-full text-sm font-semibold transition-colors ${
            tab.key === active
              ? 'bg-primary text-bg shadow-sm'
              : 'text-text/60 hover:text-text'
          }`}
        >
          {tab.label}
        </button>
      ))}
    </div>
  );
}
