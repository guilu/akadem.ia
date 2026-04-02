import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import FlashcardsExamineUnitPage from '../../pages/FlashcardsExamineUnitPage';
import * as api from '../../api';

vi.mock('react-router-dom', () => ({
  useNavigate: () => vi.fn(),
  useSearchParams: () => [new URLSearchParams('unitId=unit-1')],
}));

vi.mock('../../api', async (importOriginal) => {
  const actual = await importOriginal<typeof api>();
  return {
    ...actual,
    apiAuthJson: vi.fn(),
    getFlashcardsByUnit: vi.fn(),
  };
});

const mockCards = [
  { id: 'card-1', unitId: 'unit-1', front: '¿Qué es la derivada?', back: 'Tasa de cambio instantánea.' },
  { id: 'card-2', unitId: 'unit-1', front: '¿Qué es la integral?', back: 'Área bajo la curva.' },
];

describe('FlashcardsExamineUnitPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.setItem('ak_token', 'test-token');
  });

  it('shows skeleton while loading', () => {
    vi.mocked(api.getFlashcardsByUnit).mockReturnValue(new Promise(() => {}));

    const { container } = render(<FlashcardsExamineUnitPage />);

    const skeletons = container.querySelectorAll('.animate-pulse');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('renders cards when loaded', async () => {
    vi.mocked(api.getFlashcardsByUnit).mockResolvedValue(mockCards);

    render(<FlashcardsExamineUnitPage />);

    await waitFor(() => {
      expect(screen.getByText('¿Qué es la derivada?')).toBeInTheDocument();
    });
  });

  it('shows empty state with Volver and import CTA when no cards', async () => {
    vi.mocked(api.getFlashcardsByUnit).mockResolvedValue([]);

    render(<FlashcardsExamineUnitPage />);

    await waitFor(() => {
      expect(screen.getByText('No hay tarjetas en esta unidad.')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: /volver/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /importar flashcards/i })).toBeInTheDocument();
  });

  it('shows error state on API failure', async () => {
    vi.mocked(api.getFlashcardsByUnit).mockRejectedValue(new Error('api_error'));

    render(<FlashcardsExamineUnitPage />);

    await waitFor(() => {
      expect(screen.getByText('No se pudieron cargar las tarjetas.')).toBeInTheDocument();
    });
  });

  it('flips card when clicked to show back, and again to show front', async () => {
    vi.mocked(api.getFlashcardsByUnit).mockResolvedValue(mockCards);

    render(<FlashcardsExamineUnitPage />);

    await waitFor(() => expect(screen.getByText('¿Qué es la derivada?')).toBeInTheDocument());

    // Initially shows front text
    expect(screen.getByText('Toca para ver la respuesta')).toBeInTheDocument();

    // Click card to flip
    fireEvent.click(screen.getByText('¿Qué es la derivada?').closest('.cursor-pointer')!);

    await waitFor(() => {
      expect(screen.getByText('Tasa de cambio instantánea.')).toBeInTheDocument();
    });
    expect(screen.getByText('Toca para ver la pregunta')).toBeInTheDocument();

    // Click again to flip back
    fireEvent.click(screen.getByText('Tasa de cambio instantánea.').closest('.cursor-pointer')!);

    await waitFor(() => {
      expect(screen.getByText('¿Qué es la derivada?')).toBeInTheDocument();
    });
  });

  it('navigates to next card when Siguiente is clicked', async () => {
    vi.mocked(api.getFlashcardsByUnit).mockResolvedValue(mockCards);

    render(<FlashcardsExamineUnitPage />);

    await waitFor(() => expect(screen.getByText('¿Qué es la derivada?')).toBeInTheDocument());

    fireEvent.click(screen.getByText('Siguiente →'));

    await waitFor(() => {
      expect(screen.getByText('¿Qué es la integral?')).toBeInTheDocument();
    });
  });

  it('navigates to previous card when Anterior is clicked', async () => {
    vi.mocked(api.getFlashcardsByUnit).mockResolvedValue(mockCards);

    render(<FlashcardsExamineUnitPage />);

    await waitFor(() => expect(screen.getByText('¿Qué es la derivada?')).toBeInTheDocument());

    // Go to second card
    fireEvent.click(screen.getByText('Siguiente →'));
    await waitFor(() => expect(screen.getByText('¿Qué es la integral?')).toBeInTheDocument());

    // Go back
    fireEvent.click(screen.getByText('← Anterior'));
    await waitFor(() => expect(screen.getByText('¿Qué es la derivada?')).toBeInTheDocument());
  });

  it('disables Anterior button on first card', async () => {
    vi.mocked(api.getFlashcardsByUnit).mockResolvedValue(mockCards);

    render(<FlashcardsExamineUnitPage />);

    await waitFor(() => expect(screen.getByText('¿Qué es la derivada?')).toBeInTheDocument());

    const prevBtn = screen.getByText('← Anterior');
    expect(prevBtn).toBeDisabled();
  });

  it('disables Siguiente button on last card', async () => {
    vi.mocked(api.getFlashcardsByUnit).mockResolvedValue(mockCards);

    render(<FlashcardsExamineUnitPage />);

    await waitFor(() => expect(screen.getByText('¿Qué es la derivada?')).toBeInTheDocument());

    fireEvent.click(screen.getByText('Siguiente →'));
    await waitFor(() => expect(screen.getByText('¿Qué es la integral?')).toBeInTheDocument());

    expect(screen.getByText('Siguiente →')).toBeDisabled();
  });

  it('resets flip state when navigating to next card', async () => {
    vi.mocked(api.getFlashcardsByUnit).mockResolvedValue(mockCards);

    render(<FlashcardsExamineUnitPage />);

    await waitFor(() => expect(screen.getByText('¿Qué es la derivada?')).toBeInTheDocument());

    // Flip current card
    fireEvent.click(screen.getByText('¿Qué es la derivada?').closest('.cursor-pointer')!);
    await waitFor(() => expect(screen.getByText('Tasa de cambio instantánea.')).toBeInTheDocument());

    // Navigate to next — flip should reset
    fireEvent.click(screen.getByText('Siguiente →'));
    await waitFor(() => {
      expect(screen.getByText('¿Qué es la integral?')).toBeInTheDocument();
    });
    expect(screen.getByText('Toca para ver la respuesta')).toBeInTheDocument();
  });
});
