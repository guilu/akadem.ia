import { apiBase, apiJson } from '../api';

export type PurchaseStatus = 'PENDING' | 'PAID' | 'FAILED';

export interface PurchaseInfoResponse {
  email: string;
  productName: string;
  status: PurchaseStatus;
  amountCents: number;
  currency: string;
}

export function downloadUrl(token: string): string {
  return `${apiBase}/api/v1/downloads/${token}`;
}

export function purchaseInfoUrl(token: string): string {
  return `${apiBase}/api/v1/downloads/${token}/info`;
}

export async function fetchPurchaseInfo(token: string): Promise<PurchaseInfoResponse> {
  return apiJson<PurchaseInfoResponse>(purchaseInfoUrl(token), { method: 'GET' });
}
