export type ContentScope = 'GLOBAL' | 'PRIVATE' | 'ALL';

interface ScopeFilterProps {
  scope: ContentScope;
  onChange: (scope: ContentScope) => void;
  isAdmin: boolean;
}

export default function ScopeFilter({ scope, onChange, isAdmin }: ScopeFilterProps) {
  type Option = { value: ContentScope; label: string };

  const options: Option[] = isAdmin
    ? [
        { value: 'ALL', label: 'Todo' },
        { value: 'GLOBAL', label: 'Global' },
        { value: 'PRIVATE', label: 'Personal' },
      ]
    : [
        { value: 'ALL', label: 'Todo' },
        { value: 'PRIVATE', label: 'Personal' },
      ];

  return (
    <div className="flex items-center gap-1 bg-secondary/8 rounded-xl p-1 w-fit">
      {options.map((opt) => (
        <button
          key={opt.value}
          onClick={() => onChange(opt.value)}
          className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
            scope === opt.value
              ? 'bg-primary text-white shadow-sm'
              : 'text-text/60 hover:text-text hover:bg-secondary/15'
          }`}
        >
          {opt.label}
        </button>
      ))}
    </div>
  );
}
