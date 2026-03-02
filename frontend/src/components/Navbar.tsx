import { Navbar, NavbarBrand, NavbarCollapse, NavbarLink, NavbarToggle } from 'flowbite-react';
import { BookOpen, Cog, Home, ArrowRightToBracket, UserAdd, ArrowLeftToBracket } from 'flowbite-react-icons/outline';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '../constants/routes';

export default function NavbarComponent({ isAuthed, isAdmin, onLogout }: {
  isAuthed: boolean;
  isAdmin: boolean;
  onLogout: () => void;
}) {
  const [isDark, setIsDark] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    setIsDark(document.documentElement.classList.contains('dark'));
  }, []);

  function toggleTheme() {
    document.documentElement.classList.toggle('dark');
    const next = document.documentElement.classList.contains('dark');
    localStorage.setItem('theme', next ? 'dark' : 'light');
    setIsDark(next);
  }

  return (
    <Navbar fluid className="fixed top-0 left-0 w-full z-50 border-b border-secondary/40 bg-bg/90 backdrop-blur" style={{ alignItems: 'center' }}>
      <NavbarBrand
        href={ROUTES.home}
        onClick={(event) => { event.preventDefault(); navigate(ROUTES.home); }}
        className="flex items-center gap-3 text-2xl font-bold"
      >
        <img src="/assets/icons/akdmia-icon-64x64.png" alt="AKDMIA" className="w-16 h-16" />
       ConstituApp
      </NavbarBrand>
      <div className="flex md:order-2 items-center gap-2">
        <button
          className={`theme-toggle ${isDark ? 'dark' : 'light'}`}
          onClick={toggleTheme}
          aria-label="cambiar tema"
        >
          <span className="theme-thumb" />
          <span className="theme-icon" aria-hidden>☾</span>
          <span className="theme-icon" aria-hidden>☀︎</span>
        </button>
        <NavbarToggle />
      </div>
      <NavbarCollapse className="md:flex md:items-center [&>ul]:md:flex [&>ul]:md:items-center [&>ul]:md:gap-6 [&>ul>li>a]:justify-center [&>ul>li>a]:text-center">
        <NavbarLink className="flex items-center gap-2" href={ROUTES.home} onClick={(event) => { event.preventDefault(); navigate(ROUTES.home); }}>
          <Home className="w-4 h-4" />
          Home
        </NavbarLink>
        {!isAuthed && (
          <>
            <NavbarLink className="flex items-center gap-2" href={ROUTES.login} onClick={(event) => { event.preventDefault(); navigate(ROUTES.login); }}>
              <ArrowRightToBracket className="w-4 h-4" />
              Login
            </NavbarLink>
            <NavbarLink className="flex items-center gap-2" href={ROUTES.register} onClick={(event) => { event.preventDefault(); navigate(ROUTES.register); }}>
              <UserAdd className="w-4 h-4" />
              Register
            </NavbarLink>
          </>
        )}
        {isAuthed && (
          <>
            <NavbarLink className="flex items-center gap-2" href={ROUTES.flashcards} onClick={(event) => { event.preventDefault(); navigate(ROUTES.flashcards); }}>
              <BookOpen className="w-4 h-4" />
              Flashcards
            </NavbarLink>
            <NavbarLink className="flex items-center gap-2" href={ROUTES.subjects} onClick={(event) => { event.preventDefault(); navigate(ROUTES.subjects); }}>
              <BookOpen className="w-4 h-4" />
              Exámenes
            </NavbarLink>
            {isAdmin && (
              <NavbarLink className="flex items-center gap-2" href={ROUTES.settings} onClick={(event) => { event.preventDefault(); navigate(ROUTES.settings); }}>
                <Cog className="w-4 h-4" />
                Configuración
              </NavbarLink>
            )}
            <NavbarLink
              href="#"
              className="flex items-center"
              onClick={(event) => { event.preventDefault(); onLogout(); }}
            >
              <span className="btn btn-outline h-9 flex items-center gap-2">
                <ArrowLeftToBracket className="w-4 h-4" />
                Salir
              </span>
            </NavbarLink>
          </>
        )}
      </NavbarCollapse>
    </Navbar>
  );
}
