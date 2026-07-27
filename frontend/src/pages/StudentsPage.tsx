import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { StudentResponse } from '../api/types';
import { StudentForm } from '../components/StudentForm';
import { StudentEnrollmentsModal } from '../components/StudentEnrollmentsModal';

export function StudentsPage() {
  const [students, setStudents] = useState<StudentResponse[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState<StudentResponse | 'new' | null>(null);
  const [managingTariffsFor, setManagingTariffsFor] = useState<StudentResponse | null>(null);

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
              <th>Тарифи</th>
              <th>Дата додавання</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {students.map((s) => (
              <tr key={s.id}>
                <td>{s.fullName}</td>
                <td className="muted">
                  {s.activeTariffLabels.length > 0 ? s.activeTariffLabels.join(', ') : '— (ще не визначено)'}
                </td>
                <td className="muted">{new Date(s.createdAt).toLocaleDateString('uk-UA')}</td>
                <td>
                  <button onClick={() => setManagingTariffsFor(s)}>Тарифи</button>{' '}
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
          students={students}
          onSubmit={async (data) => {
            if (editing === 'new') {
              await api.createStudent({ fullName: data.fullName });
            } else {
              await api.updateStudent(editing.id, data);
            }
            setEditing(null);
            reload();
          }}
          onMerge={
            editing === 'new'
              ? undefined
              : async (otherStudentId) => {
                  await api.mergeStudents([editing.id, otherStudentId], editing.id);
                  setEditing(null);
                  reload();
                }
          }
          onCancel={() => setEditing(null)}
        />
      )}

      {managingTariffsFor && (
        <StudentEnrollmentsModal
          studentId={managingTariffsFor.id}
          studentName={managingTariffsFor.fullName}
          onClose={() => setManagingTariffsFor(null)}
          onChanged={reload}
        />
      )}
    </div>
  );
}
