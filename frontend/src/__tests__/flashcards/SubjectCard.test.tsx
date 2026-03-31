import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import SubjectCard from '../../components/flashcards/SubjectCard';
import type { SubjectSummary } from '../../components/flashcards/SubjectCard';

const subject: SubjectSummary = {
  subjectId: 'subj-1',
  subjectName: 'Matemáticas',
  newCount: 3,
  reviewCount: 2,
  dueCount: 1,
  unitCount: 2,
};

describe('SubjectCard', () => {
  const onClick = vi.fn();
  const onExport = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders subject name and pending count', () => {
    render(<SubjectCard subject={subject} onClick={onClick} />);
    expect(screen.getByText('Matemáticas')).toBeInTheDocument();
    // pending = dueCount + newCount = 1 + 3 = 4
    expect(screen.getByText('4')).toBeInTheDocument();
  });

  it('calls onClick when button is clicked', () => {
    render(<SubjectCard subject={subject} onClick={onClick} />);
    // The main button contains all text content of the card
    const buttons = screen.getAllByRole('button');
    // The subject card button is the first (and only one when no onExport)
    fireEvent.click(buttons[0]);
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it('does not render export buttons when onExport is not provided', () => {
    render(<SubjectCard subject={subject} onClick={onClick} />);
    expect(screen.queryByTitle('Exportar CSV')).not.toBeInTheDocument();
    expect(screen.queryByTitle('Exportar JSON')).not.toBeInTheDocument();
  });

  it('renders export buttons when onExport is provided', () => {
    render(<SubjectCard subject={subject} onClick={onClick} onExport={onExport} />);
    expect(screen.getByTitle('Exportar CSV')).toBeInTheDocument();
    expect(screen.getByTitle('Exportar JSON')).toBeInTheDocument();
  });

  it('calls onExport with csv when CSV button is clicked', () => {
    render(<SubjectCard subject={subject} onClick={onClick} onExport={onExport} />);
    fireEvent.click(screen.getByTitle('Exportar CSV'));
    expect(onExport).toHaveBeenCalledWith('csv');
    expect(onClick).not.toHaveBeenCalled();
  });

  it('calls onExport with json when JSON button is clicked', () => {
    render(<SubjectCard subject={subject} onClick={onClick} onExport={onExport} />);
    fireEvent.click(screen.getByTitle('Exportar JSON'));
    expect(onExport).toHaveBeenCalledWith('json');
    expect(onClick).not.toHaveBeenCalled();
  });
});
