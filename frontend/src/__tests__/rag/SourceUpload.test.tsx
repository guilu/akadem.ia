import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import SourceUpload from '../../components/rag/SourceUpload';
import * as api from '../../api';

vi.mock('../../api', () => ({
  uploadSource: vi.fn()
}));

const mockDoc = {
  id: 'doc-1',
  name: 'test.pdf',
  type: 'PDF',
  status: 'PROCESSED' as const,
  uploadedAt: '2024-01-01T00:00:00'
};

describe('SourceUpload', () => {
  const onUploaded = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the upload zone', () => {
    render(<SourceUpload token="tok" onUploaded={onUploaded} />);
    expect(screen.getByRole('button', { name: /zona de subida/i })).toBeInTheDocument();
    expect(screen.getByText(/arrastra un pdf/i)).toBeInTheDocument();
  });

  it('rejects non-PDF files', async () => {
    render(<SourceUpload token="tok" onUploaded={onUploaded} />);
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File(['content'], 'doc.docx', { type: 'application/msword' });
    Object.defineProperty(input, 'files', { value: [file] });
    fireEvent.change(input);
    expect(await screen.findByRole('alert')).toHaveTextContent(/solo se admiten archivos pdf/i);
    expect(api.uploadSource).not.toHaveBeenCalled();
  });

  it('rejects files over 50 MB', async () => {
    render(<SourceUpload token="tok" onUploaded={onUploaded} />);
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    const bigContent = new Uint8Array(51 * 1024 * 1024);
    const file = new File([bigContent], 'big.pdf', { type: 'application/pdf' });
    Object.defineProperty(input, 'files', { value: [file] });
    fireEvent.change(input);
    expect(await screen.findByRole('alert')).toHaveTextContent(/50 mb/i);
    expect(api.uploadSource).not.toHaveBeenCalled();
  });

  it('calls uploadSource and onUploaded for valid PDF', async () => {
    vi.mocked(api.uploadSource).mockResolvedValue(mockDoc);
    render(<SourceUpload token="tok" onUploaded={onUploaded} />);
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File(['%PDF-1.4'], 'test.pdf', { type: 'application/pdf' });
    Object.defineProperty(input, 'files', { value: [file] });
    fireEvent.change(input);
    await waitFor(() => expect(api.uploadSource).toHaveBeenCalledWith('tok', file));
    await waitFor(() => expect(onUploaded).toHaveBeenCalledWith(mockDoc));
  });

  it('shows error when upload fails', async () => {
    const err: any = new Error('api_error');
    err.body = { message: 'Server error' };
    vi.mocked(api.uploadSource).mockRejectedValue(err);
    render(<SourceUpload token="tok" onUploaded={onUploaded} />);
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File(['%PDF'], 'fail.pdf', { type: 'application/pdf' });
    Object.defineProperty(input, 'files', { value: [file] });
    fireEvent.change(input);
    expect(await screen.findByRole('alert')).toHaveTextContent(/server error/i);
  });

  it('shows error and still calls onUploaded when server returns FAILED status', async () => {
    const failedDoc = { ...mockDoc, status: 'FAILED' as const };
    vi.mocked(api.uploadSource).mockResolvedValue(failedDoc);
    render(<SourceUpload token="tok" onUploaded={onUploaded} />);
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File(['%PDF'], 'fail.pdf', { type: 'application/pdf' });
    Object.defineProperty(input, 'files', { value: [file] });
    fireEvent.change(input);
    await waitFor(() => expect(onUploaded).toHaveBeenCalledWith(failedDoc));
    expect(await screen.findByRole('alert')).toHaveTextContent(/no pudo procesarse/i);
  });
});
