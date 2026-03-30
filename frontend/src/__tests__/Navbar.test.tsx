import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import NavbarComponent from '../components/Navbar';
import type { NavUser } from '../types';

vi.mock('react-router-dom', () => ({
  useNavigate: () => vi.fn(),
  useLocation: () => ({ pathname: '/' }),
}));

const mockUser: NavUser = {
  email: 'user@example.com',
  initials: 'US',
};

describe('Navbar — Desktop', () => {
  const onLogout = vi.fn();
  const onSettings = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows user menu trigger when isAuthed=true and user provided', () => {
    render(
      <NavbarComponent
        isAuthed={true}
        isAdmin={false}
        user={mockUser}
        onLogout={onLogout}
        onSettings={onSettings}
      />
    );
    expect(screen.getByRole('button', { name: /user menu/i })).toBeInTheDocument();
  });

  it('shows Login and Registro links when isAuthed=false', () => {
    render(
      <NavbarComponent
        isAuthed={false}
        isAdmin={false}
        user={null}
        onLogout={onLogout}
        onSettings={onSettings}
      />
    );
    // Both desktop and mobile render the links, so multiple elements are expected
    expect(screen.getAllByText('Login').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Registro').length).toBeGreaterThan(0);
  });

  it('IA link is hidden for non-admin users', () => {
    render(
      <NavbarComponent
        isAuthed={true}
        isAdmin={false}
        user={mockUser}
        onLogout={onLogout}
        onSettings={onSettings}
      />
    );
    expect(screen.queryByText('IA')).not.toBeInTheDocument();
  });

  it('IA link is visible for admin users', () => {
    render(
      <NavbarComponent
        isAuthed={true}
        isAdmin={true}
        user={mockUser}
        onLogout={onLogout}
        onSettings={onSettings}
      />
    );
    expect(screen.getAllByText('IA').length).toBeGreaterThan(0);
  });

  it('desktop nav area does not contain standalone "Ajustes" or "Salir" buttons', () => {
    const { container } = render(
      <NavbarComponent
        isAuthed={true}
        isAdmin={false}
        user={mockUser}
        onLogout={onLogout}
        onSettings={onSettings}
      />
    );
    // Desktop nav links are inside the "hidden md:flex" div
    const desktopNav = container.querySelector('.hidden.md\\:flex');
    expect(desktopNav).not.toBeNull();
    // "Ajustes" and "Salir" must NOT appear in the desktop nav area
    expect(desktopNav!.textContent).not.toContain('Ajustes');
    expect(desktopNav!.textContent).not.toContain('Salir');
  });
});

describe('Navbar — theme toggle', () => {
  const onLogout = vi.fn();
  const onSettings = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    document.documentElement.classList.remove('dark');
  });

  it('toggles dark class on documentElement when theme button is clicked', () => {
    render(
      <NavbarComponent
        isAuthed={false}
        isAdmin={false}
        user={null}
        onLogout={onLogout}
        onSettings={onSettings}
      />
    );
    const themeBtn = screen.getByRole('button', { name: /cambiar tema/i });
    expect(document.documentElement.classList.contains('dark')).toBe(false);
    fireEvent.click(themeBtn);
    expect(document.documentElement.classList.contains('dark')).toBe(true);
    fireEvent.click(themeBtn);
    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });
});

describe('Navbar — navigation (go)', () => {
  const onLogout = vi.fn();
  const onSettings = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('clicking a nav link does not throw (exercises go function)', () => {
    render(
      <NavbarComponent
        isAuthed={false}
        isAdmin={false}
        user={null}
        onLogout={onLogout}
        onSettings={onSettings}
      />
    );
    // Click a Login button (which calls go(ROUTES.login) → navigate + setMenuOpen)
    const loginBtns = screen.getAllByText('Login');
    fireEvent.click(loginBtns[0]);
    expect(loginBtns[0]).toBeInTheDocument();
  });
});

describe('Navbar — Mobile', () => {
  const onLogout = vi.fn();
  const onSettings = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('divider between nav zone and user zone is present in the DOM when authed', () => {
    const { container } = render(
      <NavbarComponent
        isAuthed={true}
        isAdmin={false}
        user={mockUser}
        onLogout={onLogout}
        onSettings={onSettings}
      />
    );
    // The divider separating navigation and user zone in the mobile menu
    const dividers = container.querySelectorAll('.border-t.border-secondary\\/15');
    expect(dividers.length).toBeGreaterThan(0);
  });

  it('user email is displayed in the mobile user zone when authed', () => {
    render(
      <NavbarComponent
        isAuthed={true}
        isAdmin={false}
        user={mockUser}
        onLogout={onLogout}
        onSettings={onSettings}
      />
    );
    // Email appears in the mobile user zone (rendered in DOM even when menu closed)
    expect(screen.getByText('user@example.com')).toBeInTheDocument();
  });
});
