import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { UserMenuDropdown } from '../components/UserMenuDropdown';
import type { NavUser } from '../types';

const baseUser: NavUser = {
  email: 'test@example.com',
  initials: 'TE',
};

const userWithAvatar: NavUser = {
  email: 'avatar@example.com',
  initials: 'AV',
  avatarUrl: 'https://example.com/avatar.png',
};

describe('UserMenuDropdown', () => {
  const onProfile = vi.fn();
  const onSettings = vi.fn();
  const onLogout = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders avatar initials when no avatarUrl provided', () => {
    render(
      <UserMenuDropdown
        user={baseUser}
        isAdmin={false}
        onProfile={onProfile}
        onSettings={onSettings}
        onLogout={onLogout}
      />
    );
    const trigger = screen.getByRole('button', { name: /user menu/i });
    expect(trigger).toHaveTextContent('TE');
    expect(trigger.querySelector('img')).toBeNull();
  });

  it('renders <img> when avatarUrl is provided with correct alt attribute', () => {
    render(
      <UserMenuDropdown
        user={userWithAvatar}
        isAdmin={false}
        onProfile={onProfile}
        onSettings={onSettings}
        onLogout={onLogout}
      />
    );
    const img = screen.getByRole('img', { name: 'AV' });
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute('src', 'https://example.com/avatar.png');
  });

  it('dropdown panel is NOT visible initially', () => {
    render(
      <UserMenuDropdown
        user={baseUser}
        isAdmin={false}
        onProfile={onProfile}
        onSettings={onSettings}
        onLogout={onLogout}
      />
    );
    expect(screen.queryByText('Ajustes')).not.toBeInTheDocument();
    expect(screen.queryByText('Mi perfil')).not.toBeInTheDocument();
    expect(screen.queryByText('Cerrar sesión')).not.toBeInTheDocument();
  });

  it('clicking the trigger opens the panel', () => {
    render(
      <UserMenuDropdown
        user={baseUser}
        isAdmin={false}
        onProfile={onProfile}
        onSettings={onSettings}
        onLogout={onLogout}
      />
    );
    fireEvent.click(screen.getByRole('button', { name: /user menu/i }));
    expect(screen.getByText('Mi perfil')).toBeInTheDocument();
    expect(screen.getByText('Ajustes')).toBeInTheDocument();
    expect(screen.getByText('Cerrar sesión')).toBeInTheDocument();
    expect(screen.getByText('test@example.com')).toBeInTheDocument();
  });

  it('clicking outside the dropdown closes it', () => {
    render(
      <div>
        <div data-testid="outside">Outside</div>
        <UserMenuDropdown
          user={baseUser}
          isAdmin={false}
          onProfile={onProfile}
          onSettings={onSettings}
          onLogout={onLogout}
        />
      </div>
    );
    fireEvent.click(screen.getByRole('button', { name: /user menu/i }));
    expect(screen.getByText('Ajustes')).toBeInTheDocument();

    fireEvent.mouseDown(screen.getByTestId('outside'));
    expect(screen.queryByText('Ajustes')).not.toBeInTheDocument();
  });

  it('pressing Escape closes the panel', () => {
    render(
      <UserMenuDropdown
        user={baseUser}
        isAdmin={false}
        onProfile={onProfile}
        onSettings={onSettings}
        onLogout={onLogout}
      />
    );
    fireEvent.click(screen.getByRole('button', { name: /user menu/i }));
    expect(screen.getByText('Ajustes')).toBeInTheDocument();

    fireEvent.keyDown(document, { key: 'Escape' });
    expect(screen.queryByText('Ajustes')).not.toBeInTheDocument();
  });

  it('clicking "Mi perfil" calls onProfile and closes the panel', () => {
    render(
      <UserMenuDropdown
        user={baseUser}
        isAdmin={false}
        onProfile={onProfile}
        onSettings={onSettings}
        onLogout={onLogout}
      />
    );
    fireEvent.click(screen.getByRole('button', { name: /user menu/i }));
    fireEvent.click(screen.getByText('Mi perfil'));

    expect(onProfile).toHaveBeenCalledTimes(1);
    expect(screen.queryByText('Mi perfil')).not.toBeInTheDocument();
  });

  it('clicking "Ajustes" calls onSettings and closes the panel', () => {
    render(
      <UserMenuDropdown
        user={baseUser}
        isAdmin={false}
        onProfile={onProfile}
        onSettings={onSettings}
        onLogout={onLogout}
      />
    );
    fireEvent.click(screen.getByRole('button', { name: /user menu/i }));
    fireEvent.click(screen.getByText('Ajustes'));

    expect(onSettings).toHaveBeenCalledTimes(1);
    expect(screen.queryByText('Ajustes')).not.toBeInTheDocument();
  });

  it('clicking "Cerrar sesión" calls onLogout and closes the panel', () => {
    render(
      <UserMenuDropdown
        user={baseUser}
        isAdmin={false}
        onProfile={onProfile}
        onSettings={onSettings}
        onLogout={onLogout}
      />
    );
    fireEvent.click(screen.getByRole('button', { name: /user menu/i }));
    fireEvent.click(screen.getByText('Cerrar sesión'));

    expect(onLogout).toHaveBeenCalledTimes(1);
    expect(screen.queryByText('Cerrar sesión')).not.toBeInTheDocument();
  });
});
