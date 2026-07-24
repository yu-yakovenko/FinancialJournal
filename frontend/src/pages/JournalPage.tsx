import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { JournalGrid as JournalGridData, StudentResponse } from '../api/types';
import { JournalGrid } from '../components/JournalGrid';
import { PaymentDetailModal } from '../components/PaymentDetailModal';
import { CashPaymentForm } from '../components/CashPaymentForm';

const CURRENT_YEAR = new Date().getFullYear();

export function JournalPage() {
  const [year, setYear] = useState(CURRENT_YEAR);
  const [grid, setGrid] = useState<JournalGridData | null>(null);
  const [students, setStudents] = useState<StudentResponse[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [detail, setDetail] = useState<{ studentId: number; fullName: string; month: number } | null>(null);
  const [showCashForm, setShowCashForm] = useState(false);

  function reload() {
    api.journal(year).then(setGrid).catch((e) => setError(e.message));
    api.students().then(setStudents).catch((e) => setError(e.message));
  }

  useEffect(reload, [year]);

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
        <button className="primary" onClick={() => setShowCashForm(true)}>+ Готівкова оплата</button>
      </div>

      {grid && <JournalGrid grid={grid} onCellClick={(studentId, fullName, month) => setDetail({ studentId, fullName, month })} />}

      {detail && (
        <PaymentDetailModal
          studentId={detail.studentId}
          fullName={detail.fullName}
          year={year}
          month={detail.month}
          onClose={() => setDetail(null)}
        />
      )}

      {showCashForm && (
        <CashPaymentForm
          students={students}
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
