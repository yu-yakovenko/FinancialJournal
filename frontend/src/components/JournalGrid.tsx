import { useState } from 'react';
import { formatUah } from '../api/client';
import type { JournalGrid as JournalGridData } from '../api/types';

const MONTH_NAMES_SHORT = ['Січ', 'Лют', 'Бер', 'Кві', 'Тра', 'Чер', 'Лип', 'Сер', 'Вер', 'Жов', 'Лис', 'Гру'];

type SortField = 'fullName' | 'tariffLabel';
type SortDirection = 'asc' | 'desc';

interface Props {
  grid: JournalGridData;
  onCellClick: (studentId: number, tariffPlanId: number, fullName: string, tariffLabel: string, month: number) => void;
  emptyMessage?: string;
}

export function JournalGrid({ grid, onCellClick, emptyMessage }: Props) {
  const [sortField, setSortField] = useState<SortField>('fullName');
  const [sortDirection, setSortDirection] = useState<SortDirection>('asc');

  if (grid.rows.length === 0) {
    return (
      <div className="empty-state">
        {emptyMessage ?? 'Ще немає жодного студента. Додайте студента вручну або зачекайте на перший платіж.'}
      </div>
    );
  }

  function handleSort(field: SortField) {
    if (field === sortField) {
      setSortDirection((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortField(field);
      setSortDirection('asc');
    }
  }

  function sortIndicator(field: SortField) {
    if (field !== sortField) return null;
    return sortDirection === 'asc' ? ' ▲' : ' ▼';
  }

  const sortedRows = [...grid.rows].sort((a, b) => {
    const aValue = sortField === 'fullName' ? a.fullName : (a.tariffLabel ?? '');
    const bValue = sortField === 'fullName' ? b.fullName : (b.tariffLabel ?? '');
    const comparison = aValue.localeCompare(bValue, 'uk');
    return sortDirection === 'asc' ? comparison : -comparison;
  });

  const monthlyTotals = MONTH_NAMES_SHORT.map((_, index) =>
    grid.rows.reduce((sum, row) => sum + (row.cells[index]?.amountKopiykas ?? 0), 0),
  );

  return (
    <div className="table-scroll">
      <table>
        <thead>
          <tr>
            <th onClick={() => handleSort('fullName')} style={{ cursor: 'pointer' }}>ПІБ{sortIndicator('fullName')}</th>
            <th onClick={() => handleSort('tariffLabel')} style={{ cursor: 'pointer' }}>Тариф{sortIndicator('tariffLabel')}</th>
            {MONTH_NAMES_SHORT.map((name) => (
              <th key={name}>{name}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {sortedRows.map((row) => (
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
        <tfoot>
          <tr>
            <td colSpan={2} style={{ fontWeight: 600, borderTop: '2px solid var(--border)' }}>Разом</td>
            {monthlyTotals.map((total, index) => (
              <td key={index} style={{ fontWeight: 600, textAlign: 'right', borderTop: '2px solid var(--border)' }}>
                {total > 0 ? formatUah(total) : ''}
              </td>
            ))}
          </tr>
        </tfoot>
      </table>
    </div>
  );
}
