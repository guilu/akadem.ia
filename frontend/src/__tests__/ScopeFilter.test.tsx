import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import ScopeFilter from '../components/ScopeFilter';
import type { ContentScope } from '../components/ScopeFilter';

describe('ScopeFilter — non-admin', () => {
  it('renders Todo and Personal buttons only', () => {
    const onChange = vi.fn();
    render(<ScopeFilter scope="ALL" onChange={onChange} isAdmin={false} />);

    expect(screen.getByRole('button', { name: 'Todo' })).toBeDefined();
    expect(screen.getByRole('button', { name: 'Personal' })).toBeDefined();
    expect(screen.queryByRole('button', { name: 'Global' })).toBeNull();
  });

  it('calls onChange with PRIVATE when Personal is clicked', () => {
    const onChange = vi.fn();
    render(<ScopeFilter scope="ALL" onChange={onChange} isAdmin={false} />);

    fireEvent.click(screen.getByRole('button', { name: 'Personal' }));
    expect(onChange).toHaveBeenCalledWith('PRIVATE');
  });

  it('calls onChange with ALL when Todo is clicked', () => {
    const onChange = vi.fn();
    render(<ScopeFilter scope="PRIVATE" onChange={onChange} isAdmin={false} />);

    fireEvent.click(screen.getByRole('button', { name: 'Todo' }));
    expect(onChange).toHaveBeenCalledWith('ALL');
  });
});

describe('ScopeFilter — admin', () => {
  it('renders Todo, Global and Personal buttons', () => {
    const onChange = vi.fn();
    render(<ScopeFilter scope="ALL" onChange={onChange} isAdmin={true} />);

    expect(screen.getByRole('button', { name: 'Todo' })).toBeDefined();
    expect(screen.getByRole('button', { name: 'Global' })).toBeDefined();
    expect(screen.getByRole('button', { name: 'Personal' })).toBeDefined();
  });

  it('calls onChange with GLOBAL when Global is clicked', () => {
    const onChange = vi.fn();
    render(<ScopeFilter scope="ALL" onChange={onChange} isAdmin={true} />);

    fireEvent.click(screen.getByRole('button', { name: 'Global' }));
    expect(onChange).toHaveBeenCalledWith('GLOBAL');
  });

  it('highlights active scope button', () => {
    const onChange = vi.fn();
    render(<ScopeFilter scope="GLOBAL" onChange={onChange} isAdmin={true} />);

    const globalBtn = screen.getByRole('button', { name: 'Global' });
    expect(globalBtn.className).toContain('bg-primary');

    const allBtn = screen.getByRole('button', { name: 'Todo' });
    expect(allBtn.className).not.toContain('bg-primary');
  });
});
