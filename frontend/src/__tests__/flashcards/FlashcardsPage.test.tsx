import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import FlashcardsPage from '../../pages/FlashcardsPage';
import * as api from '../../api';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
  useSearchParams: () => [new URLSearchParams()],
}));

vi.mock('../../components/flashcards/FlashcardImportModal', () => ({
  default: ({ onClose, onImported }: { onClose: () => void; onImported: () => void }) => (
    <div data-testid="import-modal">
      <button onClick={onClose}>Cerrar modal</button>
      <button onClick={onImported}>Importado</button>
    </div>
  ),
}));

vi.mock('../../components/flashcards/FlashcardsTabs', () => ({
  default: ({ onTab }: { onTab: (tab: string) => void }) => (
    <div data-testid="flashcards-tabs">
      <button onClick={() => onTab('estudio')}>Estudio</button>
      <button onClick={() => onTab('examinar')}>Examinar</button>
      <button onClick={() => onTab('historial')}>Historial</button>
    </div>
  ),
}));

vi.mock('../../components/flashcards/SearchInput', () => ({
  default: ({ onChange }: { onChange: (v: string) => void }) => (
    <input data-testid="search-input" onChange={(e) => onChange(e.target.value)} />
  ),
}));

vi.mock('../../api', async (importOriginal) => {
  const actual = await importOriginal<typeof api>();
  return {
    ...actual,
    apiJson: vi.fn(),
    exportFlashcardsBySubject: vi.fn(),
  };
});

const mockUnits = [
  {
    unitId: 'unit-1',
    unitName: 'Álgebra',
    subjectId: 'subj-1',
    subjectName: 'Matemáticas',
    newCount: 3,
    reviewCount: 2,
    dueCount: 1,
  },
  {
    unitId: 'unit-2',
    unitName: 'Trigonometría',
    subjectId: 'subj-1',
    subjectName: 'Matemáticas',
    newCount: 1,
    reviewCount: 0,
    dueCount: 0,
  },
];

