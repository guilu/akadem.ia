import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import Login from '../components/Login';
import { API_ROUTES } from '../constants/apiRoutes';

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

describe('Login component', () => {
  const onAuthSuccess = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    mockFetch.mockReset();
  });

  it('renders the login form', () => {
    render(<Login onAuthSuccess={onAuthSuccess} />);
    expect(screen.getByPlaceholderText('tu@email.com')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('••••••••')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /entrar/i })).toBeInTheDocument();
  });

  it('shows validation error when email is empty', async () => {
    render(<Login onAuthSuccess={onAuthSuccess} />);
    fireEvent.click(screen.getByRole('button', { name: /entrar/i }));
    await waitFor(() => {
      expect(screen.getByText('El email es obligatorio')).toBeInTheDocument();
    });
    expect(mockFetch).not.toHaveBeenCalled();
  });

  it('shows validation error for invalid email', async () => {
    render(<Login onAuthSuccess={onAuthSuccess} />);
    fireEvent.change(screen.getByPlaceholderText('tu@email.com'), {
      target: { value: 'not-an-email' },
    });
    fireEvent.click(screen.getByRole('button', { name: /entrar/i }));
    await waitFor(() => {
      expect(screen.getByText('El email no es válido')).toBeInTheDocument();
    });
    expect(mockFetch).not.toHaveBeenCalled();
  });

  it('shows validation error when password is empty', async () => {
    render(<Login onAuthSuccess={onAuthSuccess} />);
    fireEvent.change(screen.getByPlaceholderText('tu@email.com'), {
      target: { value: 'user@example.com' },
    });
    fireEvent.click(screen.getByRole('button', { name: /entrar/i }));
    await waitFor(() => {
      expect(screen.getByText('La contraseña es obligatoria')).toBeInTheDocument();
    });
    expect(mockFetch).not.toHaveBeenCalled();
  });

  it('calls onAuthSuccess with authenticated user on successful login', async () => {
    mockFetch.mockResolvedValue(makeResponse({ email: 'user@example.com', role: 'USER' }));

    render(<Login onAuthSuccess={onAuthSuccess} />);
    fireEvent.change(screen.getByPlaceholderText('tu@email.com'), {
      target: { value: 'user@example.com' },
    });
    fireEvent.change(screen.getByPlaceholderText('••••••••'), {
      target: { value: 'password123' },
    });
    fireEvent.click(screen.getByRole('button', { name: /entrar/i }));

    await waitFor(() => {
      expect(onAuthSuccess).toHaveBeenCalledWith({ email: 'user@example.com', role: 'USER' });
    });
  });

  it('uses API_ROUTES.auth.login endpoint', async () => {
    mockFetch.mockResolvedValue(makeResponse({ email: 'user@example.com', role: 'USER' }));

    render(<Login onAuthSuccess={onAuthSuccess} />);
    fireEvent.change(screen.getByPlaceholderText('tu@email.com'), {
      target: { value: 'user@example.com' },
    });
    fireEvent.change(screen.getByPlaceholderText('••••••••'), {
      target: { value: 'pass1234' },
    });
    fireEvent.click(screen.getByRole('button', { name: /entrar/i }));

    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledOnce();
      const [url] = mockFetch.mock.calls[0];
      expect(url).toContain(API_ROUTES.auth.login);
    });
  });

  it('shows error message from server on failed login', async () => {
    mockFetch.mockResolvedValue(makeResponse({ error: 'Credenciales incorrectas' }, 401, false));

    render(<Login onAuthSuccess={onAuthSuccess} />);
    fireEvent.change(screen.getByPlaceholderText('tu@email.com'), {
      target: { value: 'user@example.com' },
    });
    fireEvent.change(screen.getByPlaceholderText('••••••••'), {
      target: { value: 'wrongpass' },
    });
    fireEvent.click(screen.getByRole('button', { name: /entrar/i }));

    await waitFor(() => {
      expect(screen.getByText('Credenciales incorrectas')).toBeInTheDocument();
    });
  });

  it('shows default error when server returns no error message', async () => {
    mockFetch.mockResolvedValue(makeResponse({}, 401, false));

    render(<Login onAuthSuccess={onAuthSuccess} />);
    fireEvent.change(screen.getByPlaceholderText('tu@email.com'), {
      target: { value: 'user@example.com' },
    });
    fireEvent.change(screen.getByPlaceholderText('••••••••'), {
      target: { value: 'wrongpass' },
    });
    fireEvent.click(screen.getByRole('button', { name: /entrar/i }));

    await waitFor(() => {
      expect(screen.getByText('Credenciales inválidas')).toBeInTheDocument();
    });
  });

  it('shows network error on fetch failure', async () => {
    mockFetch.mockRejectedValue(new Error('network failure'));

    render(<Login onAuthSuccess={onAuthSuccess} />);
    fireEvent.change(screen.getByPlaceholderText('tu@email.com'), {
      target: { value: 'user@example.com' },
    });
    fireEvent.change(screen.getByPlaceholderText('••••••••'), {
      target: { value: 'pass1234' },
    });
    fireEvent.click(screen.getByRole('button', { name: /entrar/i }));

    await waitFor(() => {
      expect(screen.getByText('No se pudo conectar con el servidor')).toBeInTheDocument();
    });
  });

  it('triggers login on Enter key in email field', async () => {
    mockFetch.mockResolvedValue(makeResponse({ email: 'user@example.com', role: 'USER' }));

    render(<Login onAuthSuccess={onAuthSuccess} />);
    fireEvent.change(screen.getByPlaceholderText('tu@email.com'), {
      target: { value: 'user@example.com' },
    });
    fireEvent.change(screen.getByPlaceholderText('••••••••'), {
      target: { value: 'pass1234' },
    });
    fireEvent.keyDown(screen.getByPlaceholderText('tu@email.com'), { key: 'Enter' });

    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledOnce();
    });
  });

  it('triggers login on Enter key in password field', async () => {
    mockFetch.mockResolvedValue(makeResponse({ email: 'user@example.com', role: 'USER' }));

    render(<Login onAuthSuccess={onAuthSuccess} />);
    fireEvent.change(screen.getByPlaceholderText('tu@email.com'), {
      target: { value: 'user@example.com' },
    });
    fireEvent.change(screen.getByPlaceholderText('••••••••'), {
      target: { value: 'pass1234' },
    });
    fireEvent.keyDown(screen.getByPlaceholderText('••••••••'), { key: 'Enter' });

    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledOnce();
    });
  });

  it('renders Google OAuth link with correct URL', () => {
    render(<Login onAuthSuccess={onAuthSuccess} />);
    const googleLink = screen.getByText('Continuar con Google').closest('a');
    expect(googleLink).toHaveAttribute('href');
    expect(googleLink?.getAttribute('href')).toContain(API_ROUTES.auth.oauthGoogle);
  });

  it('renders link to register page', () => {
    render(<Login onAuthSuccess={onAuthSuccess} />);
    expect(screen.getByText('Regístrate gratis')).toBeInTheDocument();
  });
});
