import { useState } from 'react';
import type { FormEvent } from 'react';
import { api, formatUah } from '../api/client';
import type { DuplicateGroup, IngestResult, StudentMergeSummary } from '../api/types';

function defaultPreviousYearRange(): { from: string; to: string } {
  const previousYear = new Date().getFullYear() - 1;
  return { from: `${previousYear}-01-01`, to: `${previousYear}-12-31` };
}

export function AdminPage() {
  const defaults = defaultPreviousYearRange();
  const [from, setFrom] = useState(defaults.from);
  const [to, setTo] = useState(defaults.to);
  const [running, setRunning] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<IngestResult | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setRunning(true);
    setError(null);
    setResult(null);
    try {
      const ingestResult = await api.backfillPayments(from, to);
      setResult(ingestResult);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setRunning(false);
    }
  }

  return (
    <div>
      <h2>Довантаження старих платежів</h2>
      <div className="notice notice-amber">
        <span className="notice-icon">!</span>
        <span>
          Одноразове довантаження виписки з Monobank за минулий період (наприклад, за попередній рік). Через
          обмеження Monobank API (максимум 31 день на запит, ~1 запит на хвилину) довантаження за рік займає
          приблизно 10-15 хвилин — не закривайте цю вкладку, поки запит не завершиться.
        </span>
      </div>

      <form onSubmit={handleSubmit} style={{ maxWidth: 400 }}>
        {error && <div className="error-banner">{error}</div>}

        <div className="form-row">
          <label htmlFor="from">З дати</label>
          <input id="from" type="date" value={from} onChange={(e) => setFrom(e.target.value)} required />
        </div>

        <div className="form-row">
          <label htmlFor="to">По дату</label>
          <input id="to" type="date" value={to} onChange={(e) => setTo(e.target.value)} required />
        </div>

        <div className="toolbar">
          <button type="submit" className="primary" disabled={running}>
            {running ? 'Довантаження…' : 'Запустити довантаження'}
          </button>
        </div>
      </form>

      {result && (
        <p>
          Готово: зараховано {result.matched}, на перевірку {result.needsReview}, пропущено {result.skipped}.
        </p>
      )}

      <DuplicateStudentsSection />
    </div>
  );
}

function DuplicateStudentsSection() {
  const [groups, setGroups] = useState<DuplicateGroup[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [autoResult, setAutoResult] = useState<StudentMergeSummary | null>(null);

  async function loadDuplicates() {
    setLoading(true);
    setError(null);
    try {
      setGroups(await api.studentDuplicates());
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
    }
  }

  async function handleAuto() {
    if (!confirm("Автоматично об'єднати всіх однозначних дублікатів студентів? Дію не можна скасувати.")) return;
    setLoading(true);
    setError(null);
    setAutoResult(null);
    try {
      const autoSummary = await api.mergeStudentsAuto();
      setAutoResult(autoSummary);
      await loadDuplicates();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <h2>Дублікати студентів</h2>
      <p className="muted">
        Знаходить студентів, чиє ім'я повністю співпадає або є частиною імені іншого (без урахування регістра) —
        так трапляється, коли платник у коментарі до переказу то вказує повне ПІБ, то лише прізвище.
      </p>
      {error && <div className="error-banner">{error}</div>}

      <div className="toolbar">
        <button onClick={loadDuplicates} disabled={loading}>
          {loading ? 'Завантаження…' : 'Знайти дублікати'}
        </button>
        <button className="primary" onClick={handleAuto} disabled={loading}>
          Прибрати дублі автоматично
        </button>
      </div>

      {autoResult && (
        <p className="muted">
          Автоматично об'єднано груп: {autoResult.mergedGroups}, прибрано дублікатів: {autoResult.mergedStudents}.
          {autoResult.mergedGroups === 0 &&
            ' Однозначних дублікатів не знайдено — можливі кандидати нижче потребують ручної перевірки.'}
        </p>
      )}

      {groups && groups.length === 0 && <div className="empty-state">Дублікатів не знайдено.</div>}

      {groups && groups.length > 0 && (
        <div>
          {groups.map((group) => (
            <DuplicateGroupCard key={group.students.map((s) => s.id).join('-')} group={group} onMerged={loadDuplicates} />
          ))}
        </div>
      )}
    </div>
  );
}

function DuplicateGroupCard({ group, onMerged }: { group: DuplicateGroup; onMerged: () => void }) {
  const [checked, setChecked] = useState<Record<number, boolean>>(
    Object.fromEntries(group.students.map((s) => [s.id, true])),
  );
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const checkedIds = group.students.filter((s) => checked[s.id]).map((s) => s.id);

  function toggle(id: number) {
    setChecked((prev) => ({ ...prev, [id]: !prev[id] }));
  }

  async function merge() {
    setBusy(true);
    setError(null);
    try {
      await api.mergeStudents(checkedIds);
      onMerged();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="card" style={{ maxWidth: 700 }}>
      {error && <div className="error-banner">{error}</div>}
      {group.students.map((s) => (
        <label key={s.id} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '6px 0' }}>
          <input type="checkbox" checked={!!checked[s.id]} onChange={() => toggle(s.id)} />
          <span style={{ flex: 1 }}>
            {s.fullName}
            {s.recommendedTarget && <span className="muted"> · залишиться основним</span>}
          </span>
          <span className="muted" style={{ fontSize: 13, textAlign: 'right' }}>
            {s.lastPaymentDate
              ? `Останній платіж: ${formatUah(s.lastPaymentAmountKopiykas ?? 0)} грн, ${s.lastPaymentDate}${
                  s.lastPaymentTariffLabel ? ` (${s.lastPaymentTariffLabel})` : ''
                }`
              : 'Платежів немає'}
          </span>
        </label>
      ))}
      <div className="toolbar" style={{ marginTop: 12, marginBottom: 0 }}>
        <button className="primary" disabled={busy || checkedIds.length < 2} onClick={merge}>
          Об'єднати
        </button>
      </div>
    </div>
  );
}
