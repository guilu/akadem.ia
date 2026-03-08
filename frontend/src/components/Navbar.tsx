import { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { BookOpen, Cog, Home, ArrowRightToBracket, UserAdd, ArrowLeftToBracket } from 'flowbite-react-icons/outline';
import { ROUTES } from '../constants/routes';

export default function NavbarComponent({ isAuthed, isAdmin, onLogout }: {
  isAuthed: boolean;
  isAdmin: boolean;
  onLogout: () => void;
}) {
  const [isDark, setIsDark] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    setIsDark(document.documentElement.classList.contains('dark'));
  }, []);

  // Close menu on route change
  useEffect(() => {
    setMenuOpen(false);
  }, [location.pathname]);

  function toggleTheme() {
    document.documentElement.classList.toggle('dark');
    const next = document.documentElement.classList.contains('dark');
    localStorage.setItem('theme', next ? 'dark' : 'light');
    setIsDark(next);
  }

  function go(route: string) {
    navigate(route);
    setMenuOpen(false);
  }

  const isActive = (route: string) => location.pathname === route || location.pathname.startsWith(route + '/');

  const linkCls = (route: string) =>
    `flex items-center gap-2 px-3 py-2 rounded-xl text-sm font-medium transition-colors ${
      isActive(route)
        ? 'bg-primary/10 text-primary'
        : 'text-text/65 hover:text-text hover:bg-secondary/10'
    }`;

  const mobileLinkCls = (route: string) =>
    `w-full text-left flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-colors ${
      isActive(route)
        ? 'bg-primary/10 text-primary'
        : 'text-text/65 hover:text-text hover:bg-secondary/10'
    }`;

  return (
    <nav className="fixed top-0 left-0 w-full z-50 border-b border-secondary/20 bg-bg/90 backdrop-blur-md">

      {/* ── Main bar ── */}
      <div className="max-w-7xl mx-auto px-5 h-16 flex items-center justify-between">

        {/* Logo */}
        <button
          onClick={() => go(ROUTES.home)}
          className="flex items-center gap-2.5 font-extrabold text-lg tracking-tight"
        >
          <img src="/assets/icons/akdmia-icon-64x64.png" alt="AKDMIA" className="w-8 h-8" />
          Akdmia
        </button>

        {/* Desktop links */}
        <div className="hidden md:flex items-center gap-1">
          <button onClick={() => go(ROUTES.home)} className={linkCls(ROUTES.home)}>
            <Home className="w-4 h-4" />
            Home
          </button>
          {!isAuthed && (
            <>
              <button onClick={() => go(ROUTES.login)} className={linkCls(ROUTES.login)}>
                <ArrowRightToBracket className="w-4 h-4" />
                Login
              </button>
              <button onClick={() => go(ROUTES.register)} className={linkCls(ROUTES.register)}>
                <UserAdd className="w-4 h-4" />
                Registro
              </button>
            </>
          )}
          {isAuthed && (
            <>
              <button onClick={() => go(ROUTES.flashcards)} className={linkCls(ROUTES.flashcards)}>
                <BookOpen className="w-4 h-4" />
                Flashcards
              </button>
              <button onClick={() => go(ROUTES.subjects)} className={linkCls(ROUTES.subjects)}>
                <BookOpen className="w-4 h-4" />
                Exámenes
              </button>
              {isAdmin && (
                <>
                  <button onClick={() => go(ROUTES.rag)} className={linkCls(ROUTES.rag)}>
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden>
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.456 2.456L21.75 6l-1.035.259a3.375 3.375 0 00-2.456 2.456z" />
                    </svg>
                    IA
                  </button>
                  <button onClick={() => go(ROUTES.settings)} className={linkCls(ROUTES.settings)}>
                    <Cog className="w-4 h-4" />
                    Ajustes
                  </button>
                </>
              )}
              <button
                onClick={onLogout}
                className="flex items-center gap-2 px-3 py-2 rounded-xl text-sm font-medium text-text/65 hover:text-red-400 hover:bg-red-400/10 transition-colors"
              >
                <ArrowLeftToBracket className="w-4 h-4" />
                Salir
              </button>
            </>
          )}
        </div>

        {/* Right: theme toggle + mobile burger */}
        <div className="flex items-center gap-2">
          <button
            className={`theme-toggle ${isDark ? 'dark' : 'light'}`}
            onClick={toggleTheme}
            aria-label="cambiar tema"
          >
            <span className="theme-thumb" />
            <span className="theme-icon" aria-hidden>☾</span>
            <span className="theme-icon" aria-hidden>☀︎</span>
          </button>

          {/* Hamburger / X button */}
          <button
            className="md:hidden flex items-center justify-center w-9 h-9 rounded-xl border border-secondary/25 bg-secondary/5 hover:bg-secondary/10 transition-colors"
            onClick={() => setMenuOpen((o) => !o)}
            aria-label={menuOpen ? 'Cerrar menú' : 'Abrir menú'}
            aria-expanded={menuOpen}
          >
            <div className="relative w-5 h-4">
              <span className={`absolute left-0 block h-0.5 w-5 bg-current rounded-full transition-all duration-300 origin-center ${menuOpen ? 'top-[7px] rotate-45' : 'top-0'}`} />
              <span className={`absolute left-0 block h-0.5 bg-current rounded-full transition-all duration-200 ${menuOpen ? 'w-0 opacity-0 top-[7px] left-1/2' : 'w-5 opacity-100 top-[7px]'}`} />
              <span className={`absolute left-0 block h-0.5 w-5 bg-current rounded-full transition-all duration-300 origin-center ${menuOpen ? 'top-[7px] -rotate-45' : 'top-[14px]'}`} />
            </div>
          </button>
        </div>
      </div>

      {/* ── Mobile menu ── */}
      <div
        className={`md:hidden overflow-hidden transition-all duration-300 ease-in-out ${
          menuOpen ? 'max-h-96 opacity-100' : 'max-h-0 opacity-0'
        }`}
      >
        <div className="px-4 pb-4 pt-2 border-t border-secondary/15 space-y-1 bg-bg/95 backdrop-blur-md">
          <button onClick={() => go(ROUTES.home)} className={mobileLinkCls(ROUTES.home)}>
            <Home className="w-4 h-4" />
            Home
          </button>
          {!isAuthed && (
            <>
              <button onClick={() => go(ROUTES.login)} className={mobileLinkCls(ROUTES.login)}>
                <ArrowRightToBracket className="w-4 h-4" />
                Login
              </button>
              <button onClick={() => go(ROUTES.register)} className={mobileLinkCls(ROUTES.register)}>
                <UserAdd className="w-4 h-4" />
                Registro
              </button>
            </>
          )}
          {isAuthed && (
            <>
              <button onClick={() => go(ROUTES.flashcards)} className={mobileLinkCls(ROUTES.flashcards)}>
                <BookOpen className="w-4 h-4" />
                Flashcards
              </button>
              <button onClick={() => go(ROUTES.subjects)} className={mobileLinkCls(ROUTES.subjects)}>
                <BookOpen className="w-4 h-4" />
                Exámenes
              </button>
              {isAdmin && (
                <>
                  <button onClick={() => go(ROUTES.rag)} className={mobileLinkCls(ROUTES.rag)}>
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden>
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.456 2.456L21.75 6l-1.035.259a3.375 3.375 0 00-2.456 2.456z" />
                    </svg>
                    IA
                  </button>
                  <button onClick={() => go(ROUTES.settings)} className={mobileLinkCls(ROUTES.settings)}>
                    <Cog className="w-4 h-4" />
                    Ajustes
                  </button>
                </>
              )}
              <div className="pt-1 mt-1 border-t border-secondary/15">
                <button
                  onClick={() => { onLogout(); setMenuOpen(false); }}
                  className="w-full text-left flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium text-red-400 hover:bg-red-400/10 transition-colors"
                >
                  <ArrowLeftToBracket className="w-4 h-4" />
                  Salir
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}
