import { Search } from 'flowbite-react-icons/outline';

export default function SearchInput({ value, onChange }: { value: string; onChange: (v: string) => void }) {
  return (
    <div className="relative">
      <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-text/35" />
      <input
        type="text"
        placeholder="Buscar unidad..."
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-full bg-bg border border-secondary/30 rounded-xl pl-10 pr-4 py-2.5 text-sm text-text placeholder:text-text/35 focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary/50 transition-colors"
      />
    </div>
  );
}
