import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import SourceUpload from '../../components/rag/SourceUpload';
import * as api from '../../api';
import type { IndexPreview } from '../../types';

vi.mock('../../api', () => ({
  uploadSource: vi.fn(),
  confirmIndex: vi.fn()
}));

const mockPreview: IndexPreview = {
  document: {
    id: 'doc-1',
    name: 'test.pdf',
    type: 'PDF',
    status: 'PENDING_REVIEW',
    uploadedAt: '2024-01-01T00:00:00'
  },
  detectedUnits: []
};

describe('SourceUpload', () => {
  const onUploaded = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the upload zone', () => {
    render(<SourceUpload token="tok" subjectId="subj-1" onUploaded={onUploaded} />);
    expect(screen.getByRole('button', { name: /zona de subida/i })).toBeInTheDocument();
    expect(screen.getByText(/arrastra un pdf/i)).toBeInTheDocument();
  });

  it('rejects non-PDF files', async () => {
    render(<SourceUpload token="tok" subjectId="subj-1" onUploaded={onUploaded} />);
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File(['content'], 'doc.docx', { type: 'application/msword' });
    Object.defineProperty(input, 'files', { value: [file] });
    fireEvent.change(input);
    expect(await screen.findByRole('alert')).toHaveTextContent(/solo se admiten archivos pdf/i);
    expect(api.uploadSource).not.toHaveBeenCalled();
  });

  it('rejects files over 50 MB', async () => {
    render(<SourceUpload token="tok" subjectId="subj-1" onUploaded={onUploaded} />);
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    const bigContent = new Uint8Array(51 * 1024 * 1024);
    const file = new File([bigContent], 'big.pdf', { type: 'application/pdf' });
    Object.defineProperty(input, 'files', { value: [file] });
    fireEvent.change(input);
    expect(await screen.findByRole('alert')).toHaveTextContent(/50 mb/i);
    expect(api.uploadSource).not.toHaveBeenCalled();
  });

  it('calls uploadSource and shows preview for valid PDF', async () => {
    vi.mocked(api.uploadSource).mockResolvedValue(mockPreview);
    render(<SourceUpload token="tok" subjectId="subj-1" onUploaded={onUploaded} />);
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File(['%PDF-1.4'], 'test.pdf', { type: 'application/pdf' });
    Object.defineProperty(input, 'files', { value: [file] });
    fireEvent.change(input);
    await waitFor(() => expect(api.uploadSource).toHaveBeenCalledWith('tok', file, 'subj-1'));
    // After upload shows the index preview
    expect(await screen.findByText(/temas detectados/i)).toBeInTheDocument();
  });

  it('shows error when upload fails', async () => {
    const err: any = new Error('api_error');
    err.body = { message: 'Server error' };
    vi.mocked(api.uploadSource).mockRejectedValue(err);
    render(<SourceUpload token="tok" subjectId="subj-1" onUploaded={onUploaded} />);
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File(['%PDF'], 'fail.pdf', { type: 'application/pdf' });
    Object.defineProperty(input, 'files', { value: [file] });
    fireEvent.change(input);
    expect(await screen.findByRole('alert')).toHaveTextContent(/server error/i);
  });

  it('shows error and calls onUploaded when server returns FAILED status', async () => {
    const failedPreview: IndexPreview = {
      document: { ...mockPreview.document, status: 'FAILED' },
      detectedUnits: []
    };
    vi.mocked(api.uploadSource).mockResolvedValue(failedPreview);
    render(<SourceUpload token="tok" subjectId="subj-1" onUploaded={onUploaded} />);
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File(['%PDF'], 'fail.pdf', { type: 'application/pdf' });
    Object.defineProperty(input, 'files', { value: [file] });
    fireEvent.change(input);
    await waitFor(() => expect(onUploaded).toHaveBeenCalledWith(failedPreview.document));
    expect(await screen.findByRole('alert')).toHaveTextContent(/no pudo procesarse/i);
  });
});
