import { useState } from 'react';
import type { FormEvent } from 'react';
import type { StudentResponse } from '../api/types';

interface Props {
  student?: StudentResponse;
  /** Full roster, for the "merge with" dropdown — only shown when editing an existing student. */
  students?: StudentResponse[];
  onSubmit: (data: { fullName: string; active: boolean }) => Promise<void>;
  onMerge?: (otherStudentId: number) => Promise<void>;
  onCancel: () => void;
}

export function StudentForm({ student, students, onSubmit, onMerge, onCancel }: Props) {
  const [fullName, setFullName] = useState(student?.fullName ?? '');
  const [active, setActive] = useState(student?.active ?? true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [mergeTargetId, setMergeTargetId] = useState<number | ''>('');
  const [merging, setMerging] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await onSubmit({ fullName: fullName.trim(), active });
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setSaving(false);
    }
  }

  async function handleMerge() {
    if (!mergeTargetId || !onMerge) return;
    setMerging(true);
    setError(null);
    try {
      await onMerge(mergeTargetId);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setMerging(false);
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

        {!student && (
          <p className="muted">Тарифи додаються окремо, після створення студента.</p>
        )}

        <div className="toolbar">
          <button type="submit" className="primary" disabled={saving}>
            {saving ? 'Збереження…' : 'Зберегти'}
          </button>
          <button type="button" onClick={onCancel}>Скасувати</button>
        </div>

        {student && students && onMerge && (
          <div className="form-row">
            <label htmlFor="mergeWith">Об'єднати з</label>
            <div className="toolbar" style={{ marginBottom: 0 }}>
              <select
                id="mergeWith"
                value={mergeTargetId}
                onChange={(e) => setMergeTargetId(e.target.value ? Number(e.target.value) : '')}
              >
                <option value="">Оберіть студента…</option>
                {students
                  .filter((s) => s.id !== student.id)
                  .sort((a, b) => a.fullName.localeCompare(b.fullName, 'uk'))
                  .map((s) => (
                    <option key={s.id} value={s.id}>{s.fullName}</option>
                  ))}
              </select>
              <button type="button" disabled={!mergeTargetId || merging} onClick={handleMerge}>
                {merging ? "Об'єднання…" : "Об'єднати"}
              </button>
            </div>
            <p className="muted">Обраний студент буде видалений, а його платежі й тарифи перейдуть до {student.fullName}.</p>
          </div>
        )}
      </form>
    </div>
  );
}
