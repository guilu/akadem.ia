import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import ProfilePage from '../pages/ProfilePage';
import * as api from '../api';
import type { UserProfile } from '../types';

vi.mock('../api', () => ({
  getMyProfile: vi.fn(),
  updateMyProfile: vi.fn(),
}));

const mockProfile: UserProfile = {
  id: 'user-1',
  email: 'test@example.com',
  firstName: 'Ana',
  lastName: 'García',
};

describe('ProfilePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders form with email, firstName and lastName fields', async () => {
    vi.mocked(api.getMyProfile).mockResolvedValue(mockProfile);

    render(<ProfilePage />);

    expect(screen.getByLabelText(/correo electrónico/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/nombre/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/apellidos/i)).toBeInTheDocument();
  });

  it('on mount calls getMyProfile and populates fields', async () => {
    vi.mocked(api.getMyProfile).mockResolvedValue(mockProfile);

    render(<ProfilePage />);

    await waitFor(() => {
      expect(api.getMyProfile).toHaveBeenCalledWith();
    });

    expect(screen.getByLabelText(/correo electrónico/i)).toHaveValue('test@example.com');
    expect(screen.getByLabelText(/nombre/i)).toHaveValue('Ana');
    expect(screen.getByLabelText(/apellidos/i)).toHaveValue('García');
  });

  it('submitting valid data calls updateMyProfile and shows success message', async () => {
    vi.mocked(api.getMyProfile).mockResolvedValue(mockProfile);
    vi.mocked(api.updateMyProfile).mockResolvedValue({ ...mockProfile, firstName: 'Ana', lastName: 'García' });

    render(<ProfilePage />);

    await waitFor(() => expect(screen.getByLabelText(/nombre/i)).toHaveValue('Ana'));

    fireEvent.change(screen.getByLabelText(/nombre/i), { target: { value: 'María' } });
    fireEvent.submit(screen.getByRole('button', { name: /guardar cambios/i }).closest('form')!);

    await waitFor(() => {
      expect(api.updateMyProfile).toHaveBeenCalledWith({
        firstName: 'María',
        lastName: 'García',
      });
    });

    expect(await screen.findByRole('status')).toHaveTextContent(/cambios guardados/i);
  });

  it('on API error from updateMyProfile shows error message', async () => {
    vi.mocked(api.getMyProfile).mockResolvedValue(mockProfile);
    vi.mocked(api.updateMyProfile).mockRejectedValue(new Error('api_error'));

    render(<ProfilePage />);

    await waitFor(() => expect(screen.getByLabelText(/nombre/i)).toHaveValue('Ana'));

    fireEvent.submit(screen.getByRole('button', { name: /guardar cambios/i }).closest('form')!);

    expect(await screen.findByRole('alert')).toHaveTextContent(/no se pudieron guardar/i);
  });

  it('on API error from getMyProfile shows error message', async () => {
    vi.mocked(api.getMyProfile).mockRejectedValue(new Error('api_error'));

    render(<ProfilePage />);

    expect(await screen.findByRole('alert')).toHaveTextContent(/no se pudo cargar el perfil/i);
  });

  it('button is disabled while loading', async () => {
    // Never resolves so loading stays true
    vi.mocked(api.getMyProfile).mockReturnValue(new Promise(() => {}));

    render(<ProfilePage />);

    const btn = screen.getByRole('button', { name: /guardando/i });
    expect(btn).toBeDisabled();
  });

  it('handles null firstName and lastName gracefully', async () => {
    vi.mocked(api.getMyProfile).mockResolvedValue({ ...mockProfile, firstName: null, lastName: null });

    render(<ProfilePage />);

    await waitFor(() => {
      expect(screen.getByLabelText(/nombre/i)).toHaveValue('');
      expect(screen.getByLabelText(/apellidos/i)).toHaveValue('');
    });
  });
});
