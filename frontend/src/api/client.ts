import type {
  DuplicateGroup,
  EnrollmentResponse,
  IngestResult,
  JournalGrid,
  PaymentDetail,
  PaymentResponse,
  ServiceType,
  StudentMergeSummary,
  StudentResponse,
  TariffPlan,
  TariffRate,
} from './types';

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

  studentPayments: (studentId: number, tariffPlanId: number, year: number, month: number) =>
    request<PaymentDetail[]>(`/students/${studentId}/payments?tariffPlanId=${tariffPlanId}&year=${year}&month=${month}`),

  students: () => request<StudentResponse[]>('/students'),

  createStudent: (data: { fullName: string }) =>
    request<StudentResponse>('/students', { method: 'POST', body: JSON.stringify(data) }),

  updateStudent: (id: number, data: { fullName: string; active: boolean }) =>
    request<StudentResponse>(`/students/${id}`, { method: 'PUT', body: JSON.stringify(data) }),

  deactivateStudent: (id: number) => request<void>(`/students/${id}`, { method: 'DELETE' }),

  studentDuplicates: () => request<DuplicateGroup[]>('/students/duplicates'),

  mergeStudents: (studentIds: number[], targetId?: number) =>
    request<StudentResponse>('/students/merge', { method: 'POST', body: JSON.stringify({ studentIds, targetId }) }),

  mergeStudentsAuto: () => request<StudentMergeSummary>('/students/merge/auto', { method: 'POST' }),

  studentEnrollments: (studentId: number) =>
    request<EnrollmentResponse[]>(`/students/${studentId}/enrollments`),

  addEnrollment: (studentId: number, data: { tariffPlanId: number; validFrom?: string }) =>
    request<EnrollmentResponse>(`/students/${studentId}/enrollments`, { method: 'POST', body: JSON.stringify(data) }),

  endEnrollment: (id: number, validTo?: string) =>
    request<EnrollmentResponse>(`/enrollments/${id}/end`, { method: 'POST', body: JSON.stringify({ validTo }) }),

  reactivateEnrollment: (id: number) =>
    request<EnrollmentResponse>(`/enrollments/${id}/reactivate`, { method: 'POST' }),

  addCashPayment: (data: {
    studentId: number;
    tariffPlanId: number;
    amountUah: number;
    paymentDate: string;
    periodYear: number;
    periodMonth: number;
    comment?: string;
  }) => request<PaymentResponse>('/payments/cash', { method: 'POST', body: JSON.stringify(data) }),

  allPayments: () => request<PaymentResponse[]>('/payments'),

  unmatchedPayments: () => request<PaymentResponse[]>('/payments/unmatched'),

  resolvePayment: (id: number, data: { studentId: number; tariffPlanId: number; periodYear: number; periodMonth: number }) =>
    request<PaymentResponse>(`/payments/${id}/resolve`, { method: 'POST', body: JSON.stringify(data) }),

  patchPayment: (id: number, data: { studentId?: number; tariffPlanId?: number; periodYear?: number; periodMonth?: number }) =>
    request<PaymentResponse>(`/payments/${id}`, { method: 'PATCH', body: JSON.stringify(data) }),

  ignorePayment: (id: number) => request<PaymentResponse>(`/payments/${id}/ignore`, { method: 'POST' }),

  tariffPlans: () => request<TariffPlan[]>('/tariffs'),

  createTariffPlan: (data: { serviceType: ServiceType; label: string; initialAmountUah: number; effectiveFrom?: string }) =>
    request<TariffPlan>('/tariffs', { method: 'POST', body: JSON.stringify(data) }),

  updateTariffPlan: (id: number, data: { label: string; active: boolean }) =>
    request<TariffPlan>(`/tariffs/${id}`, { method: 'PUT', body: JSON.stringify(data) }),

  tariffRates: (id: number) => request<TariffRate[]>(`/tariffs/${id}/rates`),

  addTariffRate: (id: number, data: { amountUah: number; effectiveFrom?: string }) =>
    request<TariffRate>(`/tariffs/${id}/rates`, { method: 'POST', body: JSON.stringify(data) }),

  updateTariffRate: (rateId: number, data: { amountUah: number; effectiveFrom: string }) =>
    request<TariffRate>(`/tariffs/rates/${rateId}`, { method: 'PUT', body: JSON.stringify(data) }),

  backfillPayments: (from: string, to: string) =>
    request<IngestResult>(`/admin/ingest/backfill?from=${from}&to=${to}`, { method: 'POST' }),
};

export function formatUah(amountKopiykas: number): string {
  return (amountKopiykas / 100).toLocaleString('uk-UA', { maximumFractionDigits: 2 });
}
