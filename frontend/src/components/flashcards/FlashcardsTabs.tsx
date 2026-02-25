type Tab = 'estudio' | 'examinar' | 'historial';

export default function FlashcardsTabs({ active, onTab }: { active: Tab; onTab?: (tab: Tab) => void }) {
  const tabs: { key: Tab; label: string }[] = [
    { key: 'estudio', label: 'Estudio' },
    { key: 'examinar', label: 'Examinar' },
    { key: 'historial', label: 'Historial' }
  ];

  return (
    <div className="inline-flex bg-white/80 dark:bg-slate-800 rounded-full p-1 shadow-sm border border-gray-200 dark:border-slate-700">
      {tabs.map((tab) => {
        const isActive = tab.key === active;
        return (
          <button
            key={tab.key}
            onClick={() => onTab?.(tab.key)}
            className={`px-4 py-2 rounded-full text-sm font-semibold transition ${
              isActive
                ? 'bg-primary text-white shadow'
                : 'text-slate-500 hover:text-slate-900 dark:text-slate-300'
            }`}
          >
            {tab.label}
          </button>
        );
      })}
    </div>
  );
}
