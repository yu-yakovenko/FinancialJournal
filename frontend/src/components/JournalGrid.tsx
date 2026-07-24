import { formatUah } from '../api/client';
import type { JournalGrid as JournalGridData } from '../api/types';
import { TARIFF_LABELS } from '../api/types';

const MONTH_NAMES_SHORT = ['Січ', 'Лют', 'Бер', 'Кві', 'Тра', 'Чер', 'Лип', 'Сер', 'Вер', 'Жов', 'Лис', 'Гру'];

interface Props {
  grid: JournalGridData;
  onCellClick: (studentId: number, fullName: string, month: number) => void;
}

export function JournalGrid({ grid, onCellClick }: Props) {
  if (grid.rows.length === 0) {
    return <p className="muted">Ще немає жодного студента. Додайте студента вручну або зачекайте на перший платіж.</p>;
  }

  return (
    <div className="table-scroll">
      <table>
        <thead>
          <tr>
            <th>ПІБ</th>
            <th>Тариф</th>
            {MONTH_NAMES_SHORT.map((name) => (
              <th key={name}>{name}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {grid.rows.map((row) => (
            <tr key={row.studentId}>
              <td>{row.fullName}</td>
              <td className="muted">{row.tariffLabel ? TARIFF_LABELS[row.tariffLabel] : '—'}</td>
              {row.cells.map((cell, index) => {
                const month = index + 1;
                if (!cell) {
                  return <td key={month} className="journal-cell empty" />;
                }
                return (
                  <td
                    key={month}
                    className={`journal-cell status-${cell.status.toLowerCase()}`}
                    onClick={() => onCellClick(row.studentId, row.fullName, month)}
                    title="Показати деталізацію"
                  >
                    {cell.amountKopiykas > 0 ? formatUah(cell.amountKopiykas) : ''}
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
