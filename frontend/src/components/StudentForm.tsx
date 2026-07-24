import { useState } from 'react';
import type { FormEvent } from 'react';
import { TARIFF_LABELS, TARIFFS } from '../api/types';
import type { StudentResponse, Tariff } from '../api/types';

interface Props {
  student?: StudentResponse;
  onSubmit: (data: { fullName: string; tariff: Tariff | null; active: boolean }) => Promise<void>;
  onCancel: () => void;
}

export function StudentForm({ student, onSubmit, onCancel }: Props) {
  const [fullName, setFullName] = useState(student?.fullName ?? '');
  const [tariff, setTariff] = useState<Tariff | ''>(student?.tariff ?? '');
  const [active, setActive] = useState(student?.active ?? true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await onSubmit({ fullName: fullName.trim(), tariff: tariff || null, active });
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <form className="modal" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
        <h3>{student ? 'Редагувати студента' : 'Додати студента'}</h3>
        {error && <div className="error-banner">{error}</div>}

        <div className="form-row">
          <label htmlFor="fullName">ПІБ</label>
          <input
            id="fullName"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            placeholder="Прізвище Ім'я По-батькові"
            required
          />
        </div>

        <div className="form-row">
          <label htmlFor="tariff">Тариф</label>
          <select id="tariff" value={tariff} onChange={(e) => setTariff(e.target.value as Tariff | '')}>
            <option value="">Ще не визначено</option>
            {TARIFFS.map((t) => (
              <option key={t} value={t}>{TARIFF_LABELS[t]}</option>
            ))}
          </select>
        </div>

        {student && (
          <div className="form-row">
            <label>
              <input
                type="checkbox"
                checked={active}
                onChange={(e) => setActive(e.target.checked)}
                style={{ marginRight: 6 }}
              />
              Активний (показувати в журналі)
            </label>
          </div>
        )}

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
