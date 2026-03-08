import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import QuizGenerateForm from '../../components/rag/QuizGenerateForm';
import * as api from '../../api';
import type { SourceDocument, Subject } from '../../types';

vi.mock('../../api', () => ({
  getUnitsForSubject: vi.fn().mockResolvedValue([])
}));

const processedSource: SourceDocument = {
  id: 'src-1',
  name: 'constitucion.pdf',
  type: 'PDF',
  status: 'PROCESSED',
  uploadedAt: '2024-01-01T00:00:00'
};

const subjects: Subject[] = [{ id: 'subj-1', name: 'Derecho Constitucional' }];

describe('QuizGenerateForm', () => {
  const onGenerate = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders form fields', () => {
    render(<QuizGenerateForm token="tok" sources={[processedSource]} subjects={subjects} onGenerate={onGenerate} loading={false} />);
    expect(screen.getByText(/documento fuente/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/la corona/i)).toBeInTheDocument();
    expect(screen.getByText(/dificultad/i)).toBeInTheDocument();
  });

  it('shows warning when no processed sources', () => {
    render(<QuizGenerateForm token="tok" sources={[]} subjects={[]} onGenerate={onGenerate} loading={false} />);
    expect(screen.getByText(/no hay documentos procesados/i)).toBeInTheDocument();
  });

  it('validates empty topic', async () => {
    render(<QuizGenerateForm token="tok" sources={[processedSource]} subjects={[]} onGenerate={onGenerate} loading={false} />);
    // Submit without filling topic (topic is checked first)
    fireEvent.click(screen.getByRole('button', { name: /generar preguntas/i }));
    expect(await screen.findByRole('alert')).toHaveTextContent(/introduce el tema/i);
    expect(onGenerate).not.toHaveBeenCalled();
  });

  it('calls onGenerate with correct command', async () => {
    render(<QuizGenerateForm token="tok" sources={[processedSource]} subjects={[]} onGenerate={onGenerate} loading={false} />);
    fireEvent.change(screen.getByRole('combobox', { name: /documento fuente \*/i }), { target: { value: 'src-1' } });
    fireEvent.change(screen.getByPlaceholderText(/la corona/i), { target: { value: 'La Corona' } });
    fireEvent.click(screen.getByRole('button', { name: /generar preguntas/i }));
    await waitFor(() => expect(onGenerate).toHaveBeenCalledWith(expect.objectContaining({
      sourceId: 'src-1',
      topic: 'La Corona',
      difficulty: 'MEDIUM',
      questionCount: 5
    })));
  });

  it('disables submit button while loading', () => {
    render(<QuizGenerateForm token="tok" sources={[processedSource]} subjects={[]} onGenerate={onGenerate} loading={true} />);
    expect(screen.getByRole('button', { name: /generando/i })).toBeDisabled();
  });
});
