import { formatUah } from '../api/client';
import type { JournalGrid as JournalGridData } from '../api/types';

const MONTH_NAMES_SHORT = ['Січ', 'Лют', 'Бер', 'Кві', 'Тра', 'Чер', 'Лип', 'Сер', 'Вер', 'Жов', 'Лис', 'Гру'];

interface Props {
  grid: JournalGridData;
  onCellClick: (studentId: number, tariffPlanId: number, fullName: string, tariffLabel: string, month: number) => void;
}

export function JournalGrid({ grid, onCellClick }: Props) {
  if (grid.rows.length === 0) {
    return <div className="empty-state">Ще немає жодного студента. Додайте студента вручну або зачекайте на перший платіж.</div>;
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
            <tr key={`${row.studentId}-${row.tariffPlanId}`}>
              <td>{row.fullName}</td>
              <td className="muted">{row.tariffLabel ?? '—'}</td>
              {row.cells.map((cell, index) => {
                const month = index + 1;
                if (!cell) {
                  return <td key={month} className="journal-cell empty" />;
                }
                const title = cell.expectedAmountKopiykas
                  ? `Очікувано за цей місяць: ${formatUah(cell.expectedAmountKopiykas)} грн`
                  : 'Показати деталізацію';
                return (
                  <td
                    key={month}
                    className={`journal-cell status-${cell.status.toLowerCase()}`}
                    onClick={() => row.tariffPlanId && onCellClick(row.studentId, row.tariffPlanId, row.fullName, row.tariffLabel ?? '', month)}
                    title={title}
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
