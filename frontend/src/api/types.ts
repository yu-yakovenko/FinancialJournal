export type ServiceType = 'INDIVIDUAL' | 'CHOIR';

export const SERVICE_TYPE_LABELS: Record<ServiceType, string> = {
  INDIVIDUAL: 'Індивідуальні заняття',
  CHOIR: 'Хор',
};

export interface TariffPlan {
  id: number;
  serviceType: ServiceType;
  label: string;
  active: boolean;
  currentAmountKopiykas: number | null;
}

export interface TariffRate {
  id: number;
  amountKopiykas: number;
  effectiveFrom: string;
}

export interface EnrollmentResponse {
  id: number;
  studentId: number;
  tariffPlanId: number;
  tariffLabel: string;
  validFrom: string;
  validTo: string | null;
  active: boolean;
}

export type CellStatus = 'GREEN' | 'YELLOW' | 'RED';

export interface JournalCell {
  amountKopiykas: number;
  expectedAmountKopiykas: number | null;
  status: CellStatus;
}

/** One row per (student, tariff enrollment) — a student on two tariffs gets two rows. */
export interface JournalRow {
  studentId: number;
  fullName: string;
  tariffPlanId: number | null;
  tariffLabel: string | null;
  cells: (JournalCell | null)[];
}

export interface JournalGrid {
  year: number;
  rows: JournalRow[];
}

export type PaymentSource = 'BANK' | 'CASH';
export type PaymentMatchStatus = 'MATCHED' | 'NEEDS_REVIEW' | 'IGNORED';

export interface PaymentDetail {
  id: number;
  source: PaymentSource;
  amountKopiykas: number;
  paymentDate: string;
  comment: string | null;
}

export interface StudentResponse {
  id: number;
  fullName: string;
  activeTariffLabels: string[];
  active: boolean;
  createdAt: string;
}

export interface PaymentResponse {
  id: number;
  studentId: number | null;
  tariffPlanId: number | null;
  tariffLabel: string | null;
  source: PaymentSource;
  matchStatus: PaymentMatchStatus;
  amountKopiykas: number;
  paymentDate: string;
  periodYear: number | null;
  periodMonth: number | null;
  rawComment: string | null;
  parsedPayerName: string | null;
}
