import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import SubjectsPage from '../../pages/SubjectsPage';
import * as api from '../../api';

vi.mock('react-router-dom', () => ({
  Link: ({ children, to, className }: { children: React.ReactNode; to: string; className?: string }) => (
    <a href={to} className={className}>{children}</a>
  ),
}));

vi.mock('../../api', async (importOriginal) => {
  const actual = await importOriginal<typeof api>();
  return {
    ...actual,
    getExamAttempts: vi.fn(),
  };
});

const mockSubjects = [
  { id: 'subj-1', name: 'Matemáticas', description: 'Álgebra y cálculo' },
  { id: 'subj-2', name: 'Física', description: 'Mecánica y termodinámica' },
];

const mockHistory = [
  {
    attemptId: 'att-1',
    subjectName: 'Matemáticas',
    startedAt: '2024-01-10T10:00:00Z',
    finishedAt: '2024-01-10T10:30:00Z',
    score: 8,
    percent: 80,
    totalTimeSeconds: 1800,
  },
  {
    attemptId: 'att-2',
    subjectName: 'Física',
    startedAt: '2024-01-11T10:00:00Z',
    finishedAt: null,
    score: 0,
    percent: 0,
    totalTimeSeconds: 900,
  },
];

describe('SubjectsPage', () => {
  const onUnauthorized = vi.fn();
  const onViewResult = vi.fn();
  const onResumeAttempt = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders subject cards', async () => {
    vi.mocked(api.getExamAttempts).mockResolvedValue([]);

    render(
      <SubjectsPage
        subjects={mockSubjects}
        token="test-token"
        onUnauthorized={onUnauthorized}
        onViewResult={onViewResult}
        onResumeAttempt={onResumeAttempt}
      />
    );

    expect(screen.getByText('Matemáticas')).toBeInTheDocument();
    expect(screen.getByText('Física')).toBeInTheDocument();
    await waitFor(() => expect(vi.mocked(api.getExamAttempts)).toHaveBeenCalledOnce());
  });

  it('shows loading state initially', () => {
    vi.mocked(api.getExamAttempts).mockImplementation(() => new Promise(() => {}));

    render(
      <SubjectsPage
        subjects={mockSubjects}
        token="test-token"
        onUnauthorized={onUnauthorized}
        onViewResult={onViewResult}
        onResumeAttempt={onResumeAttempt}
      />
    );

    expect(screen.getByText('Cargando historial...')).toBeInTheDocument();
  });

  it('shows empty state when no exam history', async () => {
    vi.mocked(api.getExamAttempts).mockResolvedValue([]);

    render(
      <SubjectsPage
        subjects={mockSubjects}
        token="test-token"
        onUnauthorized={onUnauthorized}
        onViewResult={onViewResult}
        onResumeAttempt={onResumeAttempt}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('Aún no tienes exámenes realizados')).toBeInTheDocument();
    });
  });

  it('shows exam history when attempts exist', async () => {
    vi.mocked(api.getExamAttempts).mockResolvedValue(mockHistory as any);

    render(
      <SubjectsPage
        subjects={mockSubjects}
        token="test-token"
        onUnauthorized={onUnauthorized}
        onViewResult={onViewResult}
        onResumeAttempt={onResumeAttempt}
      />
    );

    await waitFor(() => {
      expect(screen.getAllByText('Matemáticas').length).toBeGreaterThan(0);
    });
    expect(screen.getByText('Ver resultados')).toBeInTheDocument();
    expect(screen.getByText('Reanudar')).toBeInTheDocument();
  });

  it('calls onViewResult when "Ver resultados" is clicked', async () => {
    vi.mocked(api.getExamAttempts).mockResolvedValue(mockHistory as any);

    render(
      <SubjectsPage
        subjects={mockSubjects}
        token="test-token"
        onUnauthorized={onUnauthorized}
        onViewResult={onViewResult}
        onResumeAttempt={onResumeAttempt}
      />
    );

    await waitFor(() => screen.getByText('Ver resultados'));
    fireEvent.click(screen.getByText('Ver resultados'));
    expect(onViewResult).toHaveBeenCalledWith('att-1');
  });

  it('calls onResumeAttempt when "Reanudar" is clicked', async () => {
    vi.mocked(api.getExamAttempts).mockResolvedValue(mockHistory as any);

    render(
      <SubjectsPage
        subjects={mockSubjects}
        token="test-token"
        onUnauthorized={onUnauthorized}
        onViewResult={onViewResult}
        onResumeAttempt={onResumeAttempt}
      />
    );

    await waitFor(() => screen.getByText('Reanudar'));
    fireEvent.click(screen.getByText('Reanudar'));
    expect(onResumeAttempt).toHaveBeenCalledWith('att-2');
  });

  it('shows error state when API fails', async () => {
    vi.mocked(api.getExamAttempts).mockRejectedValue(new Error('network error'));

    render(
      <SubjectsPage
        subjects={mockSubjects}
        token="test-token"
        onUnauthorized={onUnauthorized}
        onViewResult={onViewResult}
        onResumeAttempt={onResumeAttempt}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('No se pudo cargar el historial.')).toBeInTheDocument();
    });
  });

  it('calls onUnauthorized when API returns 401', async () => {
    const err = Object.assign(new Error('Unauthorized'), { status: 401 });
    vi.mocked(api.getExamAttempts).mockRejectedValue(err);

    render(
      <SubjectsPage
        subjects={mockSubjects}
        token="test-token"
        onUnauthorized={onUnauthorized}
        onViewResult={onViewResult}
        onResumeAttempt={onResumeAttempt}
      />
    );

    await waitFor(() => {
      expect(onUnauthorized).toHaveBeenCalledOnce();
    });
  });

  it('shows active attempt resume link when activeAttemptId is provided', async () => {
    vi.mocked(api.getExamAttempts).mockResolvedValue([]);

    render(
      <SubjectsPage
        subjects={mockSubjects}
        activeAttemptId="active-123"
        token="test-token"
        onUnauthorized={onUnauthorized}
        onViewResult={onViewResult}
        onResumeAttempt={onResumeAttempt}
      />
    );

    expect(screen.getByText('Reanudar examen')).toBeInTheDocument();
  });

  it('shows "Ver menos" and "Ver más" button for long history lists', async () => {
    const longHistory = Array.from({ length: 8 }, (_, i) => ({
      attemptId: `att-${i}`,
      subjectName: 'Matemáticas',
      startedAt: '2024-01-10T10:00:00Z',
      finishedAt: '2024-01-10T10:30:00Z',
      score: 8,
      percent: 80,
      totalTimeSeconds: 1800,
    }));
    vi.mocked(api.getExamAttempts).mockResolvedValue(longHistory as any);

    render(
      <SubjectsPage
        subjects={mockSubjects}
        token="test-token"
        onUnauthorized={onUnauthorized}
        onViewResult={onViewResult}
        onResumeAttempt={onResumeAttempt}
      />
    );

    await waitFor(() => screen.getByText('Ver 3 más'));
    fireEvent.click(screen.getByText('Ver 3 más'));
    expect(screen.getByText('Ver menos')).toBeInTheDocument();
    fireEvent.click(screen.getByText('Ver menos'));
    expect(screen.getByText('Ver 3 más')).toBeInTheDocument();
  });
});
