import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { TARIFF_LABELS } from '../api/types';
import type { StudentResponse } from '../api/types';
import { StudentForm } from '../components/StudentForm';

export function StudentsPage() {
  const [students, setStudents] = useState<StudentResponse[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState<StudentResponse | 'new' | null>(null);

  function reload() {
    api.students().then(setStudents).catch((e) => setError(e.message));
  }

  useEffect(reload, []);

  async function handleDeactivate(id: number) {
    if (!confirm('Деактивувати студента? Історія оплат збережеться.')) return;
    await api.deactivateStudent(id);
    reload();
  }

  return (
    <div>
      <h2>Студенти</h2>
      {error && <div className="error-banner">{error}</div>}

      <div className="toolbar">
        <button className="primary" onClick={() => setEditing('new')}>+ Додати студента</button>
      </div>

      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th>ПІБ</th>
              <th>Тариф</th>
              <th>Дата додавання</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {students.map((s) => (
              <tr key={s.id}>
                <td>{s.fullName}</td>
                <td className="muted">{s.tariff ? TARIFF_LABELS[s.tariff] : '— (визначиться по першому платежу)'}</td>
                <td className="muted">{new Date(s.createdAt).toLocaleDateString('uk-UA')}</td>
                <td>
                  <button onClick={() => setEditing(s)}>Редагувати</button>{' '}
                  <button onClick={() => handleDeactivate(s.id)}>Деактивувати</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {editing && (
        <StudentForm
          student={editing === 'new' ? undefined : editing}
          onSubmit={async (data) => {
            if (editing === 'new') {
              await api.createStudent({ fullName: data.fullName, tariff: data.tariff });
            } else {
              await api.updateStudent(editing.id, data);
            }
            setEditing(null);
            reload();
          }}
          onCancel={() => setEditing(null)}
        />
      )}
    </div>
  );
}
