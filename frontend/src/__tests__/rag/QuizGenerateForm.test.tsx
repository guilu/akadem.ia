import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import QuizGenerateForm from '../../components/rag/QuizGenerateForm';
import * as api from '../../api';
import type { SourceDocument } from '../../types';

vi.mock('../../api', () => ({
  getUnitsForSubject: vi.fn().mockResolvedValue([{ id: 'unit-1', name: 'La Corona' }])
}));

const processedSource: SourceDocument = {
  id: 'src-1',
  name: 'constitucion.pdf',
  type: 'PDF',
  status: 'PROCESSED',
  uploadedAt: '2024-01-01T00:00:00'
};

describe('QuizGenerateForm', () => {
  const onGenerate = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders form fields', async () => {
    render(<QuizGenerateForm sources={[processedSource]} subjectId="subj-1" onGenerate={onGenerate} loading={false} />);
    expect(screen.getByText(/documento fuente/i)).toBeInTheDocument();
    expect(screen.getByText(/unidad temática/i)).toBeInTheDocument();
    expect(screen.getByText(/dificultad/i)).toBeInTheDocument();
  });

  it('shows warning when no processed sources', () => {
    render(<QuizGenerateForm sources={[]} subjectId="subj-1" onGenerate={onGenerate} loading={false} />);
    expect(screen.getByText(/no hay documentos procesados/i)).toBeInTheDocument();
  });

  it('validates missing source and unit', async () => {
    render(<QuizGenerateForm sources={[processedSource]} subjectId="subj-1" onGenerate={onGenerate} loading={false} />);
    fireEvent.click(screen.getByRole('button', { name: /generar preguntas/i }));
    expect(await screen.findByRole('alert')).toHaveTextContent(/selecciona un documento/i);
    expect(onGenerate).not.toHaveBeenCalled();
  });

  it('calls onGenerate with correct command', async () => {
    render(<QuizGenerateForm sources={[processedSource]} subjectId="subj-1" onGenerate={onGenerate} loading={false} />);
    // Wait for units to load
    await waitFor(() => expect(api.getUnitsForSubject).toHaveBeenCalled());
    fireEvent.change(screen.getByRole('combobox', { name: /documento fuente \*/i }), { target: { value: 'src-1' } });
    fireEvent.change(screen.getByRole('combobox', { name: /unidad temática \*/i }), { target: { value: 'unit-1' } });
    fireEvent.click(screen.getByRole('button', { name: /generar preguntas/i }));
    await waitFor(() => expect(onGenerate).toHaveBeenCalledWith(expect.objectContaining({
      sourceId: 'src-1',
      unitId: 'unit-1',
      difficulty: 'MEDIUM',
      questionCount: 5
    })));
  });

  it('disables submit button while loading', () => {
    render(<QuizGenerateForm sources={[processedSource]} subjectId="subj-1" onGenerate={onGenerate} loading={true} />);
    expect(screen.getByRole('button', { name: /generando/i })).toBeDisabled();
  });
});
