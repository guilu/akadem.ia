import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import ExamAttemptPage from '../../pages/ExamAttemptPage';
import * as api from '../../api';

const mockNavigate = vi.fn();
let mockAttemptId: string | undefined = 'attempt-123';

vi.mock('react-router-dom', () => ({
  useParams: () => ({ attemptId: mockAttemptId }),
  useNavigate: () => mockNavigate,
  Navigate: ({ to }: { to: string }) => <div data-testid="navigate" data-to={to} />,
}));

vi.mock('../../components/ExamRunner', () => ({
  default: ({ questions }: { questions: unknown[] }) => (
    <div data-testid="exam-runner">
      <span>Questions: {questions.length}</span>
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

const mockAttemptData = {
  attemptId: 'attempt-123',
  totalTimeSeconds: 1800,
  questions: [
    {
      id: 'q-1',
      text: 'What is 2+2?',
      answers: [
        { id: 'a-1', text: '3' },
        { id: 'a-2', text: '4' },
      ],
      selectedAnswerId: null,
    },
  ],
  nextQuestionIndex: 0,
  finished: false,
};

describe('ExamAttemptPage', () => {
  const onUnauthorized = vi.fn();
  const onFinish = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    mockAttemptId = 'attempt-123';
  });

  it('shows loading state initially', () => {
    vi.mocked(api.apiJson).mockImplementation(() => new Promise(() => {}));

    render(
      <ExamAttemptPage onUnauthorized={onUnauthorized} onFinish={onFinish} />
    );

    expect(screen.getByText('Cargando intento...')).toBeInTheDocument();
  });

  it('renders ExamRunner when attempt loads successfully', async () => {
    vi.mocked(api.apiJson).mockResolvedValue(mockAttemptData);

    render(
      <ExamAttemptPage onUnauthorized={onUnauthorized} onFinish={onFinish} />
    );

    await waitFor(() => {
      expect(screen.getByTestId('exam-runner')).toBeInTheDocument();
    });
    expect(screen.getByText('Questions: 1')).toBeInTheDocument();
  });

  it('shows error state when API fails', async () => {
    vi.mocked(api.apiJson).mockRejectedValue(new Error('network error'));

    render(
      <ExamAttemptPage onUnauthorized={onUnauthorized} onFinish={onFinish} />
    );

    await waitFor(() => {
      expect(screen.getByText('Error al reanudar el intento')).toBeInTheDocument();
    });
  });

  it('calls onUnauthorized when API returns 401', async () => {
    const err = Object.assign(new Error('Unauthorized'), { status: 401 });
    vi.mocked(api.apiJson).mockRejectedValue(err);

    render(
      <ExamAttemptPage onUnauthorized={onUnauthorized} onFinish={onFinish} />
    );

    await waitFor(() => {
      expect(onUnauthorized).toHaveBeenCalledOnce();
    });
  });

  it('shows timeout error message when API times out', async () => {
    const err = Object.assign(new Error('timeout'), { code: 'timeout' });
    vi.mocked(api.apiJson).mockRejectedValue(err);

    render(
      <ExamAttemptPage onUnauthorized={onUnauthorized} onFinish={onFinish} />
    );

    await waitFor(() => {
      expect(screen.getByText('Error al reanudar el intento')).toBeInTheDocument();
    });
  });

  it('redirects to subjects when attemptId is undefined', () => {
    mockAttemptId = undefined;

    render(
      <ExamAttemptPage onUnauthorized={onUnauthorized} onFinish={onFinish} />
    );

    expect(screen.getByTestId('navigate')).toBeInTheDocument();
  });

  it('calls apiJson with correct attempt URL and timeout', async () => {
    vi.mocked(api.apiJson).mockResolvedValue(mockAttemptData);

    render(
      <ExamAttemptPage onUnauthorized={onUnauthorized} onFinish={onFinish} />
    );

    await waitFor(() => {
      expect(vi.mocked(api.apiJson)).toHaveBeenCalledWith(
        'http://localhost:8080/api/exams/attempts/attempt-123',
        { timeoutMs: 15000 }
      );
    });
  });
});
