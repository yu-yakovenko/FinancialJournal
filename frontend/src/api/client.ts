import type { JournalGrid, PaymentDetail, PaymentResponse, StudentResponse, Tariff } from './types';

const BASE = '/api';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.error ?? `Помилка запиту: ${response.status}`);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

export const api = {
  journal: (year: number) => request<JournalGrid>(`/journal?year=${year}`),

  studentPayments: (studentId: number, year: number, month: number) =>
    request<PaymentDetail[]>(`/students/${studentId}/payments?year=${year}&month=${month}`),

  students: () => request<StudentResponse[]>('/students'),

  createStudent: (data: { fullName: string; tariff: Tariff | null }) =>
    request<StudentResponse>('/students', { method: 'POST', body: JSON.stringify(data) }),

  updateStudent: (id: number, data: { fullName: string; tariff: Tariff | null; active: boolean }) =>
    request<StudentResponse>(`/students/${id}`, { method: 'PUT', body: JSON.stringify(data) }),

  deactivateStudent: (id: number) => request<void>(`/students/${id}`, { method: 'DELETE' }),

  addCashPayment: (data: {
    studentId: number;
    amountUah: number;
    paymentDate: string;
    periodYear: number;
    periodMonth: number;
    comment?: string;
  }) => request<PaymentResponse>('/payments/cash', { method: 'POST', body: JSON.stringify(data) }),

  unmatchedPayments: () => request<PaymentResponse[]>('/payments/unmatched'),

  resolvePayment: (id: number, data: { studentId: number; periodYear: number; periodMonth: number }) =>
    request<PaymentResponse>(`/payments/${id}/resolve`, { method: 'POST', body: JSON.stringify(data) }),

  ignorePayment: (id: number) => request<PaymentResponse>(`/payments/${id}/ignore`, { method: 'POST' }),
};

export function formatUah(amountKopiykas: number): string {
  return (amountKopiykas / 100).toLocaleString('uk-UA', { maximumFractionDigits: 2 });
}
