import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { JournalGrid as JournalGridData, StudentResponse, TariffPlan } from '../api/types';
import { JournalGrid } from '../components/JournalGrid';
import { PaymentDetailModal } from '../components/PaymentDetailModal';
import { CashPaymentForm } from '../components/CashPaymentForm';

const CURRENT_YEAR = new Date().getFullYear();

interface DetailTarget {
  studentId: number;
  tariffPlanId: number;
  fullName: string;
  tariffLabel: string;
  month: number;
}

export function JournalPage() {
  const [year, setYear] = useState(CURRENT_YEAR);
  const [grid, setGrid] = useState<JournalGridData | null>(null);
  const [students, setStudents] = useState<StudentResponse[]>([]);
  const [tariffPlans, setTariffPlans] = useState<TariffPlan[]>([]);
  const [tariffFilter, setTariffFilter] = useState<number | 'all'>('all');
  const [error, setError] = useState<string | null>(null);
  const [detail, setDetail] = useState<DetailTarget | null>(null);
  const [showCashForm, setShowCashForm] = useState(false);

  function reload() {
    api.journal(year).then(setGrid).catch((e) => setError(e.message));
    api.students().then(setStudents).catch((e) => setError(e.message));
    api.tariffPlans().then(setTariffPlans).catch((e) => setError(e.message));
  }

  useEffect(reload, [year]);

  const filteredGrid = grid && tariffFilter !== 'all'
    ? { ...grid, rows: grid.rows.filter((row) => row.tariffPlanId === tariffFilter) }
    : grid;

  return (
    <div>
      <h2>Журнал оплат</h2>
      {error && <div className="error-banner">{error}</div>}

      <div className="toolbar">
        <label htmlFor="year">Рік:</label>
        <select id="year" value={year} onChange={(e) => setYear(Number(e.target.value))}>
          {Array.from({ length: 5 }, (_, i) => CURRENT_YEAR - 2 + i).map((y) => (
            <option key={y} value={y}>{y}</option>
          ))}
        </select>
        <label htmlFor="tariffFilter">Тариф:</label>
        <select
          id="tariffFilter"
          value={tariffFilter}
          onChange={(e) => setTariffFilter(e.target.value === 'all' ? 'all' : Number(e.target.value))}
        >
          <option value="all">Усі тарифи</option>
          {tariffPlans.map((plan) => (
            <option key={plan.id} value={plan.id}>{plan.label}</option>
          ))}
        </select>
        <button className="primary" onClick={() => setShowCashForm(true)}>+ Готівкова оплата</button>
      </div>

      {filteredGrid && (
        <JournalGrid
          grid={filteredGrid}
          onCellClick={(studentId, tariffPlanId, fullName, tariffLabel, month) =>
            setDetail({ studentId, tariffPlanId, fullName, tariffLabel, month })
          }
          emptyMessage={tariffFilter !== 'all' ? 'Немає студентів із цим тарифом за обраний рік.' : undefined}
        />
      )}

      {detail && (
        <PaymentDetailModal
          studentId={detail.studentId}
          tariffPlanId={detail.tariffPlanId}
          fullName={detail.fullName}
          tariffLabel={detail.tariffLabel}
          year={year}
          month={detail.month}
          onClose={() => setDetail(null)}
        />
      )}

      {showCashForm && (
        <CashPaymentForm
          students={students}
          tariffPlans={tariffPlans}
          defaultYear={year}
          onSubmit={async (data) => {
            await api.addCashPayment(data);
            setShowCashForm(false);
            reload();
          }}
          onCancel={() => setShowCashForm(false)}
        />
      )}
    </div>
  );
}
