import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import PaymentModal from '../PaymentModal';

vi.mock('../../api/paymentApi', () => ({
  createPaymentIntent: vi.fn(),
}));

vi.mock('../../lib/stripe', () => ({
  stripePromise: Promise.resolve(null),
}));

vi.mock('@stripe/react-stripe-js', () => ({
  Elements: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="stripe-elements">{children}</div>
  ),
  PaymentElement: () => <div data-testid="stripe-payment-element" />,
  useStripe: () => null,
  useElements: () => null,
}));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

import { createPaymentIntent } from '../../api/paymentApi';

function renderModal(productId = 'TEMARIO_SUBALTERNO_GVA') {
  const onClose = vi.fn();
  const result = render(
    <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <PaymentModal isOpen={true} onClose={onClose} productId={productId} />
    </MemoryRouter>
  );
  return { ...result, onClose };
}

describe('PaymentModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('step 1: shows email input and Continuar button', () => {
    renderModal();
    expect(screen.getByPlaceholderText(/introduce tu email/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /continuar/i })).toBeInTheDocument();
  });

  it('Continuar is disabled until a syntactically valid email is entered', () => {
    renderModal();
    const button = screen.getByRole('button', { name: /continuar/i });
    expect(button).toBeDisabled();

    const input = screen.getByPlaceholderText(/introduce tu email/i);
    fireEvent.change(input, { target: { value: 'not-an-email' } });
    expect(button).toBeDisabled();

    fireEvent.change(input, { target: { value: 'buyer@example.com' } });
    expect(button).not.toBeDisabled();
  });

  it('shows mailcheck hint on blur for typo domain and applies correction on click', () => {
    renderModal();
    const input = screen.getByPlaceholderText(/introduce tu email/i) as HTMLInputElement;
    fireEvent.change(input, { target: { value: 'foo@gmial.com' } });
    fireEvent.blur(input);

    const hint = screen.getByRole('button', { name: /foo@gmail\.com/i });
    fireEvent.click(hint);

    expect(input.value).toBe('foo@gmail.com');
  });

  it('after Continuar with valid email, calls createPaymentIntent and renders Stripe Elements', async () => {
    vi.mocked(createPaymentIntent).mockResolvedValue({
      clientSecret: 'pi_test_secret',
      downloadToken: '11111111-1111-1111-1111-111111111111',
    });

    renderModal();
    const input = screen.getByPlaceholderText(/introduce tu email/i);
    fireEvent.change(input, { target: { value: 'buyer@example.com' } });
    fireEvent.click(screen.getByRole('button', { name: /continuar/i }));

    await waitFor(() => {
      expect(createPaymentIntent).toHaveBeenCalledWith('buyer@example.com', 'TEMARIO_SUBALTERNO_GVA');
    });
    expect(await screen.findByTestId('stripe-payment-element')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /pagar 15€/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /volver/i })).toBeInTheDocument();
  });

  it('Volver button returns to email step preserving the email', async () => {
    vi.mocked(createPaymentIntent).mockResolvedValue({
      clientSecret: 'pi_test_secret',
      downloadToken: '11111111-1111-1111-1111-111111111111',
    });

    renderModal();
    fireEvent.change(screen.getByPlaceholderText(/introduce tu email/i), {
      target: { value: 'buyer@example.com' },
    });
    fireEvent.click(screen.getByRole('button', { name: /continuar/i }));
    await screen.findByTestId('stripe-payment-element');

    fireEvent.click(screen.getByRole('button', { name: /volver/i }));

    const input = await screen.findByPlaceholderText(/introduce tu email/i);
    expect((input as HTMLInputElement).value).toBe('buyer@example.com');
  });

  it('on createPaymentIntent failure, returns to email step with an error message', async () => {
    vi.mocked(createPaymentIntent).mockRejectedValue(new Error('boom'));

    renderModal();
    fireEvent.change(screen.getByPlaceholderText(/introduce tu email/i), {
      target: { value: 'buyer@example.com' },
    });
    fireEvent.click(screen.getByRole('button', { name: /continuar/i }));

    expect(await screen.findByText(/no se pudo iniciar el pago/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /continuar/i })).toBeInTheDocument();
  });
});
