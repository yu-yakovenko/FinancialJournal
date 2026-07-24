import { useEffect, useState } from 'react';
import { api, formatUah } from '../api/client';
import type { EnrollmentResponse, TariffPlan } from '../api/types';

interface Props {
  studentId: number;
  studentName: string;
  onClose: () => void;
  onChanged: () => void;
}

export function StudentEnrollmentsModal({ studentId, studentName, onClose, onChanged }: Props) {
  const [enrollments, setEnrollments] = useState<EnrollmentResponse[] | null>(null);
  const [tariffPlans, setTariffPlans] = useState<TariffPlan[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [addingTariffId, setAddingTariffId] = useState<number | ''>('');
  const [busy, setBusy] = useState(false);

  function reload() {
    api.studentEnrollments(studentId).then(setEnrollments).catch((e) => setError(e.message));
    api.tariffPlans().then(setTariffPlans).catch((e) => setError(e.message));
  }

  useEffect(reload, [studentId]);

  async function handleAdd() {
    if (!addingTariffId) return;
    setBusy(true);
    setError(null);
    try {
      await api.addEnrollment(studentId, { tariffPlanId: addingTariffId });
      setAddingTariffId('');
      reload();
      onChanged();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setBusy(false);
    }
  }

  async function handleEnd(id: number) {
    setBusy(true);
    setError(null);
    try {
      await api.endEnrollment(id);
      reload();
      onChanged();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setBusy(false);
    }
  }

  async function handleReactivate(id: number) {
    setBusy(true);
    setError(null);
    try {
      await api.reactivateEnrollment(id);
      reload();
      onChanged();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>Тарифи: {studentName}</h3>
        {error && <div className="error-banner">{error}</div>}

        {!enrollments && <p className="muted">Завантаження…</p>}
        {enrollments && enrollments.length === 0 && <p className="muted">Ще немає жодного тарифу.</p>}
        {enrollments && enrollments.length > 0 && (
          <table>
            <thead>
              <tr>
                <th>Тариф</th>
                <th>Чинний з</th>
                <th>Чинний до</th>
                <th>Статус</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {enrollments.map((e) => (
                <tr key={e.id}>
                  <td>{e.tariffLabel}</td>
                  <td>{e.validFrom}</td>
                  <td>{e.validTo ?? '—'}</td>
                  <td>
                    <span className={`status-badge ${e.active ? 'status-green' : 'status-red'}`}>
                      <span className="dot" />
                      {e.active ? 'Активний' : 'Завершено'}
                    </span>
                  </td>
                  <td>
                    {e.active ? (
                      <button disabled={busy} onClick={() => handleEnd(e.id)}>Завершити</button>
                    ) : (
                      <button disabled={busy} onClick={() => handleReactivate(e.id)}>Відновити</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        <div className="toolbar" style={{ marginTop: 16 }}>
          <select value={addingTariffId} onChange={(e) => setAddingTariffId(e.target.value ? Number(e.target.value) : '')}>
            <option value="">Додати тариф…</option>
            {tariffPlans.filter((p) => p.active).map((p) => (
              <option key={p.id} value={p.id}>
                {p.label}
                {p.currentAmountKopiykas != null ? ` — ${formatUah(p.currentAmountKopiykas)} грн` : ''}
              </option>
            ))}
          </select>
          <button className="primary" disabled={!addingTariffId || busy} onClick={handleAdd}>Додати</button>
        </div>

        <div className="toolbar">
          <button onClick={onClose}>Закрити</button>
        </div>
      </div>
    </div>
  );
}
