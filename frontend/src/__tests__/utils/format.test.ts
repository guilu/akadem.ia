import { describe, it, expect } from 'vitest';
import { deriveInitials, formatDuration } from '../../utils/format';

describe('formatDuration', () => {
  it('formats 0 seconds as 0m 00s', () => {
    expect(formatDuration(0)).toBe('0m 00s');
  });
  it('formats 90 seconds as 1m 30s', () => {
    expect(formatDuration(90)).toBe('1m 30s');
  });
  it('formats 3661 seconds as 61m 01s', () => {
    expect(formatDuration(3661)).toBe('61m 01s');
  });
  it('pads single-digit seconds with leading zero', () => {
    expect(formatDuration(65)).toBe('1m 05s');
  });
});

describe('deriveInitials', () => {
  it('returns initials from john.doe@example.com', () => {
    expect(deriveInitials('john.doe@example.com')).toBe('JD');
  });
  it('returns initials from admin@akademia.es', () => {
    expect(deriveInitials('admin@akademia.es')).toBe('A');
  });
  it('handles single segment', () => {
    expect(deriveInitials('x@y.com')).toBe('X');
  });
  it('returns empty string for empty input', () => {
    expect(deriveInitials('')).toBe('');
  });
  it('handles dash separator', () => {
    expect(deriveInitials('jane-doe@example.com')).toBe('JD');
  });
});
