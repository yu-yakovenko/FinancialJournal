import { useState } from 'react';
import type { FormEvent } from 'react';
import { formatUah } from '../api/client';
import type { StudentResponse, TariffPlan } from '../api/types';

interface Props {
  students: StudentResponse[];
  tariffPlans: TariffPlan[];
  defaultYear: number;
  onSubmit: (data: {
    studentId: number;
    tariffPlanId: number;
    amountUah: number;
    paymentDate: string;
    periodYear: number;
    periodMonth: number;
    comment?: string;
  }) => Promise<void>;
  onCancel: () => void;
}

const MONTH_NAMES = [
  'січень', 'лютий', 'березень', 'квітень', 'травень', 'червень',
  'липень', 'серпень', 'вересень', 'жовтень', 'листопад', 'грудень',
];

export function CashPaymentForm({ students, tariffPlans, defaultYear, onSubmit, onCancel }: Props) {
  const [studentId, setStudentId] = useState<number | ''>('');
  const [tariffPlanId, setTariffPlanId] = useState<number | ''>('');
  const [amountUah, setAmountUah] = useState('');
  const [paymentDate, setPaymentDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [periodYear, setPeriodYear] = useState(defaultYear);
  const [periodMonth, setPeriodMonth] = useState(new Date().getMonth() + 1);
  const [comment, setComment] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!studentId) {
      setError('Оберіть студента');
      return;
    }
    if (!tariffPlanId) {
      setError('Оберіть тариф');
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await onSubmit({
        studentId,
        tariffPlanId,
        amountUah: Number(amountUah),
        paymentDate,
        periodYear,
        periodMonth,
        comment: comment.trim() || undefined,
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
        <h3>Готівкова оплата</h3>
        {error && <div className="error-banner">{error}</div>}

        <div className="form-row">
          <label htmlFor="student">Студент</label>
          <select
            id="student"
            value={studentId}
            onChange={(e) => setStudentId(e.target.value ? Number(e.target.value) : '')}
            required
          >
            <option value="">Оберіть студента</option>
            {students.map((s) => (
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
            required
          >
            <option value="">Оберіть тариф</option>
            {tariffPlans.filter((p) => p.active).map((p) => (
              <option key={p.id} value={p.id}>
                {p.label}
                {p.currentAmountKopiykas != null ? ` — ${formatUah(p.currentAmountKopiykas)} грн` : ''}
              </option>
            ))}
          </select>
        </div>

        <div className="form-row">
          <label htmlFor="amount">Сума, грн</label>
          <input
            id="amount"
            type="number"
            step="0.01"
            min="0.01"
            value={amountUah}
            onChange={(e) => setAmountUah(e.target.value)}
            required
          />
        </div>

        <div className="form-row">
          <label htmlFor="date">Дата оплати</label>
          <input
            id="date"
            type="date"
            value={paymentDate}
            onChange={(e) => setPaymentDate(e.target.value)}
            required
          />
        </div>

        <div className="form-row">
          <label htmlFor="periodMonth">За який місяць</label>
          <div style={{ display: 'flex', gap: 8 }}>
            <select
              id="periodMonth"
              value={periodMonth}
              onChange={(e) => setPeriodMonth(Number(e.target.value))}
            >
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

        <div className="form-row">
          <label htmlFor="comment">Коментар (необов'язково)</label>
          <input id="comment" value={comment} onChange={(e) => setComment(e.target.value)} />
        </div>

        <div className="toolbar">
          <button type="submit" className="primary" disabled={saving}>
            {saving ? 'Збереження…' : 'Додати оплату'}
          </button>
          <button type="button" onClick={onCancel}>Скасувати</button>
        </div>
      </form>
    </div>
  );
}
