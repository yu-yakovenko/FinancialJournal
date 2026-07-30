import { useState } from 'react';
import type { FormEvent } from 'react';
import { formatUah } from '../api/client';
import type { PaymentResponse, StudentResponse, TariffPlan } from '../api/types';

const MONTH_NAMES = [
  'січень', 'лютий', 'березень', 'квітень', 'травень', 'червень',
  'липень', 'серпень', 'вересень', 'жовтень', 'листопад', 'грудень',
];

interface Props {
  payment: PaymentResponse;
  students: StudentResponse[];
  tariffPlans: TariffPlan[];
  onSubmit: (data: { studentId?: number; tariffPlanId?: number; periodYear?: number; periodMonth?: number }) => Promise<void>;
  onCancel: () => void;
}

export function PaymentEditForm({ payment, students, tariffPlans, onSubmit, onCancel }: Props) {
  const now = new Date();
  const [studentId, setStudentId] = useState<number | ''>(payment.studentId ?? '');
  const [tariffPlanId, setTariffPlanId] = useState<number | ''>(payment.tariffPlanId ?? '');
  const [periodYear, setPeriodYear] = useState(payment.periodYear ?? now.getFullYear());
  const [periodMonth, setPeriodMonth] = useState(payment.periodMonth ?? now.getMonth() + 1);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await onSubmit({
        studentId: studentId === '' ? undefined : studentId,
        tariffPlanId: tariffPlanId === '' ? undefined : tariffPlanId,
        periodYear,
        periodMonth,
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <form className="modal" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
        <h3>Редагувати платіж</h3>
        <p className="muted">
          {formatUah(payment.amountKopiykas)} грн · {payment.paymentDate} · {payment.source === 'CASH' ? 'Готівка' : 'Банк'}
        </p>
        {payment.rawComment && <p className="muted">Коментар: {payment.rawComment}</p>}
        {error && <div className="error-banner">{error}</div>}

        <div className="form-row">
          <label htmlFor="student">Студент</label>
          <select
            id="student"
            value={studentId}
            onChange={(e) => setStudentId(e.target.value ? Number(e.target.value) : '')}
          >
            <option value="">Не визначено</option>
            {[...students].sort((a, b) => a.fullName.localeCompare(b.fullName, 'uk')).map((s) => (
              <option key={s.id} value={s.id}>{s.fullName}</option>
            ))}
          </select>
        </div>

        <div className="form-row">
          <label htmlFor="tariff">Тариф</label>
          <select
            id="tariff"
            value={tariffPlanId}
            onChange={(e) => setTariffPlanId(e.target.value ? Number(e.target.value) : '')}
          >
            <option value="">Не визначено</option>
            {tariffPlans.filter((p) => p.active || p.id === payment.tariffPlanId).map((p) => (
              <option key={p.id} value={p.id}>{p.label}</option>
            ))}
          </select>
        </div>

        <div className="form-row">
          <label htmlFor="period">Період</label>
          <div className="toolbar" style={{ marginBottom: 0 }}>
            <select id="period" value={periodMonth} onChange={(e) => setPeriodMonth(Number(e.target.value))}>
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
          </div>
        </div>

        <div className="toolbar">
          <button type="submit" className="primary" disabled={saving}>
            {saving ? 'Збереження…' : 'Зберегти'}
          </button>
          <button type="button" onClick={onCancel}>Скасувати</button>
        </div>
      </form>
    </div>
  );
}
