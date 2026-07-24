import { useEffect, useState } from 'react';
import { api, formatUah } from '../api/client';
import type { PaymentDetail } from '../api/types';

interface Props {
  studentId: number;
  fullName: string;
  year: number;
  month: number;
  onClose: () => void;
}

const MONTH_NAMES = [
  'січень', 'лютий', 'березень', 'квітень', 'травень', 'червень',
  'липень', 'серпень', 'вересень', 'жовтень', 'листопад', 'грудень',
];

export function PaymentDetailModal({ studentId, fullName, year, month, onClose }: Props) {
  const [payments, setPayments] = useState<PaymentDetail[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.studentPayments(studentId, year, month)
      .then(setPayments)
      .catch((e) => setError(e.message));
  }, [studentId, year, month]);

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>{fullName} — {MONTH_NAMES[month - 1]} {year}</h3>
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
              </tr>
            </thead>
            <tbody>
              {payments.map((payment) => (
                <tr key={payment.id}>
                  <td>{payment.paymentDate}</td>
                  <td>{formatUah(payment.amountKopiykas)} грн</td>
                  <td>{payment.source === 'CASH' ? 'Готівка' : 'Банк'}</td>
                  <td>{payment.comment ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <div className="toolbar" style={{ marginTop: 16 }}>
          <button onClick={onClose}>Закрити</button>
        </div>
      </div>
    </div>
  );
}
