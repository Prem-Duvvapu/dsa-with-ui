function DpCell({ className, glyph, state, value, rowLabel, columnLabel }) {
  return (
    <td
      className={className}
      aria-label={`row ${rowLabel}, column ${columnLabel}: ${value || 'empty'} (${state})`}
    >
      <span className="dp-glyph" aria-hidden="true">{glyph}</span>
      <span className="dp-value">{value}</span>
    </td>
  );
}

function renderCell(cell, rowLabel, columnLabel, key) {
  const value = cell.value == null ? '' : String(cell.value);

  switch (cell.state) {
    case 'probe':
      return <DpCell key={key} className="dp-cell dp-cell-probe" glyph="▼" state="probe" value={value} rowLabel={rowLabel} columnLabel={columnLabel} />;
    case 'read':
      return <DpCell key={key} className="dp-cell dp-cell-read" glyph="○" state="read" value={value} rowLabel={rowLabel} columnLabel={columnLabel} />;
    case 'known':
      return <DpCell key={key} className="dp-cell dp-cell-known" glyph="□" state="known" value={value} rowLabel={rowLabel} columnLabel={columnLabel} />;
    case 'resolved':
      return <DpCell key={key} className="dp-cell dp-cell-resolved" glyph="✓" state="resolved" value={value} rowLabel={rowLabel} columnLabel={columnLabel} />;
    case 'void':
    default:
      return <DpCell key={key} className="dp-cell dp-cell-void" glyph="▫" state="void" value={value} rowLabel={rowLabel} columnLabel={columnLabel} />;
  }
}

function labelAt(labels, index, prefix) {
  const label = labels[index];
  return typeof label === 'string' ? label : `${prefix}${index}`;
}

export default function DpTableCanvas({ currentStep, step }) {
  const activeStep = currentStep || step;
  const table = activeStep?.dpTable;
  const rowLabels = Array.isArray(table?.rowLabels) ? table.rowLabels : [];
  const colLabels = Array.isArray(table?.colLabels) ? table.colLabels : [];
  const cells = Array.isArray(table?.cells) ? table.cells : [];
  const rowCount = cells.length;
  const columnCount = rowCount > 0 && Array.isArray(cells[0]) ? cells[0].length : 0;
  const hasTable = rowCount > 0
    && columnCount > 0
    && cells.every((row) => Array.isArray(row)
      && row.length === columnCount
      && row.every((cell) => cell !== null && typeof cell === 'object' && !Array.isArray(cell)));

  // App owns the one shared CanvasShell. Returning stage content here avoids a nested
  // header/legend taking space away from the recurrence table.
  if (!hasTable) return <p className="dp-empty">No DP table data</p>;

  return (
    <div className="dp-table-wrap">
      <table className="dp-table" aria-label="Dynamic programming table">
        <thead>
          <tr>
            <th className="dp-corner" aria-label="Row labels" />
            {Array.from({ length: columnCount }, (_, columnIndex) => (
              <th className="dp-col-label" scope="col" key={columnIndex}>
                {labelAt(colLabels, columnIndex, 'c')}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {Array.from({ length: rowCount }, (_, rowIndex) => {
            const rowLabel = labelAt(rowLabels, rowIndex, 'r');
            const row = Array.isArray(cells[rowIndex]) ? cells[rowIndex] : [];

            return (
              <tr key={rowIndex}>
                <th className="dp-row-label" scope="row">{rowLabel}</th>
                {Array.from({ length: columnCount }, (_, columnIndex) => {
                  const rawCell = row[columnIndex];
                  const cell = rawCell && typeof rawCell === 'object' ? rawCell : {};
                  const columnLabel = labelAt(colLabels, columnIndex, 'c');

                  return renderCell(cell, rowLabel, columnLabel, columnIndex);
                })}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
