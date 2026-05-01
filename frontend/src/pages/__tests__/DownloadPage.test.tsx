import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import DownloadPage from '../DownloadPage';

vi.mock('../../api/downloadApi', async () => {
  const actual = await vi.importActual<typeof import('../../api/downloadApi')>(
    '../../api/downloadApi'
  );
  return {
    ...actual,
    fetchPurchaseInfo: vi.fn(),
    downloadUrl: (token: string) => `/api/v1/downloads/${token}`,
  };
});

import { fetchPurchaseInfo } from '../../api/downloadApi';

const TOKEN = '11111111-1111-1111-1111-111111111111';

function renderAt(token: string = TOKEN) {
  return render(
    <MemoryRouter initialEntries={[`/descarga/${token}`]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <Routes>
        <Route path="/descarga/:token" element={<DownloadPage />} />
      </Routes>
    </MemoryRouter>
  );
}

describe('DownloadPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('PAID purchase → renders download CTA + register link with email param', async () => {
    vi.mocked(fetchPurchaseInfo).mockResolvedValue({
      email: 'buyer@example.com',
      productName: 'Temario Subalterno GVA',
      status: 'PAID',
      amountCents: 1500,
      currency: 'eur',
    });

    renderAt();

    const downloadLink = await screen.findByRole('link', { name: /descargar temario subalterno gva/i });
    expect(downloadLink).toHaveAttribute('href', `/api/v1/downloads/${TOKEN}`);

    const registerLink = screen.getByRole('link', { name: /crear cuenta con buyer@example\.com/i });
    expect(registerLink.getAttribute('href')).toContain('email=buyer%40example.com');
  });

  it('PENDING purchase → shows processing message and no download CTA', async () => {
    vi.mocked(fetchPurchaseInfo).mockResolvedValue({
      email: 'buyer@example.com',
      productName: 'Temario Subalterno GVA',
      status: 'PENDING',
      amountCents: 1500,
      currency: 'eur',
    });

    renderAt();

    await waitFor(() => {
      expect(screen.getByText(/procesando tu pago/i)).toBeInTheDocument();
    });
    expect(screen.queryByRole('link', { name: /descargar/i })).toBeNull();
  });

  it('FAILED purchase → shows failed message and no download CTA', async () => {
    vi.mocked(fetchPurchaseInfo).mockResolvedValue({
      email: 'buyer@example.com',
      productName: 'Temario Subalterno GVA',
      status: 'FAILED',
      amountCents: 1500,
      currency: 'eur',
    });

    renderAt();

    await waitFor(() => {
      expect(screen.getByText(/pago fallido/i)).toBeInTheDocument();
    });
    expect(screen.queryByRole('link', { name: /descargar/i })).toBeNull();
  });

  it('404 from /info → shows "Enlace no válido" with no download CTA', async () => {
    vi.mocked(fetchPurchaseInfo).mockRejectedValue({ status: 404, message: 'api_error' });

    renderAt();

    await waitFor(() => {
      expect(screen.getByText(/enlace no válido/i)).toBeInTheDocument();
    });
    expect(screen.queryByRole('link', { name: /descargar/i })).toBeNull();
  });
});
