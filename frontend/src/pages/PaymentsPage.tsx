import { useEffect, useState } from 'react';
import { api, formatUah } from '../api/client';
import type { PaymentMatchStatus, PaymentResponse, PaymentSource, StudentResponse, TariffPlan } from '../api/types';
import { PaymentEditForm } from '../components/PaymentEditForm';

const MONTH_NAMES = [
  'січень', 'лютий', 'березень', 'квітень', 'травень', 'червень',
  'липень', 'серпень', 'вересень', 'жовтень', 'листопад', 'грудень',
];

const STATUS_LABELS: Record<PaymentMatchStatus, string> = {
  MATCHED: 'Зараховано',
  NEEDS_REVIEW: 'Потребує уваги',
  IGNORED: 'Ігнорується',
};

const STATUS_DOT_CLASS: Record<PaymentMatchStatus, string> = {
  MATCHED: 'status-green',
  NEEDS_REVIEW: 'status-yellow',
  IGNORED: 'status-red',
};

const PAGE_SIZE = 25;

type IdFilter = '' | 'none' | number;

function getPageNumbers(current: number, total: number): (number | '...')[] {
  if (total <= 1) return [1];
  const delta = 2;
  const left = Math.max(2, current - delta);
  const right = Math.min(total - 1, current + delta);
  const pages: (number | '...')[] = [1];
  if (left > 2) pages.push('...');
  for (let i = left; i <= right; i++) pages.push(i);
  if (right < total - 1) pages.push('...');
  pages.push(total);
  return pages;
}

