export function formatDuration(seconds: number) {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins}m ${String(secs).padStart(2, '0')}s`;
}

export function deriveInitials(email: string): string {
  if (!email) return '';
  const local = email.split('@')[0];
  const parts = local.split(/[._-]/);
  return parts
    .slice(0, 2)
    .map(p => p[0]?.toUpperCase() ?? '')
    .join('');
}
