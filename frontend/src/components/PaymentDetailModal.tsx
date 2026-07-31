import { useEffect, useState } from 'react';
import { api, formatUah } from '../api/client';
import type { PaymentDetail, PaymentMatchStatus, PaymentResponse, StudentResponse, TariffPlan } from '../api/types';
import { PaymentEditForm } from './PaymentEditForm';

interface Props {
  studentId: number;
  tariffPlanId: number;
  fullName: string;
  tariffLabel: string;
  year: number;
  month: number;
  students: StudentResponse[];
  tariffPlans: TariffPlan[];
  onClose: () => void;
  onChanged: () => void;
}

const MONTH_NAMES = [
  'січень', 'лютий', 'березень', 'квітень', 'травень', 'червень',
  'липень', 'серпень', 'вересень', 'жовтень', 'листопад', 'грудень',
];

function toPaymentResponse(payment: PaymentDetail, studentId: number, tariffPlanId: number, tariffLabel: string, year: number, month: number): PaymentResponse {
  return {
    id: payment.id,
    studentId,
    tariffPlanId,
    tariffLabel,
    source: payment.source,
    matchStatus: 'MATCHED',
    amountKopiykas: payment.amountKopiykas,
    paymentDate: payment.paymentDate,
    periodYear: year,
    periodMonth: month,
    rawComment: payment.comment,
    parsedPayerName: null,
  };
}

export function PaymentDetailModal({
  studentId, tariffPlanId, fullName, tariffLabel, year, month, students, tariffPlans, onClose, onChanged,
}: Props) {
  const [payments, setPayments] = useState<PaymentDetail[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState<PaymentDetail | null>(null);

  function reload() {
    api.studentPayments(studentId, tariffPlanId, year, month)
      .then(setPayments)
      .catch((e) => setError(e.message));
  }

  useEffect(reload, [studentId, tariffPlanId, year, month]);

  async function handleSave(data: {
    studentId?: number;
    tariffPlanId?: number;
    periodYear?: number;
    periodMonth?: number;
    matchStatus?: PaymentMatchStatus;
  }) {
    if (!editing) return;
    await api.patchPayment(editing.id, data);
    setEditing(null);
    reload();
    onChanged();
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>{fullName} ({tariffLabel}) — {MONTH_NAMES[month - 1]} {year}</h3>
        {error && <div className="error-banner">{error}</div>}
        {!payments && !error && <p className="muted">Завантаження…</p>}
        {payments && payments.length === 0 && <p className="muted">Оплат за цей місяць немає.</p>}
        {payments && payments.length > 0 && (
          <table>
            <thead>
              <tr>
                <th>Дата</th>
                <th>Сума</th>
                <th>Джерело</th>
                <th>Коментар</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {payments.map((payment) => (
                <tr key={payment.id}>
                  <td>{payment.paymentDate}</td>
                  <td>{formatUah(payment.amountKopiykas)} грн</td>
                  <td>{payment.source === 'CASH' ? 'Готівка' : 'Банк'}</td>
                  <td>{payment.comment ?? '—'}</td>
                  <td>
                    <button onClick={() => setEditing(payment)}>Редагувати</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <div className="toolbar" style={{ marginTop: 16 }}>
          <button onClick={onClose}>Закрити</button>
        </div>
      </div>

      {editing && (
        <PaymentEditForm
          payment={toPaymentResponse(editing, studentId, tariffPlanId, tariffLabel, year, month)}
          students={students}
          tariffPlans={tariffPlans}
          onSubmit={handleSave}
          onCancel={() => setEditing(null)}
        />
      )}
    </div>
  );
}
