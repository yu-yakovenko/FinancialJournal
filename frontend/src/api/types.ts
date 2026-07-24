export type Tariff = 'INDIVIDUAL_500' | 'INDIVIDUAL_700' | 'CHOIR_VPO_680' | 'CHOIR_STANDARD_1700';

export const TARIFFS: Tariff[] = ['INDIVIDUAL_500', 'INDIVIDUAL_700', 'CHOIR_VPO_680', 'CHOIR_STANDARD_1700'];

export const TARIFF_LABELS: Record<Tariff, string> = {
  INDIVIDUAL_500: 'Індивідуальні, 500 грн',
  INDIVIDUAL_700: 'Індивідуальні, 700 грн',
  CHOIR_VPO_680: 'Хор (ВПО), 680 грн',
  CHOIR_STANDARD_1700: 'Хор, 1700 грн',
};

export type CellStatus = 'GREEN' | 'YELLOW' | 'RED';

export interface JournalCell {
  amountKopiykas: number;
  status: CellStatus;
}

export interface JournalRow {
  studentId: number;
  fullName: string;
  tariffLabel: Tariff | null;
  tariffAmountKopiykas: number | null;
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
  tariff: Tariff | null;
  tariffAmountKopiykas: number | null;
  active: boolean;
  createdAt: string;
}

export interface PaymentResponse {
  id: number;
  studentId: number | null;
  source: PaymentSource;
  matchStatus: PaymentMatchStatus;
  amountKopiykas: number;
  paymentDate: string;
  periodYear: number | null;
  periodMonth: number | null;
  rawComment: string | null;
  parsedPayerName: string | null;
}
