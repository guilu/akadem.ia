import { Navbar, NavbarBrand, NavbarCollapse, NavbarLink, NavbarToggle } from 'flowbite-react';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

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
    <Navbar fluid className="border-b border-secondary/40 bg-bg/90 backdrop-blur" style={{ alignItems: 'center' }}>
      <NavbarBrand
        href="/"
        onClick={(event) => { event.preventDefault(); navigate('/'); }}
        className="flex items-center gap-3 text-2xl font-bold"
      >
        <img src="/assets/icons/akdmia-icon-64x64.png" alt="AKDMIA" className="w-16 h-16" />
        akadem.ia
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
        <NavbarLink className="flex items-center" href="/" onClick={(event) => { event.preventDefault(); navigate('/'); }}>Home</NavbarLink>
        {!isAuthed && (
          <>
            <NavbarLink className="flex items-center" href="/login" onClick={(event) => { event.preventDefault(); navigate('/login'); }}>Login</NavbarLink>
            <NavbarLink className="flex items-center" href="/register" onClick={(event) => { event.preventDefault(); navigate('/register'); }}>Register</NavbarLink>
          </>
        )}
        {isAuthed && (
          <>
            <NavbarLink className="flex items-center" href="/subjects" onClick={(event) => { event.preventDefault(); navigate('/subjects'); }}>Exámenes</NavbarLink>
            {isAdmin && (
              <NavbarLink className="flex items-center" href="/settings" onClick={(event) => { event.preventDefault(); navigate('/settings'); }}>Configuración</NavbarLink>
            )}
            <NavbarLink
              href="#"
              className="flex items-center"
              onClick={(event) => { event.preventDefault(); onLogout(); }}
            >
              <span className="btn btn-outline h-9">Salir</span>
            </NavbarLink>
          </>
        )}
      </NavbarCollapse>
    </Navbar>
  );
}
