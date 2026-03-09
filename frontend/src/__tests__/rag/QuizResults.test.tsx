import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import QuizResults from '../../components/rag/QuizResults';
import type { GenerateQuizResponse } from '../../types';

const mockResult: GenerateQuizResponse = {
  generated: 2,
  questions: [
    {
      id: 'q-1',
      sourceDocumentId: 'src-1',
      topic: 'La Corona',
      difficulty: 'MEDIUM',
      statement: '¿Quién es el Jefe del Estado?',
      answers: ['El Rey', 'El Presidente', 'El Parlamento', 'El Gobierno'],
      correctIndex: 0,
      hint: 'Art. 56 CE',
      explanation: 'La Constitución establece que el Rey es el Jefe del Estado.',
      reference: 'Artículo 56',
      status: 'GENERATED'
    },
    {
      id: 'q-2',
      sourceDocumentId: 'src-1',
      topic: 'La Corona',
      difficulty: 'EASY',
      statement: '¿Cuánto dura el mandato del monarca?',
      answers: ['Vitalicio', '4 años', '5 años', '6 años'],
      correctIndex: 0,
      hint: null,
      explanation: null,
      reference: null,
      status: 'GENERATED'
    }
  ]
};

describe('QuizResults', () => {
  it('renders generated count and questions', () => {
    render(<QuizResults result={mockResult} onReset={() => {}} />);
    expect(screen.getByText(/2 preguntas generadas/i)).toBeInTheDocument();
    expect(screen.getByText(/¿Quién es el Jefe del Estado\?/i)).toBeInTheDocument();
    expect(screen.getByText(/¿Cuánto dura el mandato/i)).toBeInTheDocument();
  });

  it('highlights correct answers', () => {
    render(<QuizResults result={mockResult} onReset={() => {}} />);
    const correct = screen.getAllByText('El Rey')[0].closest('li');
    expect(correct).toHaveClass('bg-green-400/15');
  });

  it('shows explanation when present', () => {
    render(<QuizResults result={mockResult} onReset={() => {}} />);
    expect(screen.getByText(/la constitución establece/i)).toBeInTheDocument();
  });

  it('shows reference when present', () => {
    render(<QuizResults result={mockResult} onReset={() => {}} />);
    expect(screen.getByText('Artículo 56')).toBeInTheDocument();
  });

  it('calls onReset when button clicked', () => {
    const onReset = vi.fn();
    render(<QuizResults result={mockResult} onReset={onReset} />);
    fireEvent.click(screen.getByRole('button', { name: /nueva generación/i }));
    expect(onReset).toHaveBeenCalledOnce();
  });

  it('shows empty state message when no questions', () => {
    render(<QuizResults result={{ generated: 0, questions: [] }} onReset={() => {}} />);
    expect(screen.getByText(/no generó preguntas válidas/i)).toBeInTheDocument();
  });
});