export function PaymentsPage() {
  const [payments, setPayments] = useState<PaymentResponse[]>([]);
  const [students, setStudents] = useState<StudentResponse[]>([]);
  const [tariffPlans, setTariffPlans] = useState<TariffPlan[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState<PaymentResponse | null>(null);

  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [amountMin, setAmountMin] = useState('');
  const [amountMax, setAmountMax] = useState('');
  const [sourceFilter, setSourceFilter] = useState<PaymentSource | ''>('');
  const [statusFilter, setStatusFilter] = useState<PaymentMatchStatus | ''>('');
  const [studentFilter, setStudentFilter] = useState<IdFilter>('');
  const [tariffFilter, setTariffFilter] = useState<IdFilter>('');
  const [page, setPage] = useState(1);

  function reload() {
    api.allPayments().then(setPayments).catch((e) => setError(e.message));
    api.students().then(setStudents).catch((e) => setError(e.message));
    api.tariffPlans().then(setTariffPlans).catch((e) => setError(e.message));
  }

  useEffect(reload, []);
  useEffect(() => setPage(1), [
    dateFrom, dateTo, amountMin, amountMax, sourceFilter, statusFilter, studentFilter, tariffFilter,
  ]);

  const studentsById = new Map(students.map((s) => [s.id, s]));

  const filtered = payments.filter((p) => {
    if (dateFrom && p.paymentDate < dateFrom) return false;
    if (dateTo && p.paymentDate > dateTo) return false;
    const amountUah = p.amountKopiykas / 100;
    if (amountMin !== '' && amountUah < Number(amountMin)) return false;
    if (amountMax !== '' && amountUah > Number(amountMax)) return false;
    if (sourceFilter && p.source !== sourceFilter) return false;
    if (statusFilter && p.matchStatus !== statusFilter) return false;
    if (studentFilter === 'none' && p.studentId !== null) return false;
    if (typeof studentFilter === 'number' && p.studentId !== studentFilter) return false;
    if (tariffFilter === 'none' && p.tariffPlanId !== null) return false;
    if (typeof tariffFilter === 'number' && p.tariffPlanId !== tariffFilter) return false;
    return true;
  });

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const paged = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

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
  }

  async function handleIgnore(payment: PaymentResponse) {
    if (!confirm('Позначити платіж як ігнорований?')) return;
    await api.ignorePayment(payment.id);
    reload();
  }

  return (
    <div>
      <h2>Усі платежі</h2>
      {error && <div className="error-banner">{error}</div>}

      <div className="toolbar">
        <label className="muted">
          Дата з{' '}
          <input type="date" value={dateFrom} onChange={(e) => setDateFrom(e.target.value)} />
        </label>
        <label className="muted">
          по{' '}
          <input type="date" value={dateTo} onChange={(e) => setDateTo(e.target.value)} />
        </label>
        <input
          type="number"
          value={amountMin}
          onChange={(e) => setAmountMin(e.target.value)}
          placeholder="Сума від"
          style={{ width: 110 }}
        />
        <input
          type="number"
          value={amountMax}
          onChange={(e) => setAmountMax(e.target.value)}
          placeholder="Сума до"
          style={{ width: 110 }}
        />
        <select value={sourceFilter} onChange={(e) => setSourceFilter(e.target.value as PaymentSource | '')}>
          <option value="">Усі джерела</option>
          <option value="BANK">Банк</option>
          <option value="CASH">Готівка</option>
        </select>
        <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value as PaymentMatchStatus | '')}>
          <option value="">Усі статуси</option>
          <option value="MATCHED">Зараховано</option>
          <option value="NEEDS_REVIEW">Потребує уваги</option>
          <option value="IGNORED">Ігнорується</option>
        </select>
        <select
          value={studentFilter}
          onChange={(e) => setStudentFilter(e.target.value === '' ? '' : e.target.value === 'none' ? 'none' : Number(e.target.value))}
        >
          <option value="">Усі студенти</option>
          <option value="none">Не визначено</option>
          {[...students].sort((a, b) => a.fullName.localeCompare(b.fullName, 'uk')).map((s) => (
            <option key={s.id} value={s.id}>{s.fullName}</option>
          ))}
        </select>
        <select
          value={tariffFilter}
          onChange={(e) => setTariffFilter(e.target.value === '' ? '' : e.target.value === 'none' ? 'none' : Number(e.target.value))}
        >
          <option value="">Усі тарифи</option>
          <option value="none">Не визначено</option>
          {tariffPlans.map((p) => (
            <option key={p.id} value={p.id}>{p.label}</option>
          ))}
        </select>
      </div>

      {filtered.length === 0 && <div className="empty-state">Платежів не знайдено.</div>}

      {filtered.length > 0 && (
        <>
          <p className="muted">Знайдено {filtered.length} платежів · сторінка {currentPage} з {totalPages}</p>
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Дата</th>
                  <th>Сума</th>
                  <th>Джерело</th>
                  <th>Статус</th>
                  <th>Студент</th>
                  <th>Тариф</th>
                  <th>Період</th>
                  <th>Коментар</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {paged.map((p) => {
                  const student = p.studentId ? studentsById.get(p.studentId) : undefined;
                  return (
                    <tr key={p.id}>
                      <td className="muted">{p.paymentDate}</td>
                      <td>{formatUah(p.amountKopiykas)} грн</td>
                      <td className="muted">{p.source === 'CASH' ? 'Готівка' : 'Банк'}</td>
                      <td>
                        <span className={`status-badge ${STATUS_DOT_CLASS[p.matchStatus]}`}>
                          <span className="dot" />
                          {STATUS_LABELS[p.matchStatus]}
                        </span>
                      </td>
                      <td>{student?.fullName ?? p.parsedPayerName ?? '—'}</td>
                      <td className="muted">{p.tariffLabel ?? '—'}</td>
                      <td className="muted">
                        {p.periodYear && p.periodMonth ? `${MONTH_NAMES[p.periodMonth - 1]} ${p.periodYear}` : '—'}
                      </td>
                      <td className="muted">{p.rawComment ?? '—'}</td>
                      <td>
                        <button onClick={() => setEditing(p)}>Редагувати</button>{' '}
                        {p.matchStatus !== 'IGNORED' && (
                          <button onClick={() => handleIgnore(p)}>Ігнорувати</button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          <div className="toolbar">
            <button disabled={currentPage <= 1} onClick={() => setPage(currentPage - 1)}>← Назад</button>
            {getPageNumbers(currentPage, totalPages).map((n, i) =>
              n === '...' ? (
                <span key={`ellipsis-${i}`} className="muted">…</span>
              ) : (
                <button
                  key={n}
                  className={n === currentPage ? 'primary' : undefined}
                  disabled={n === currentPage}
                  onClick={() => setPage(n)}
                >
                  {n}
                </button>
              )
            )}
            <button disabled={currentPage >= totalPages} onClick={() => setPage(currentPage + 1)}>Вперед →</button>
          </div>
        </>
      )}

      {editing && (
        <PaymentEditForm
          payment={editing}
          students={students}
          tariffPlans={tariffPlans}
          onSubmit={handleSave}
          onCancel={() => setEditing(null)}
        />
      )}
    </div>
  );
}
