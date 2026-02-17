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
    <Navbar fluid className="border-b border-secondary/40 bg-bg/90 backdrop-blur">
      <NavbarBrand
        href="/"
        onClick={(event) => { event.preventDefault(); navigate('/'); }}
        className="flex items-center gap-3 text-2xl font-bold"
      >
        <img src="/assets/icons/akdmia-icon-32x32.png" alt="AKDMIA" className="w-8 h-8" />
        akadem.ia
      </NavbarBrand>
      <div className="flex md:order-2 items-center gap-2">
        {isAuthed && (
          <button
            className={`theme-toggle ${isDark ? 'dark' : 'light'}`}
            onClick={toggleTheme}
            aria-label="cambiar tema"
          >
            <span className="theme-thumb" />
            <span className="theme-icon" aria-hidden>☾</span>
            <span className="theme-icon" aria-hidden>☀︎</span>
          </button>
        )}
        <NavbarToggle />
      </div>
      <NavbarCollapse>
        <NavbarLink href="/" onClick={(event) => { event.preventDefault(); navigate('/'); }}>Home</NavbarLink>
        {!isAuthed && (
          <>
            <NavbarLink href="/login" onClick={(event) => { event.preventDefault(); navigate('/login'); }}>Login</NavbarLink>
            <NavbarLink href="/register" onClick={(event) => { event.preventDefault(); navigate('/register'); }}>Register</NavbarLink>
          </>
        )}
        {isAuthed && (
          <>
            <NavbarLink href="/subjects" onClick={(event) => { event.preventDefault(); navigate('/subjects'); }}>Exámenes</NavbarLink>
            {isAdmin && (
              <NavbarLink href="/settings" onClick={(event) => { event.preventDefault(); navigate('/settings'); }}>Configuración</NavbarLink>
            )}
            <button className="btn btn-outline" onClick={onLogout}>Salir</button>
          </>
        )}
      </NavbarCollapse>
    </Navbar>
  );
}
