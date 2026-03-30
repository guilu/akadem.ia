import { useState, useEffect, useRef } from 'react';
import { NavUser } from '../types';

interface Props {
  user: NavUser;
  isAdmin: boolean;
  onSettings: () => void;
  onLogout: () => void;
}

export function UserMenuDropdown({ user, isAdmin, onSettings, onLogout }: Props) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleOutsideClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    function handleEscape(e: KeyboardEvent) {
      if (e.key === 'Escape') setOpen(false);
    }
    document.addEventListener('mousedown', handleOutsideClick);
    document.addEventListener('keydown', handleEscape);
    return () => {
      document.removeEventListener('mousedown', handleOutsideClick);
      document.removeEventListener('keydown', handleEscape);
    };
  }, []);

  return (
    <div ref={ref} className="relative">
      <button
        onClick={() => setOpen(prev => !prev)}
        className="flex items-center justify-center w-8 h-8 rounded-full bg-primary/15 text-primary text-sm font-semibold hover:bg-primary/25 transition-colors"
        aria-label="User menu"
        aria-expanded={open}
      >
        {user.avatarUrl ? (
          <img src={user.avatarUrl} alt={user.initials} className="w-8 h-8 rounded-full object-cover" />
        ) : (
          user.initials
        )}
      </button>

      {open && (
        <div className="absolute right-0 mt-2 w-48 bg-bg border border-secondary/20 rounded-xl shadow-lg z-50 py-1">
          <div className="px-3 py-2 text-xs text-text/50 truncate border-b border-secondary/10 mb-1">
            {user.email}
          </div>
          {/* Future: Profile entry */}
          <button
            onClick={() => { onSettings(); setOpen(false); }}
            className="w-full text-left px-3 py-2 text-sm text-text hover:bg-secondary/10 transition-colors"
          >
            Ajustes
          </button>
          <div className="border-t border-secondary/10 my-1" />
          <button
            onClick={() => { onLogout(); setOpen(false); }}
            className="w-full text-left px-3 py-2 text-sm text-red-500 hover:bg-red-500/10 transition-colors"
          >
            Cerrar sesión
          </button>
        </div>
      )}
    </div>
  );
}
