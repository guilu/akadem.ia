import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import FlashcardImportModal from '../../components/flashcards/FlashcardImportModal';
import * as api from '../../api';

vi.mock('../../api', async (importOriginal) => {
  const actual = await importOriginal<typeof api>();
  return {
    ...actual,
    apiBase: 'http://localhost:8080',
    apiJson: vi.fn(),
    getUnitsForSubject: vi.fn(),
  };
});

const mockFetch = vi.fn();
vi.stubGlobal('fetch', mockFetch);

const mockSubjects = [
  { id: 'subj-1', name: 'Matemáticas', description: 'Álgebra' },
  { id: 'subj-2', name: 'Física', description: 'Mecánica' },
];

const mockUnits = [
  { id: 'unit-1', name: 'Álgebra' },
  { id: 'unit-2', name: 'Geometría' },
];

describe('FlashcardImportModal', () => {
  const onClose = vi.fn();
  const onImported = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    mockFetch.mockReset();
    vi.mocked(api.apiJson).mockResolvedValue(mockSubjects as any);
    vi.mocked(api.getUnitsForSubject).mockResolvedValue(mockUnits);
  });

  it('renders the modal with subject and unit selectors', async () => {
    render(<FlashcardImportModal onClose={onClose} onImported={onImported} />);

    await waitFor(() => {
      expect(screen.getByText('Importar flashcards')).toBeInTheDocument();
    });
    expect(screen.getByText('Materia')).toBeInTheDocument();
    expect(screen.getByText('Mazo (unidad destino)')).toBeInTheDocument();
  });

  it('loads subjects on mount', async () => {
    render(<FlashcardImportModal onClose={onClose} onImported={onImported} />);

    await waitFor(() => {
      expect(vi.mocked(api.apiJson)).toHaveBeenCalledWith('http://localhost:8080/api/subjects');
    });
    await waitFor(() => {
      expect(screen.getByText('Matemáticas')).toBeInTheDocument();
    });
  });

  it('loads units when subject is selected', async () => {
    render(<FlashcardImportModal onClose={onClose} onImported={onImported} />);

    await waitFor(() => {
      expect(vi.mocked(api.getUnitsForSubject)).toHaveBeenCalledWith('subj-1');
    });
    await waitFor(() => {
      expect(screen.getByText('Álgebra')).toBeInTheDocument();
    });
  });

  it('calls onClose when cancel button is clicked', async () => {
    render(<FlashcardImportModal onClose={onClose} onImported={onImported} />);

    await waitFor(() => screen.getByText('Cancelar'));
    fireEvent.click(screen.getByText('Cancelar'));
    expect(onClose).toHaveBeenCalledOnce();
  });

  it('calls onClose when X button is clicked', async () => {
    render(<FlashcardImportModal onClose={onClose} onImported={onImported} />);

    await waitFor(() => screen.getByText('Importar flashcards'));
    fireEvent.click(screen.getByText('✕'));
    expect(onClose).toHaveBeenCalledOnce();
  });

  it('calls onClose when clicking backdrop', async () => {
    render(<FlashcardImportModal onClose={onClose} onImported={onImported} />);

    await waitFor(() => screen.getByText('Importar flashcards'));
    const backdrop = document.querySelector('.fixed.inset-0') as HTMLElement;
    // Simulate click on the backdrop itself (not the inner modal)
    fireEvent.click(backdrop);
    // backdrop click only triggers when target === currentTarget; test that the handler exists
    expect(onClose).toHaveBeenCalledOnce();
  });

  it('shows error when import is submitted without a file', async () => {
    render(<FlashcardImportModal onClose={onClose} onImported={onImported} />);

    await waitFor(() => screen.getByText('Importar'));
    fireEvent.click(screen.getByText('Importar'));

    await waitFor(() => {
      expect(screen.getByText('Selecciona un archivo para importar.')).toBeInTheDocument();
    });
  });

  it('shows CSV format description', async () => {
    render(<FlashcardImportModal onClose={onClose} onImported={onImported} />);

    await waitFor(() => {
      expect(screen.getByText(/Columnas: front,back/)).toBeInTheDocument();
    });
  });

  it('switches to JSON format when JSON button is clicked', async () => {
    render(<FlashcardImportModal onClose={onClose} onImported={onImported} />);

    await waitFor(() => screen.getByText('JSON'));
    fireEvent.click(screen.getByText('JSON'));

    await waitFor(() => {
      expect(screen.getByText(/Array JSON/)).toBeInTheDocument();
    });
  });

  it('shows error when subjects fail to load', async () => {
    vi.mocked(api.apiJson).mockRejectedValue(new Error('network error'));

    render(<FlashcardImportModal onClose={onClose} onImported={onImported} />);

    await waitFor(() => {
      expect(screen.getByText('No se pudieron cargar las materias.')).toBeInTheDocument();
    });
  });

  it('shows import result after successful import', async () => {
    vi.mocked(api.apiJson).mockResolvedValue(mockSubjects as any);
    vi.mocked(api.getUnitsForSubject).mockResolvedValue(mockUnits);
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ imported: 5, skipped: 1, errors: [] }),
    } as Response);

    render(<FlashcardImportModal onClose={onClose} onImported={onImported} />);

    await waitFor(() => screen.getByText('Importar'));

    // Simulate file upload
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File(['front,back\nHello,Hola'], 'test.csv', { type: 'text/csv' }); file.text = () => Promise.resolve('front,back\nHello,Hola');
    Object.defineProperty(fileInput, 'files', { value: [file], writable: false });
    fireEvent.change(fileInput);

    fireEvent.click(screen.getByText('Importar'));

    await waitFor(() => {
      expect(screen.getByText('5 importadas · 1 omitidas')).toBeInTheDocument();
    });
    expect(onImported).toHaveBeenCalledOnce();
  });

  it('shows error message when import throws', async () => {
    mockFetch.mockResolvedValue({ ok: false, status: 400 } as Response);

    render(<FlashcardImportModal onClose={onClose} onImported={onImported} />);

    await waitFor(() => screen.getByText('Importar'));

    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File(['bad data'], 'test.csv', { type: 'text/csv' }); file.text = () => Promise.resolve('bad data');
    Object.defineProperty(fileInput, 'files', { value: [file], writable: false });
    fireEvent.change(fileInput);

    fireEvent.click(screen.getByText('Importar'));

    await waitFor(() => {
      expect(screen.getByText('Error 400')).toBeInTheDocument();
    });
  });

  it('calls onClose from result state when Cerrar is clicked', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ imported: 3, skipped: 0, errors: [] }),
    } as Response);

    render(<FlashcardImportModal onClose={onClose} onImported={onImported} />);

    await waitFor(() => screen.getByText('Importar'));

    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File(['front,back\nA,B'], 'test.csv', { type: 'text/csv' }); file.text = () => Promise.resolve('front,back\nA,B');
    Object.defineProperty(fileInput, 'files', { value: [file], writable: false });
    fireEvent.change(fileInput);

    fireEvent.click(screen.getByText('Importar'));

    await waitFor(() => screen.getByText('3 importadas · 0 omitidas'));
    fireEvent.click(screen.getByText('Cerrar'));
    expect(onClose).toHaveBeenCalledOnce();
  });
});
