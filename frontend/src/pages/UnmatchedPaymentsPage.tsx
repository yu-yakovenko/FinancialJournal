import { useEffect, useState } from 'react';
import { api, formatUah } from '../api/client';
import type { PaymentResponse, StudentResponse, TariffPlan } from '../api/types';

const MONTH_NAMES = [
  'січень', 'лютий', 'березень', 'квітень', 'травень', 'червень',
  'липень', 'серпень', 'вересень', 'жовтень', 'листопад', 'грудень',
];

export function UnmatchedPaymentsPage() {
  const [payments, setPayments] = useState<PaymentResponse[]>([]);
  const [students, setStudents] = useState<StudentResponse[]>([]);
  const [tariffPlans, setTariffPlans] = useState<TariffPlan[]>([]);
  const [error, setError] = useState<string | null>(null);

  function reload() {
    api.unmatchedPayments().then(setPayments).catch((e) => setError(e.message));
    api.students().then(setStudents).catch((e) => setError(e.message));
    api.tariffPlans().then(setTariffPlans).catch((e) => setError(e.message));
  }

  useEffect(reload, []);

  return (
    <div>
      <h2>Неопрацьовані платежі</h2>
      <p className="muted">
        Платежі, коментар яких не вдалося розпізнати, сума яких не відповідає рівно одному тарифу (напр. часткова
        оплата), або платника не вдалося однозначно зіставити зі студентом.
      </p>
      {error && <div className="error-banner">{error}</div>}

      {payments.length === 0 && <div className="empty-state">Немає платежів, що потребують уваги.</div>}

      {payments.map((payment) => (
        <UnmatchedPaymentRow
          key={payment.id}
          payment={payment}
          students={students}
          tariffPlans={tariffPlans}
          onResolved={reload}
        />
      ))}
    </div>
  );
}

function UnmatchedPaymentRow({
  payment,
  students,
  tariffPlans,
  onResolved,
}: {
  payment: PaymentResponse;
  students: StudentResponse[];
  tariffPlans: TariffPlan[];
  onResolved: () => void;
}) {
  const now = new Date();
  const [studentId, setStudentId] = useState<number | ''>('');
  const [tariffPlanId, setTariffPlanId] = useState<number | ''>('');
  const [periodYear, setPeriodYear] = useState(payment.periodYear ?? now.getFullYear());
  const [periodMonth, setPeriodMonth] = useState(payment.periodMonth ?? now.getMonth() + 1);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function resolve() {
    if (!studentId) {
      setError('Оберіть студента');
      return;
    }
    if (!tariffPlanId) {
      setError('Оберіть тариф');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await api.resolvePayment(payment.id, { studentId, tariffPlanId, periodYear, periodMonth });
      onResolved();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setBusy(false);
    }
  }

  async function ignore() {
    setBusy(true);
    setError(null);
    try {
      await api.ignorePayment(payment.id);
      onResolved();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="card" style={{ maxWidth: 700 }}>
      <div>
        <strong>{formatUah(payment.amountKopiykas)} грн</strong> · {payment.paymentDate} · {payment.source === 'CASH' ? 'Готівка' : 'Банк'}
      </div>
      <div className="muted" style={{ margin: '4px 0 12px' }}>
        Коментар: {payment.rawComment ?? '—'}
        {payment.parsedPayerName && <> · Розпізнане ім'я: {payment.parsedPayerName}</>}
      </div>
      {error && <div className="error-banner">{error}</div>}
      <div className="toolbar">
        <select value={studentId} onChange={(e) => setStudentId(e.target.value ? Number(e.target.value) : '')}>
          <option value="">Оберіть студента</option>
          {[...students].sort((a, b) => a.fullName.localeCompare(b.fullName, 'uk')).map((s) => (
            <option key={s.id} value={s.id}>{s.fullName}</option>
          ))}
        </select>
        <select value={tariffPlanId} onChange={(e) => setTariffPlanId(e.target.value ? Number(e.target.value) : '')}>
          <option value="">Оберіть тариф</option>
          {tariffPlans.filter((p) => p.active).map((p) => (
            <option key={p.id} value={p.id}>{p.label}</option>
          ))}
        </select>
        <select value={periodMonth} onChange={(e) => setPeriodMonth(Number(e.target.value))}>
          {MONTH_NAMES.map((name, index) => (
            <option key={name} value={index + 1}>{name}</option>
          ))}
        </select>
        <input
          type="number"
          value={periodYear}
          onChange={(e) => setPeriodYear(Number(e.target.value))}
          style={{ width: 90 }}
        />
        <button className="primary" disabled={busy} onClick={resolve}>Зарахувати</button>
        <button disabled={busy} onClick={ignore}>Ігнорувати</button>
      </div>
    </div>
  );
}
