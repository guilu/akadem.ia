import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import FlashcardsStudyPage from '../../pages/FlashcardsStudyPage';
import * as api from '../../api';

vi.mock('react-router-dom', () => ({
  useNavigate: () => vi.fn(),
  useSearchParams: () => [new URLSearchParams('unitId=unit-1')],
}));

vi.mock('flowbite-react-icons/outline', () => ({
  ArrowLeft: () => <span data-testid="arrow-left" />,
  CircleMinus: () => <span data-testid="circle-minus" />,
  CheckCircle: () => <span data-testid="check-circle" />,
}));

vi.mock('../../api', async (importOriginal) => {
  const actual = await importOriginal<typeof api>();
  return {
    ...actual,
    apiAuthJson: vi.fn(),
  };
});

const mockQueue = { new: 1, due: 0, learning: 0 };
const emptyQueue = { new: 0, due: 0, learning: 0 };
const mockItem = {
  flashcardId: 'fc-1',
  front: '¿Cuánto es 2+2?',
  back: '4',
  state: 'NEW' as const,
  intervalHints: { again: '1m', good: '10m', easy: '4d' },
};

describe('FlashcardsStudyPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.setItem('ak_token', 'test-token');
  });

  it('shows skeleton while loading', () => {
    vi.mocked(api.apiAuthJson).mockReturnValue(new Promise(() => {}));

    const { container } = render(<FlashcardsStudyPage />);

    const skeletons = container.querySelectorAll('.animate-pulse');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('renders study session when loaded', async () => {
    vi.mocked(api.apiAuthJson)
      .mockResolvedValueOnce(mockQueue) // fetchQueue
      .mockResolvedValueOnce(mockItem); // fetchNext

    render(<FlashcardsStudyPage />);

    await waitFor(() => {
      expect(screen.getByText('¿Cuánto es 2+2?')).toBeInTheDocument();
    });
  });

  it('shows inline review error without replacing session', async () => {
    vi.mocked(api.apiAuthJson)
      .mockResolvedValueOnce(mockQueue) // initial fetchQueue
      .mockResolvedValueOnce(mockItem) // initial fetchNext
      .mockRejectedValueOnce(new Error('api_error')); // POST review fails

    render(<FlashcardsStudyPage />);

    await waitFor(() => {
      expect(screen.getByText('¿Cuánto es 2+2?')).toBeInTheDocument();
    });

    // Show answer first
    fireEvent.click(screen.getByText('Mostrar respuesta'));
    await waitFor(() => {
      expect(screen.getByText('4')).toBeInTheDocument();
    });

    // Click Repetir (AGAIN)
    fireEvent.click(screen.getByText('❌ Repetir'));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });

    // Session is NOT replaced — no finished screen
    expect(screen.queryByText('Sesión completada')).not.toBeInTheDocument();
    expect(screen.queryByText('Sin pendientes')).not.toBeInTheDocument();
    // Error message is visible
    expect(screen.getByRole('alert')).toHaveTextContent(
      /no se pudo registrar la respuesta/i
    );
  });

  it('review error can be dismissed', async () => {
    vi.mocked(api.apiAuthJson)
      .mockResolvedValueOnce(mockQueue)
      .mockResolvedValueOnce(mockItem)
      .mockRejectedValueOnce(new Error('api_error'));

    render(<FlashcardsStudyPage />);

    await waitFor(() => {
      expect(screen.getByText('¿Cuánto es 2+2?')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Mostrar respuesta'));
    await waitFor(() => screen.getByText('4'));

    fireEvent.click(screen.getByText('❌ Repetir'));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByLabelText('Cerrar error'));

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('shows "Nada que repasar hoy" when finished with 0 answered cards', async () => {
    vi.mocked(api.apiAuthJson)
      .mockResolvedValueOnce(emptyQueue) // fetchQueue — empty
      .mockResolvedValueOnce(undefined); // fetchNext — nothing

    render(<FlashcardsStudyPage />);

    await waitFor(() => {
      expect(screen.getByText('Nada que repasar hoy')).toBeInTheDocument();
    });

    expect(
      screen.getByText('No había tarjetas pendientes para repasar en esta sesión.')
    ).toBeInTheDocument();
    expect(screen.queryByText(/has respondido/i)).not.toBeInTheDocument();
  });

  it('shows answered count when finished after reviewing cards', async () => {
    vi.mocked(api.apiAuthJson)
      .mockResolvedValueOnce(mockQueue) // initial fetchQueue
      .mockResolvedValueOnce(mockItem) // initial fetchNext
      .mockResolvedValueOnce(undefined) // POST review (204 no content)
      .mockResolvedValueOnce(emptyQueue) // fetchQueue after review
      .mockResolvedValueOnce(undefined); // fetchNext after review — done

    render(<FlashcardsStudyPage />);

    await waitFor(() => {
      expect(screen.getByText('¿Cuánto es 2+2?')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('Mostrar respuesta'));
    await waitFor(() => screen.getByText('4'));

    fireEvent.click(screen.getByText('🚀 Memorizada'));

    await waitFor(() => {
      expect(screen.getByText('Sesión completada')).toBeInTheDocument();
    });

    expect(screen.getByText(/has respondido/i)).toBeInTheDocument();
  });

  it('opens confirm modal when Terminar sesión is clicked', async () => {
    vi.mocked(api.apiAuthJson)
      .mockResolvedValueOnce(mockQueue)
      .mockResolvedValueOnce(mockItem);

    render(<FlashcardsStudyPage />);

    await waitFor(() => expect(screen.getByText('¿Cuánto es 2+2?')).toBeInTheDocument());

    fireEvent.click(screen.getByText('Terminar sesión'));

    expect(screen.getByText('Terminar sesión', { selector: 'h3' })).toBeInTheDocument();
  });

  it('can cancel the confirm modal', async () => {
    vi.mocked(api.apiAuthJson)
      .mockResolvedValueOnce(mockQueue)
      .mockResolvedValueOnce(mockItem);

    render(<FlashcardsStudyPage />);

    await waitFor(() => expect(screen.getByText('¿Cuánto es 2+2?')).toBeInTheDocument());

    fireEvent.click(screen.getByText('Terminar sesión'));
    expect(screen.getByText('Terminar sesión', { selector: 'h3' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));
    expect(screen.queryByText('Terminar sesión', { selector: 'h3' })).not.toBeInTheDocument();
  });

  it('finishes session when Terminar is confirmed in modal', async () => {
    vi.mocked(api.apiAuthJson)
      .mockResolvedValueOnce(mockQueue)
      .mockResolvedValueOnce(mockItem);

    render(<FlashcardsStudyPage />);

    await waitFor(() => expect(screen.getByText('¿Cuánto es 2+2?')).toBeInTheDocument());

    fireEvent.click(screen.getByText('Terminar sesión'));
    // click the "Terminar" button inside the modal
    const terminarButtons = screen.getAllByRole('button', { name: 'Terminar' });
    const modalTerminar = terminarButtons.find(b => b.closest('.fixed'));
    fireEvent.click(modalTerminar!);

    await waitFor(() => {
      // After finishing with 0 answered, shows "Sin pendientes" or "Sesión completada"
      const hasSinPendientes = screen.queryByText('Sin pendientes');
      const hasSesionCompletada = screen.queryByText('Sesión completada');
      expect(hasSinPendientes || hasSesionCompletada).toBeTruthy();
    });
  });

  it('shows error state when initial load fails', async () => {
    vi.mocked(api.apiAuthJson).mockRejectedValue(new Error('api_error'));

    render(<FlashcardsStudyPage />);

    await waitFor(() => {
      expect(screen.getByText('No se pudo cargar la sesión de estudio.')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: /volver/i })).toBeInTheDocument();
  });
});