describe('FlashcardsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    globalThis.URL.createObjectURL = vi.fn(() => 'blob:test');
    globalThis.URL.revokeObjectURL = vi.fn();
  });

  it('shows empty state with import CTA when no subjects available', async () => {
    vi.mocked(api.apiJson).mockResolvedValue([]);

    render(<FlashcardsPage />);

    await waitFor(() => {
      expect(screen.getByText('No hay materias disponibles.')).toBeInTheDocument();
    });

    expect(
      screen.getByRole('button', { name: /importar flashcards/i })
    ).toBeInTheDocument();
  });

  it('opens import modal when empty state CTA is clicked', async () => {
    vi.mocked(api.apiJson).mockResolvedValue([]);

    render(<FlashcardsPage />);

    await waitFor(() => {
      expect(screen.getByText('No hay materias disponibles.')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /importar flashcards/i }));

    expect(screen.getByTestId('import-modal')).toBeInTheDocument();
  });

  it('opens import modal when header Importar button is clicked', async () => {
    vi.mocked(api.apiJson).mockResolvedValue([]);

    render(<FlashcardsPage />);

    await waitFor(() => {
      expect(screen.getByText('No hay materias disponibles.')).toBeInTheDocument();
    });

    const importButtons = screen.getAllByRole('button', { name: /importar/i });
    fireEvent.click(importButtons[0]);

    expect(screen.getByTestId('import-modal')).toBeInTheDocument();
  });

  it('closes import modal when close is clicked', async () => {
    vi.mocked(api.apiJson).mockResolvedValue([]);

    render(<FlashcardsPage />);

    await waitFor(() => expect(screen.getByText('No hay materias disponibles.')).toBeInTheDocument());

    fireEvent.click(screen.getAllByRole('button', { name: /importar/i })[0]);
    expect(screen.getByTestId('import-modal')).toBeInTheDocument();

    fireEvent.click(screen.getByText('Cerrar modal'));
    expect(screen.queryByTestId('import-modal')).not.toBeInTheDocument();
  });

  it('reloads units when onImported is called from modal', async () => {
    vi.mocked(api.apiJson).mockResolvedValue([]);

    render(<FlashcardsPage />);

    await waitFor(() => expect(screen.getByText('No hay materias disponibles.')).toBeInTheDocument());

    fireEvent.click(screen.getAllByRole('button', { name: /importar/i })[0]);
    fireEvent.click(screen.getByText('Importado'));

    // apiJson should be called again (reload)
    expect(vi.mocked(api.apiJson).mock.calls.length).toBeGreaterThan(2);
  });

  it('shows subjects when data loads', async () => {
    vi.mocked(api.apiJson).mockResolvedValue(mockUnits);

    render(<FlashcardsPage />);

    await waitFor(() => {
      expect(screen.getByText('Matemáticas')).toBeInTheDocument();
    });
  });

  it('shows unit list when subject is selected', async () => {
    vi.mocked(api.apiJson).mockResolvedValue(mockUnits);

    // We need SubjectCard to be real here — don't mock it
    render(<FlashcardsPage />);

    await waitFor(() => expect(screen.getByText('Matemáticas')).toBeInTheDocument());

    // Click on the subject button (the main button inside SubjectCard)
    fireEvent.click(screen.getByText('Matemáticas'));

    // UnitList should now show the units for the selected subject
    await waitFor(() => {
      expect(screen.getByText('Álgebra')).toBeInTheDocument();
    });
  });

  it('shows back button and navigates back when clicked', async () => {
    vi.mocked(api.apiJson).mockResolvedValue(mockUnits);

    render(<FlashcardsPage />);

    await waitFor(() => expect(screen.getByText('Matemáticas')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Matemáticas'));

    await waitFor(() => expect(screen.getByText('Álgebra')).toBeInTheDocument());

    const backButton = screen.getByRole('button', { name: /volver a materias/i });
    fireEvent.click(backButton);

    await waitFor(() => expect(screen.getByText('Matemáticas')).toBeInTheDocument());
    expect(screen.queryByText('Álgebra')).not.toBeInTheDocument();
  });

  it('navigates to study page when unit clicked in study mode', async () => {
    vi.mock('react-router-dom', () => ({
      useNavigate: () => mockNavigate,
      useSearchParams: () => [new URLSearchParams('mode=study')],
    }));

    vi.mocked(api.apiJson).mockResolvedValue(mockUnits);
    render(<FlashcardsPage />);

    await waitFor(() => expect(screen.getByText('Matemáticas')).toBeInTheDocument());
  });

  it('navigates to study page tab when Estudio tab clicked', async () => {
    vi.mocked(api.apiJson).mockResolvedValue([]);

    render(<FlashcardsPage />);

    await waitFor(() => expect(screen.getByTestId('flashcards-tabs')).toBeInTheDocument());

    fireEvent.click(screen.getByText('Estudio'));
    expect(mockNavigate).toHaveBeenCalledWith('/flashcards?mode=study');
  });

  it('navigates to flashcards when Examinar tab clicked', async () => {
    vi.mocked(api.apiJson).mockResolvedValue([]);

    render(<FlashcardsPage />);

    await waitFor(() => expect(screen.getByTestId('flashcards-tabs')).toBeInTheDocument());

    fireEvent.click(screen.getByText('Examinar'));
    expect(mockNavigate).toHaveBeenCalledWith('/flashcards');
  });

  it('navigates to history when Historial tab clicked', async () => {
    vi.mocked(api.apiJson).mockResolvedValue([]);

    render(<FlashcardsPage />);

    await waitFor(() => expect(screen.getByTestId('flashcards-tabs')).toBeInTheDocument());

    fireEvent.click(screen.getByText('Historial'));
    expect(mockNavigate).toHaveBeenCalledWith('/flashcards/history');
  });

  it('shows export error banner when unit export fetch fails', async () => {
    vi.mocked(api.apiJson).mockResolvedValue(mockUnits);
    globalThis.fetch = vi.fn().mockResolvedValue({ ok: false, status: 500 });

    render(<FlashcardsPage />);

    await waitFor(() => expect(screen.getByText('Matemáticas')).toBeInTheDocument());

    // Select subject to show units
    fireEvent.click(screen.getByText('Matemáticas'));

    await waitFor(() => expect(screen.getByText('Álgebra')).toBeInTheDocument());

    // Click CSV export button in UnitCard
    const csvButtons = screen.getAllByTitle('Exportar CSV');
    fireEvent.click(csvButtons[0]);

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });

    expect(screen.getByRole('alert')).toHaveTextContent(/no se pudo exportar/i);
  });

  it('dismisses export error banner when close button is clicked', async () => {
    vi.mocked(api.apiJson).mockResolvedValue(mockUnits);
    globalThis.fetch = vi.fn().mockResolvedValue({ ok: false, status: 500 });

    render(<FlashcardsPage />);

    await waitFor(() => expect(screen.getByText('Matemáticas')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Matemáticas'));
    await waitFor(() => expect(screen.getByText('Álgebra')).toBeInTheDocument());

    const csvButtons = screen.getAllByTitle('Exportar CSV');
    fireEvent.click(csvButtons[0]);

    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: /cerrar error/i }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('calls exportFlashcardsBySubject and creates download link on success', async () => {
    vi.mocked(api.apiJson).mockResolvedValue(mockUnits);
    vi.mocked(api.exportFlashcardsBySubject).mockResolvedValue('front,back\nHello,Hola\n');

    // Spy on link.click without mocking createElement globally
    const realCreateElement = document.createElement.bind(document);
    const clickSpy = vi.fn();
    vi.spyOn(document, 'createElement').mockImplementation((tag: string, ...args: any[]) => {
      const el = realCreateElement(tag, ...args);
      if (tag === 'a') {
        vi.spyOn(el as HTMLAnchorElement, 'click').mockImplementation(clickSpy);
      }
      return el;
    });

    render(<FlashcardsPage />);

    await waitFor(() => expect(screen.getByText('Matemáticas')).toBeInTheDocument());

    // Click CSV export on the SubjectCard
    const csvButtons = screen.getAllByTitle('Exportar CSV');
    fireEvent.click(csvButtons[0]);

    await waitFor(() => {
      expect(api.exportFlashcardsBySubject).toHaveBeenCalledWith('subj-1', 'csv');
    });

    expect(clickSpy).toHaveBeenCalled();
    vi.restoreAllMocks();
  });

  it('shows subject export error banner when exportFlashcardsBySubject fails', async () => {
    vi.mocked(api.apiJson).mockResolvedValue(mockUnits);
    vi.mocked(api.exportFlashcardsBySubject).mockRejectedValue(new Error('api_error'));

    render(<FlashcardsPage />);

    await waitFor(() => expect(screen.getByText('Matemáticas')).toBeInTheDocument());

    const csvButtons = screen.getAllByTitle('Exportar CSV');
    fireEvent.click(csvButtons[0]);

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });

    expect(screen.getByRole('alert')).toHaveTextContent(/no se pudo exportar la materia/i);
  });

  it('filters subjects by search term', async () => {
    vi.mocked(api.apiJson).mockResolvedValue([
      ...mockUnits,
      {
        unitId: 'unit-3',
        unitName: 'Historia Medieval',
        subjectId: 'subj-2',
        subjectName: 'Historia',
        newCount: 0,
        reviewCount: 0,
        dueCount: 0,
      },
    ]);

    render(<FlashcardsPage />);

    await waitFor(() => {
      expect(screen.getByText('Matemáticas')).toBeInTheDocument();
      expect(screen.getByText('Historia')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('search-input'), { target: { value: 'histo' } });

    await waitFor(() => {
      expect(screen.queryByText('Matemáticas')).not.toBeInTheDocument();
      expect(screen.getByText('Historia')).toBeInTheDocument();
    });
  });

  it('filters units by search term when subject is selected', async () => {
    vi.mocked(api.apiJson).mockResolvedValue(mockUnits);

    render(<FlashcardsPage />);

    await waitFor(() => expect(screen.getByText('Matemáticas')).toBeInTheDocument());
    fireEvent.click(screen.getByText('Matemáticas'));
    await waitFor(() => expect(screen.getByText('Álgebra')).toBeInTheDocument());

    fireEvent.change(screen.getByTestId('search-input'), { target: { value: 'álge' } });

    await waitFor(() => {
      expect(screen.getByText('Álgebra')).toBeInTheDocument();
      expect(screen.queryByText('Trigonometría')).not.toBeInTheDocument();
    });
  });
});
