import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import Register from '../components/Register';
import { API_ROUTES } from '../constants/apiRoutes';

const trackEventMock = vi.hoisted(() => vi.fn());
vi.mock('../lib/analytics', () => ({ trackEvent: trackEventMock }));

vi.mock('react-router-dom', () => ({
  Link: ({ children, to }: { children: React.ReactNode; to: string }) => (
    <a href={to}>{children}</a>
  ),
}));

const mockFetch = vi.fn();
vi.stubGlobal('fetch', mockFetch);

function makeResponse(body: unknown, status = 200, ok = true): Response {
  return {
    ok,
    status,
    json: () => Promise.resolve(body),
  } as unknown as Response;
}

describe('Register component', () => {
  const onAuthSuccess = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    mockFetch.mockReset();
  });

  it('renders the registration form', () => {
    render(<Register onAuthSuccess={onAuthSuccess} />);
    expect(screen.getByPlaceholderText('Email *')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Contraseña *')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Confirmar *')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /crear cuenta/i })).toBeInTheDocument();
  });

  it('shows error when email is empty', async () => {
    render(<Register onAuthSuccess={onAuthSuccess} />);
    fireEvent.click(screen.getByRole('button', { name: /crear cuenta/i }));
    await waitFor(() => {
      expect(screen.getByText('El email es obligatorio')).toBeInTheDocument();
    });
    expect(mockFetch).not.toHaveBeenCalled();
  });

  it('shows error for invalid email format', async () => {
    render(<Register onAuthSuccess={onAuthSuccess} />);
    fireEvent.change(screen.getByPlaceholderText('Email *'), {
      target: { value: 'not-valid' },
    });
    fireEvent.click(screen.getByRole('button', { name: /crear cuenta/i }));
    await waitFor(() => {
      expect(screen.getByText('El email no es válido')).toBeInTheDocument();
    });
  });

  it('shows error when password is empty', async () => {
    render(<Register onAuthSuccess={onAuthSuccess} />);
    fireEvent.change(screen.getByPlaceholderText('Email *'), {
      target: { value: 'user@example.com' },
    });
    fireEvent.click(screen.getByRole('button', { name: /crear cuenta/i }));
    await waitFor(() => {
      expect(screen.getByText('La contraseña es obligatoria')).toBeInTheDocument();
    });
  });

  it('shows error when password is too short', async () => {
    render(<Register onAuthSuccess={onAuthSuccess} />);
    fireEvent.change(screen.getByPlaceholderText('Email *'), {
      target: { value: 'user@example.com' },
    });
    fireEvent.change(screen.getByPlaceholderText('Contraseña *'), {
      target: { value: 'abc' },
    });
    fireEvent.click(screen.getByRole('button', { name: /crear cuenta/i }));
    await waitFor(() => {
      expect(screen.getByText('La contraseña debe tener al menos 8 caracteres')).toBeInTheDocument();
    });
  });

  it('shows error when confirm password is empty', async () => {
    render(<Register onAuthSuccess={onAuthSuccess} />);
    fireEvent.change(screen.getByPlaceholderText('Email *'), {
      target: { value: 'user@example.com' },
    });
    fireEvent.change(screen.getByPlaceholderText('Contraseña *'), {
      target: { value: 'password123' },
    });
    fireEvent.click(screen.getByRole('button', { name: /crear cuenta/i }));
    await waitFor(() => {
      expect(screen.getByText('La confirmación de contraseña es obligatoria')).toBeInTheDocument();
    });
  });

  it('shows error when passwords do not match', async () => {
    render(<Register onAuthSuccess={onAuthSuccess} />);
    fireEvent.change(screen.getByPlaceholderText('Email *'), {
      target: { value: 'user@example.com' },
    });
    fireEvent.change(screen.getByPlaceholderText('Contraseña *'), {
      target: { value: 'password123' },
    });
    fireEvent.change(screen.getByPlaceholderText('Confirmar *'), {
      target: { value: 'different456' },
    });
    fireEvent.click(screen.getByRole('button', { name: /crear cuenta/i }));
    await waitFor(() => {
      expect(screen.getByText('Las contraseñas no coinciden')).toBeInTheDocument();
    });
  });

  it('calls onAuthSuccess on successful registration', async () => {
    mockFetch.mockResolvedValue(makeResponse({ email: 'user@example.com', role: 'USER' }));

    render(<Register onAuthSuccess={onAuthSuccess} />);
    fireEvent.change(screen.getByPlaceholderText('Email *'), {
      target: { value: 'user@example.com' },
    });
    fireEvent.change(screen.getByPlaceholderText('Contraseña *'), {
      target: { value: 'password123' },
    });
    fireEvent.change(screen.getByPlaceholderText('Confirmar *'), {
      target: { value: 'password123' },
    });
    fireEvent.click(screen.getByRole('button', { name: /crear cuenta/i }));

    await waitFor(() => {
      expect(onAuthSuccess).toHaveBeenCalledWith({ email: 'user@example.com', role: 'USER' });
      expect(trackEventMock).toHaveBeenCalledWith('sign_up', { method: 'password' });
    });
  });

  it('does not track sign_up when registration fails', async () => {
    mockFetch.mockResolvedValue(makeResponse({ error: 'Email ya registrado' }, 409, false));
    render(<Register onAuthSuccess={onAuthSuccess} />);
    fireEvent.change(screen.getByPlaceholderText('Email *'), { target: { value: 'existing@example.com' } });
    fireEvent.change(screen.getByPlaceholderText('Contraseña *'), { target: { value: 'password123' } });
    fireEvent.change(screen.getByPlaceholderText('Confirmar *'), { target: { value: 'password123' } });
    fireEvent.click(screen.getByRole('button', { name: /crear cuenta/i }));
    await screen.findByText('Email ya registrado');
    expect(trackEventMock).not.toHaveBeenCalled();
  });

  it('uses API_ROUTES.auth.register endpoint', async () => {
    mockFetch.mockResolvedValue(makeResponse({ email: 'user@example.com', role: 'USER' }));

    render(<Register onAuthSuccess={onAuthSuccess} />);
    fireEvent.change(screen.getByPlaceholderText('Email *'), {
      target: { value: 'user@example.com' },
    });
    fireEvent.change(screen.getByPlaceholderText('Contraseña *'), {
      target: { value: 'password123' },
    });
    fireEvent.change(screen.getByPlaceholderText('Confirmar *'), {
      target: { value: 'password123' },
    });
    fireEvent.click(screen.getByRole('button', { name: /crear cuenta/i }));

    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledOnce();
      const [url] = mockFetch.mock.calls[0];
      expect(url).toContain(API_ROUTES.auth.register);
    });
  });

  it('shows server error message on failed registration', async () => {
    mockFetch.mockResolvedValue(makeResponse({ error: 'Email ya registrado' }, 409, false));

    render(<Register onAuthSuccess={onAuthSuccess} />);
    fireEvent.change(screen.getByPlaceholderText('Email *'), {
      target: { value: 'existing@example.com' },
    });
    fireEvent.change(screen.getByPlaceholderText('Contraseña *'), {
      target: { value: 'password123' },
    });
    fireEvent.change(screen.getByPlaceholderText('Confirmar *'), {
      target: { value: 'password123' },
    });
    fireEvent.click(screen.getByRole('button', { name: /crear cuenta/i }));

    await waitFor(() => {
      expect(screen.getByText('Email ya registrado')).toBeInTheDocument();
    });
  });

  it('shows default error when server returns no error message', async () => {
    mockFetch.mockResolvedValue(makeResponse({}, 500, false));

    render(<Register onAuthSuccess={onAuthSuccess} />);
    fireEvent.change(screen.getByPlaceholderText('Email *'), {
      target: { value: 'user@example.com' },
    });
    fireEvent.change(screen.getByPlaceholderText('Contraseña *'), {
      target: { value: 'password123' },
    });
    fireEvent.change(screen.getByPlaceholderText('Confirmar *'), {
      target: { value: 'password123' },
    });
    fireEvent.click(screen.getByRole('button', { name: /crear cuenta/i }));

    await waitFor(() => {
      expect(screen.getByText('No se pudo registrar')).toBeInTheDocument();
    });
  });

  it('shows network error on fetch failure', async () => {
    mockFetch.mockRejectedValue(new Error('network failure'));

    render(<Register onAuthSuccess={onAuthSuccess} />);
    fireEvent.change(screen.getByPlaceholderText('Email *'), {
      target: { value: 'user@example.com' },
    });
    fireEvent.change(screen.getByPlaceholderText('Contraseña *'), {
      target: { value: 'password123' },
    });
    fireEvent.change(screen.getByPlaceholderText('Confirmar *'), {
      target: { value: 'password123' },
    });
    fireEvent.click(screen.getByRole('button', { name: /crear cuenta/i }));

    await waitFor(() => {
      expect(screen.getByText('No se pudo conectar con el servidor')).toBeInTheDocument();
    });
  });

  it('renders Google OAuth link with correct URL', () => {
    render(<Register onAuthSuccess={onAuthSuccess} />);
    const googleLink = screen.getByText('Continuar con Google').closest('a');
    expect(googleLink?.getAttribute('href')).toContain(API_ROUTES.auth.oauthGoogle);
  });

  it('renders link to login page', () => {
    render(<Register onAuthSuccess={onAuthSuccess} />);
    expect(screen.getByText('Inicia sesión')).toBeInTheDocument();
  });
});
