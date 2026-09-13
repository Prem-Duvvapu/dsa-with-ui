import fields from './fields.module.css';
import React from 'react';
import { Plus, Minus } from 'lucide-react';

/**
 * Grid painter for INT_GRID. A click cycles a cell through [minValue..maxValue] — for
 * the common 0/1 case (number-of-islands) that's a plain toggle. Row/column controls
 * are bounded by minRows/maxRows/maxCols, matching InputValidator's own checks.
 */
export default function GridField({ field, value, onChange }) {
  const grid = Array.isArray(value) && value.length ? value : [[0]];
  const c = field.constraints || {};
  const minRows = c.minRows ?? 1;
  const maxRows = c.maxRows ?? Infinity;
  const maxCols = c.maxCols ?? Infinity;
  const minValue = c.minValue ?? 0;
  const maxValue = c.maxValue ?? 1;
  const cols = grid[0]?.length ?? 1;

  const cycle = (v) => (v >= maxValue ? minValue : v + 1);

  const clickCell = (r, col) => {
    const next = grid.map((row) => row.slice());
    next[r][col] = cycle(next[r][col]);
    onChange(next);
  };

  const addRow = () => {
    if (grid.length >= maxRows) return;
    onChange(grid.concat([Array(cols).fill(minValue)]));
  };

  const removeRow = () => {
    if (grid.length <= minRows || grid.length <= 1) return;
    onChange(grid.slice(0, -1));
  };

  const addCol = () => {
    if (cols >= maxCols) return;
    onChange(grid.map((row) => row.concat([minValue])));
  };

  const removeCol = () => {
    if (cols <= 1) return;
    onChange(grid.map((row) => row.slice(0, -1)));
  };

  return (
    <div className={fields.gridStack}>
      <div className={fields.gridBoard}>
        {grid.map((row, r) => (
          <div key={r} className={fields.gridRow}>
            {row.map((cell, col) => (
              <button
                type="button"
                key={col}
                onClick={() => clickCell(r, col)}
                aria-label={`Cell row ${r + 1}, column ${col + 1}, value ${cell}`}
                className={`${fields.gridCell}${cell === 0 ? '' : ` ${fields.gridCellSet}`}`}
              >
                {cell}
              </button>
            ))}
          </div>
        ))}
      </div>
      <div className={fields.gridControls}>
        <div className={fields.rowTight}>
          <span className={fields.microLabel}>Rows</span>
          <button
            type="button" className={`btn btn-outline ${fields.tinyButton}`}
            onClick={removeRow} disabled={grid.length <= Math.max(minRows, 1)}
            aria-label="Remove a row"
          ><Minus size={11} /></button>
          <button
            type="button" className={`btn btn-outline ${fields.tinyButton}`}
            onClick={addRow} disabled={grid.length >= maxRows}
            aria-label="Add a row"
          ><Plus size={11} /></button>
        </div>
        <div className={fields.rowTight}>
          <span className={fields.microLabel}>Cols</span>
          <button
            type="button" className={`btn btn-outline ${fields.tinyButton}`}
            onClick={removeCol} disabled={cols <= 1}
            aria-label="Remove a column"
          ><Minus size={11} /></button>
          <button
            type="button" className={`btn btn-outline ${fields.tinyButton}`}
            onClick={addCol} disabled={cols >= maxCols}
            aria-label="Add a column"
          ><Plus size={11} /></button>
        </div>
      </div>
    </div>
  );
}
