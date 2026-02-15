import { Link } from 'react-router-dom';

export default function Navbar({ isAuthed, isAdmin, onLogout, menuOpen, setMenuOpen, theme, onToggleTheme }: {
  isAuthed: boolean;
  isAdmin: boolean;
  onLogout: () => void;
  menuOpen: boolean;
  setMenuOpen: (v: boolean) => void;
  theme: 'dark' | 'light';
  onToggleTheme: () => void;
}) {
  return (
    <header className="border-b border-slate-800 bg-slate-950/80 backdrop-blur">
      <div className="max-w-5xl mx-auto px-6 py-4 flex items-center justify-between">
        <Link className="text-2xl font-bold flex items-center gap-3" to="/">
          <img src="/assets/icons/akdmia-icon-32x32.png" alt="AKDMIA" className="w-8 h-8" />
          akadem.ia
        </Link>
        <nav className="hidden md:flex gap-4 items-center">
          <Link className="hover:underline" to="/">Home</Link>
          {!isAuthed && (
            <>
              <Link className="hover:underline" to="/login">Login</Link>
              <Link className="hover:underline" to="/register">Register</Link>
            </>
          )}
          {isAuthed && (
            <>
              <Link className="hover:underline" to="/subjects">Asignaturas</Link>
              {isAdmin && (
                <Link className="hover:underline" to="/settings">Configuración</Link>
              )}
              <button className="text-sm border border-slate-600 rounded px-2 py-1" onClick={onLogout}>Salir</button>
            </>
          )}
        </nav>
        <div className="flex items-center gap-2">
          {isAuthed && (
            <button
              className={`theme-toggle ${theme}`}
              onClick={onToggleTheme}
              aria-label="cambiar tema"
            >
              <span className="theme-thumb" />
              <span className="theme-icon" aria-hidden>☀︎</span>
              <span className="theme-icon" aria-hidden>☾</span>
            </button>
          )}
          <button
            className="md:hidden text-sm border border-slate-600 rounded px-2 py-1"
            onClick={() => setMenuOpen(!menuOpen)}
            aria-label="menu"
          >
            {menuOpen ? 'Cerrar' : 'Menu'}
          </button>
        </div>
      </div>
      {menuOpen && (
        <div className="md:hidden border-t border-slate-800 px-6 py-3 flex flex-col gap-2">
          <Link className="text-left hover:underline" to="/" onClick={()=>setMenuOpen(false)}>Home</Link>
          {!isAuthed && (
            <>
              <Link className="text-left hover:underline" to="/login" onClick={()=>setMenuOpen(false)}>Login</Link>
              <Link className="text-left hover:underline" to="/register" onClick={()=>setMenuOpen(false)}>Register</Link>
            </>
          )}
          {isAuthed && (
            <>
              <Link className="text-left hover:underline" to="/subjects" onClick={()=>setMenuOpen(false)}>Asignaturas</Link>
              {isAdmin && (
                <Link className="text-left hover:underline" to="/settings" onClick={()=>setMenuOpen(false)}>Configuración</Link>
              )}
              <button className="text-left border border-slate-600 rounded px-2 py-1 w-fit" onClick={()=>{ onLogout(); setMenuOpen(false); }}>Salir</button>
            </>
          )}
        </div>
      )}
    </header>
  );
}
