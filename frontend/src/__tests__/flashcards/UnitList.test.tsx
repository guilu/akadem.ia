import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import UnitList from '../../components/flashcards/UnitList';
import type { UnitSummary } from '../../pages/FlashcardsPage';

vi.mock('react-router-dom', () => ({
  useNavigate: () => vi.fn(),
}));

const units: UnitSummary[] = [
  {
    unitId: 'unit-1',
    unitName: 'Álgebra',
    subjectId: 'subj-1',
    subjectName: 'Matemáticas',
    syllabusId: null,
    syllabusName: null,
    newCount: 5,
    reviewCount: 2,
    dueCount: 1,
  },
];

describe('UnitList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows skeleton while loading', () => {
    const { container } = render(
      <UnitList units={[]} loading={true} error="" />
    );
    const skeletons = container.querySelectorAll('.animate-pulse');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('shows error message when error is provided', () => {
    render(<UnitList units={[]} loading={false} error="Error cargando unidades" />);
    expect(screen.getByText('Error cargando unidades')).toBeInTheDocument();
  });

  it('shows empty state when no units', () => {
    render(<UnitList units={[]} loading={false} error="" />);
    expect(screen.getByText('No hay unidades disponibles.')).toBeInTheDocument();
  });

  it('empty state does not show import CTA when onImport is not provided', () => {
    render(<UnitList units={[]} loading={false} error="" />);
    expect(screen.queryByRole('button', { name: /importar flashcards/i })).not.toBeInTheDocument();
  });

  it('empty state shows import CTA when onImport is provided', () => {
    const onImport = vi.fn();
    render(<UnitList units={[]} loading={false} error="" onImport={onImport} />);
    expect(screen.getByRole('button', { name: /importar flashcards/i })).toBeInTheDocument();
  });

  it('empty state CTA calls onImport when clicked', () => {
    const onImport = vi.fn();
    render(<UnitList units={[]} loading={false} error="" onImport={onImport} />);
    fireEvent.click(screen.getByRole('button', { name: /importar flashcards/i }));
    expect(onImport).toHaveBeenCalledTimes(1);
  });

  it('renders unit cards when units are provided', () => {
    render(<UnitList units={units} loading={false} error="" />);
    expect(screen.getByText('Álgebra')).toBeInTheDocument();
  });
});
