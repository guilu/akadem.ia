import { apiBase, apiJson } from '../api';

export async function createPaymentIntent(): Promise<{ clientSecret: string }> {
  return apiJson<{ clientSecret: string }>(`${apiBase}/api/v1/payments/create-intent`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  });
}
