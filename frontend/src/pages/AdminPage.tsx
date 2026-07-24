import { useState } from 'react';
import type { FormEvent } from 'react';
import { api } from '../api/client';
import type { IngestResult } from '../api/types';

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
      <p className="muted">
        Одноразове довантаження виписки з Monobank за минулий період (наприклад, за попередній рік). Через
        обмеження Monobank API (максимум 31 день на запит, ~1 запит на хвилину) довантаження за рік займає
        приблизно 10-15 хвилин — не закривайте цю вкладку, поки запит не завершиться.
      </p>

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
    </div>
  );
}
