import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import FlashcardsHistoryPage from '../../pages/FlashcardsHistoryPage';
import * as api from '../../api';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
}));

vi.mock('../../components/flashcards/FlashcardsTabs', () => ({
  default: ({ active, onTab }: { active: string; onTab: (tab: string) => void }) => (
    <div data-testid="flashcards-tabs" data-active={active}>
      <button onClick={() => onTab('examinar')}>Examinar</button>
      <button onClick={() => onTab('estudio')}>Estudio</button>
      <button onClick={() => onTab('historial')}>Historial</button>
    </div>
  ),
}));

vi.mock('../../api', async (importOriginal) => {
  const actual = await importOriginal<typeof api>();
  return {
    ...actual,
    apiBase: 'http://localhost:8080',
    apiJson: vi.fn(),
  };
});

const mockHistoryItems = [
  {
    id: 'rev-1',
    flashcardId: 'fc-1',
    front: 'What is 2+2?',
    back: '4',
    grade: 'EASY' as const,
    reviewedAt: '2024-01-10T10:00:00Z',
    intervalBefore: 1,
    intervalAfter: 4,
  },
  {
    id: 'rev-2',
    flashcardId: 'fc-2',
    front: 'Capital of Spain?',
    back: 'Madrid',
    grade: 'GOOD' as const,
    reviewedAt: '2024-01-10T11:00:00Z',
    intervalBefore: null,
    intervalAfter: null,
  },
  {
    id: 'rev-3',
    flashcardId: 'fc-3',
    front: null,
    back: null,
    grade: 'AGAIN' as const,
    reviewedAt: '2024-01-10T12:00:00Z',
    intervalBefore: 2,
    intervalAfter: 2,
  },
];

describe('FlashcardsHistoryPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows loading skeletons initially', () => {
    vi.mocked(api.apiJson).mockImplementation(() => new Promise(() => {}));

    render(<FlashcardsHistoryPage />);

    // Loading state has animate-pulse elements
    const pulseEls = document.querySelectorAll('.animate-pulse');
    expect(pulseEls.length).toBeGreaterThan(0);
  });

  it('shows history items when loaded', async () => {
    vi.mocked(api.apiJson).mockResolvedValue(mockHistoryItems);

    render(<FlashcardsHistoryPage />);

    await waitFor(() => {
      expect(screen.getByText('What is 2+2?')).toBeInTheDocument();
    });
    expect(screen.getByText('Capital of Spain?')).toBeInTheDocument();
    expect(screen.getByText('(sin texto)')).toBeInTheDocument();
  });

  it('shows grade labels for each item', async () => {
    vi.mocked(api.apiJson).mockResolvedValue(mockHistoryItems);

    render(<FlashcardsHistoryPage />);

    await waitFor(() => {
      expect(screen.getByText('Easy')).toBeInTheDocument();
      expect(screen.getByText('Good')).toBeInTheDocument();
      expect(screen.getByText('Again')).toBeInTheDocument();
    });
  });

  it('shows interval info when intervalAfter is present', async () => {
    vi.mocked(api.apiJson).mockResolvedValue(mockHistoryItems);

    render(<FlashcardsHistoryPage />);

    await waitFor(() => {
      expect(screen.getByText(/Intervalo: 4d/)).toBeInTheDocument();
    });
  });

  it('shows interval change when intervalBefore differs from intervalAfter', async () => {
    vi.mocked(api.apiJson).mockResolvedValue(mockHistoryItems);

    render(<FlashcardsHistoryPage />);

    await waitFor(() => {
      expect(screen.getByText(/← 1d/)).toBeInTheDocument();
    });
  });

  it('shows empty state when no history', async () => {
    vi.mocked(api.apiJson).mockResolvedValue([]);

    render(<FlashcardsHistoryPage />);

    await waitFor(() => {
      expect(screen.getByText('Aún no has revisado ninguna tarjeta.')).toBeInTheDocument();
    });
  });

  it('navigates to study mode when "Empezar a estudiar" is clicked from empty state', async () => {
    vi.mocked(api.apiJson).mockResolvedValue([]);

    render(<FlashcardsHistoryPage />);

    await waitFor(() => screen.getByText('Empezar a estudiar'));
    fireEvent.click(screen.getByText('Empezar a estudiar'));
    expect(mockNavigate).toHaveBeenCalledWith('/flashcards?mode=study');
  });

  it('shows error state on API failure', async () => {
    vi.mocked(api.apiJson).mockRejectedValue(new Error('network error'));

    render(<FlashcardsHistoryPage />);

    await waitFor(() => {
      expect(screen.getByText('No se pudo cargar el historial.')).toBeInTheDocument();
    });
  });

  it('navigates to flashcards page when examinar tab is clicked', async () => {
    vi.mocked(api.apiJson).mockResolvedValue([]);

    render(<FlashcardsHistoryPage />);

    await waitFor(() => expect(vi.mocked(api.apiJson)).toHaveBeenCalledOnce());
    fireEvent.click(screen.getByText('Examinar'));
    expect(mockNavigate).toHaveBeenCalledWith('/flashcards');
  });

  it('navigates to study mode when estudio tab is clicked', async () => {
    vi.mocked(api.apiJson).mockResolvedValue([]);

    render(<FlashcardsHistoryPage />);

    await waitFor(() => expect(vi.mocked(api.apiJson)).toHaveBeenCalledOnce());
    fireEvent.click(screen.getByText('Estudio'));
    expect(mockNavigate).toHaveBeenCalledWith('/flashcards?mode=study');
  });

  it('fetches history with limit 50', async () => {
    vi.mocked(api.apiJson).mockResolvedValue([]);

    render(<FlashcardsHistoryPage />);

    await waitFor(() => {
      expect(vi.mocked(api.apiJson)).toHaveBeenCalledWith('http://localhost:8080/api/flashcards/history?limit=50');
    });
  });
});
