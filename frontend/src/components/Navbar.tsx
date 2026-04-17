import { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { BookOpen, Cog, Home, ArrowRightToBracket, UserAdd, ArrowLeftToBracket, User, FolderOpen } from 'flowbite-react-icons/outline';
import { ROUTES } from '../constants/routes';
import type { NavUser } from '../types';
import { UserMenuDropdown } from './UserMenuDropdown';

export default function NavbarComponent({ isAuthed, isAdmin, user, onLogout, onProfile, onSettings }: {
  isAuthed: boolean;
  isAdmin: boolean;
  user: NavUser | null;
  onLogout: () => void;
  onProfile: () => void;
  onSettings: () => void;
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
          Akadem.ia
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
              <button onClick={() => go(ROUTES.syllabuses)} className={linkCls(ROUTES.syllabuses)}>
                <BookOpen className="w-4 h-4" />
                Exámenes
              </button>
              {isAdmin && (
                <button onClick={() => go(ROUTES.rag)} className={linkCls(ROUTES.rag)}>
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden>
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.456 2.456L21.75 6l-1.035.259a3.375 3.375 0 00-2.456 2.456z" />
                  </svg>
                  IA
                </button>
              )}
            </>
          )}
        </div>

        {/* Right: theme toggle + user menu / mobile burger */}
        <div className="flex items-center gap-2">
          {/* Theme toggle pill */}
          <div className="flex items-center bg-secondary/15 rounded-full p-1 gap-0.5">
            <button
              onClick={toggleTheme}
              aria-label="Tema oscuro"
              className={`p-1.5 rounded-full transition-all ${isDark ? 'bg-bg shadow-sm text-primary' : 'text-text/40 hover:text-text/60'}`}
            >
              <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                <path d="M17.293 13.293A8 8 0 016.707 2.707a8.001 8.001 0 1010.586 10.586z" />
              </svg>
            </button>
            <button
              onClick={toggleTheme}
              aria-label="Tema claro"
              className={`p-1.5 rounded-full transition-all ${!isDark ? 'bg-bg shadow-sm text-yellow-500' : 'text-text/40 hover:text-text/60'}`}
            >
              <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M10 2a1 1 0 011 1v1a1 1 0 11-2 0V3a1 1 0 011-1zm4 8a4 4 0 11-8 0 4 4 0 018 0zm-.464 4.95l.707.707a1 1 0 001.414-1.414l-.707-.707a1 1 0 00-1.414 1.414zm2.12-10.607a1 1 0 010 1.414l-.706.707a1 1 0 11-1.414-1.414l.707-.707a1 1 0 011.414 0zM17 11a1 1 0 100-2h-1a1 1 0 100 2h1zm-7 4a1 1 0 011 1v1a1 1 0 11-2 0v-1a1 1 0 011-1zM5.05 6.464A1 1 0 106.465 5.05l-.708-.707a1 1 0 00-1.414 1.414l.707.707zm1.414 8.486l-.707.707a1 1 0 01-1.414-1.414l.707-.707a1 1 0 011.414 1.414zM4 11a1 1 0 100-2H3a1 1 0 000 2h1z" clipRule="evenodd" />
              </svg>
            </button>
          </div>

          {/* Desktop user menu */}
          {isAuthed && user && (
            <div className="hidden md:block">
              <UserMenuDropdown
                user={user}
                isAdmin={isAdmin}
                onProfile={onProfile}
                onManage={() => go(ROUTES.manage)}
                onSettings={onSettings}
                onLogout={onLogout}
              />
            </div>
          )}

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

          {/* Navigation zone */}
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
              <button onClick={() => go(ROUTES.syllabuses)} className={mobileLinkCls(ROUTES.syllabuses)}>
                <BookOpen className="w-4 h-4" />
                Exámenes
              </button>
              {isAdmin && (
                <button onClick={() => go(ROUTES.rag)} className={mobileLinkCls(ROUTES.rag)}>
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden>
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.75} d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.456 2.456L21.75 6l-1.035.259a3.375 3.375 0 00-2.456 2.456z" />
                  </svg>
                  IA
                </button>
              )}

              {/* Divider between nav zone and user zone */}
              <div className="border-t border-secondary/15 my-1" />

              {/* User zone */}
              {user && (
                <div className="px-4 py-2 text-xs text-text/50 truncate">
                  {user.email}
                </div>
              )}
              <button
                onClick={() => { onProfile(); setMenuOpen(false); }}
                className="w-full text-left flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-colors text-text/65 hover:text-text hover:bg-secondary/10"
              >
                <User className="w-4 h-4" />
                Mi perfil
              </button>
              <button
                onClick={() => { go(ROUTES.manage); setMenuOpen(false); }}
                className="w-full text-left flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-colors text-text/65 hover:text-text hover:bg-secondary/10"
              >
                <FolderOpen className="w-4 h-4" />
                Gestionar
              </button>
              <button
                onClick={() => { onSettings(); setMenuOpen(false); }}
                className="w-full text-left flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-colors text-text/65 hover:text-text hover:bg-secondary/10"
              >
                <Cog className="w-4 h-4" />
                Ajustes
              </button>
              <button
                onClick={() => { onLogout(); setMenuOpen(false); }}
                className="w-full text-left flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium text-red-400 hover:bg-red-400/10 transition-colors"
              >
                <ArrowLeftToBracket className="w-4 h-4" />
                Salir
              </button>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}
