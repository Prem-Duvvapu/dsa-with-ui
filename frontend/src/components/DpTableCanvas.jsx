import React, { useLayoutEffect, useRef, useState } from 'react';

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

/**
 * Centre of `el`, in coordinates relative to `origin`'s content box (including its
 * scroll offset) rather than the viewport — so the overlay lines up with the table
 * even while `.dp-table-wrap` is scrolled.
 */
function centreRelativeTo(el, origin) {
  const elRect = el.getBoundingClientRect();
  const originRect = origin.getBoundingClientRect();
  return {
    x: elRect.left - originRect.left + origin.scrollLeft + elRect.width / 2,
    y: elRect.top - originRect.top + origin.scrollTop + elRect.height / 2
  };
}

/**
 * One arrow per (read cell, probe cell) pair. In every tracer today a step has at most
 * one probe cell, so this is "every read cell points at the cell being written" — the
 * recurrence's provenance, drawn rather than left to the `read` ring alone.
 */
function computeArrows(wrapEl) {
  if (!wrapEl) return [];
  const probes = wrapEl.querySelectorAll('.dp-cell-probe');
  const reads = wrapEl.querySelectorAll('.dp-cell-read');
  if (probes.length === 0 || reads.length === 0) return [];

  const arrows = [];
  for (const probe of probes) {
    const to = centreRelativeTo(probe, wrapEl);
    for (const read of reads) {
      const from = centreRelativeTo(read, wrapEl);
      arrows.push({ x1: from.x, y1: from.y, x2: to.x, y2: to.y });
    }
  }
  return arrows;
}

/**
 * Provenance arrows from every `read` cell to the cell being resolved this step.
 *
 * Pure presentation — `arrows`/`size` come from the parent's own effect. A component
 * cannot reliably read a ref onto an ANCESTOR element inside its own useLayoutEffect:
 * React commits refs and layout effects bottom-up, so a child's layout effect runs
 * before the parent host element's own ref is attached. Only the component that owns
 * both the ref and the DOM node it points at gets the "attached before this effect
 * runs" guarantee — see DpTableCanvas below.
 */
function ProvenanceArrows({ arrows, size }) {
  if (arrows.length === 0) return null;

  return (
    <svg
      className="dp-arrows"
      aria-hidden="true"
      width={size.width}
      height={size.height}
      style={{ width: size.width, height: size.height }}
    >
      <defs>
        <marker id="dp-arrowhead" markerWidth="8" markerHeight="6" refX="7" refY="3" orient="auto">
          <polygon points="0 0, 8 3, 0 6" fill="var(--probe)" />
        </marker>
      </defs>
      {arrows.map((arrow, index) => (
        <line
          key={index}
          className="dp-arrow"
          x1={arrow.x1}
          y1={arrow.y1}
          x2={arrow.x2}
          y2={arrow.y2}
          stroke="var(--probe)"
          markerEnd="url(#dp-arrowhead)"
        />
      ))}
    </svg>
  );
}

/**
 * The most recent recurrence the trace stated, for the same reason `trace/lastPayload.js`
 * exists: a value that persists between steps must not read as absent on the steps that do
 * not restate it. `DpTable.formula` is a constant of the problem while `substitution` is
 * one step's arithmetic, so tracers attach the pair only where a cell is computed - and
 * reading the rule off the current step alone blanked it on every base case, comparison
 * and closing summary in between.
 *
 * Looks back only, never forward: a rule the trace has not introduced yet is not on screen.
 */
function lastFormula(steps, index, current) {
  if (typeof current?.dpTable?.formula === 'string') return current.dpTable.formula;
  if (Array.isArray(steps)) {
    for (let i = Math.min(index ?? steps.length - 1, steps.length - 1); i >= 0; i -= 1) {
      const formula = steps[i]?.dpTable?.formula;
      if (typeof formula === 'string') return formula;
    }
  }
  return null;
}

export default function DpTableCanvas({ currentStep, step, steps, currentStepIndex }) {
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

  const wrapRef = useRef(null);
  const [arrows, setArrows] = useState([]);
  const [size, setSize] = useState({ width: 0, height: 0 });

  useLayoutEffect(() => {
    const wrapEl = wrapRef.current;
    if (!wrapEl) return undefined;

    const recompute = () => {
      setArrows(computeArrows(wrapEl));
      setSize({ width: wrapEl.scrollWidth, height: wrapEl.scrollHeight });
    };
    recompute();

    window.addEventListener('resize', recompute);
    return () => window.removeEventListener('resize', recompute);
    // hasTable/cells/activeStep together capture everything the payload can change
    // between steps; re-running per keystroke of the trace is the point.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasTable, activeStep]);

  // App owns the one shared CanvasShell. Returning stage content here avoids a nested
  // header/legend taking space away from the recurrence table.
  if (!hasTable) return <p className="dp-empty">No DP table data</p>;

  const formula = lastFormula(steps, currentStepIndex, activeStep);
  // Never carried. It describes one cell of one step; held over, it would caption the
  // wrong arithmetic.
  const substitution = typeof table.substitution === 'string' ? table.substitution : null;

  return (
    <div className="dp-stage">
      {/* Absent until the trace states a rule, and on tracers that haven't adopted design
          D3 yet — see PROMPT-F-visual-fidelity.md. A bare substitution still renders
          nothing: arithmetic with no rule above it is an unexplained fact. A bare rule is
          the opposite — the recurrence standing while this step does something it does not
          cover, which is exactly what a base case is. */}
      {formula && (
        <div className="dp-recurrence">
          <div className="dp-recurrence-formula">{formula}</div>
          {substitution && (
            <div className="dp-recurrence-substitution">{substitution}</div>
          )}
        </div>
      )}
      <div className="dp-table-wrap" ref={wrapRef}>
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
        <ProvenanceArrows arrows={arrows} size={size} />
      </div>
    </div>
  );
}
