import { apiBase, apiJson } from '../api';

export interface CreateIntentResponse {
  clientSecret: string;
  downloadToken: string;
}

export async function createPaymentIntent(
  email: string,
  productId: string
): Promise<CreateIntentResponse> {
  return apiJson<CreateIntentResponse>(`${apiBase}/api/v1/payments/create-intent`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, productId }),
  });
}
