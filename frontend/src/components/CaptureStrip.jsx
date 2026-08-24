import { useEffect, useMemo, useRef } from 'react';

/**
 * The whole execution, on screen at once.
 *
 * One column per step, one row per tracked slot. "Step 1 of 2" is a counter; this is a
 * shape — you can see how long a run is, where the interesting part sits, and that you
 * are in the third of four probes, before looking at any single frame.
 *
 * ONE COMPONENT, not one per category. A row means something different per data
 * structure — an array index, a graph vertex, a dp entry, a linked-list node — but the
 * grid is identical, which is the entire reason Bench generalises. If this ever splits
 * into ArrayStrip / GraphStrip / TreeStrip, the design has been lost.
 */

/** Rows come from whichever payload the step actually carries. */
function rowStates(step) {
  if (!step) return [];
  if (step.arrayState?.length) return step.arrayState.map((e) => e.state);
  if (step.treeNodes?.length) return step.treeNodes.map((n) => n.state);
  if (step.listState?.length) return step.listState.map((n) => n.state);
  if (step.gridState?.length) return step.gridState.map(gridRowState);
  if (step.nodeStates && Object.keys(step.nodeStates).length) {
    return Object.keys(step.nodeStates)
      .sort((a, b) => Number(a) - Number(b))
      .map((k) => step.nodeStates[k]);
  }
  return [];
}

function rowLabels(step) {
  if (!step) return [];
  if (step.arrayState?.length) return step.arrayState.map((e) => `[${e.index}]`);
  if (step.treeNodes?.length) return step.treeNodes.map((n) => n.val);
  if (step.listState?.length) return step.listState.map((n) => n.val);
  if (step.gridState?.length) return step.gridState.map((_, i) => `r${i}`);
  if (step.nodeStates) {
    return Object.keys(step.nodeStates).sort((a, b) => Number(a) - Number(b));
  }
  return [];
}

function rowValues(step) {
  if (!step) return [];
  if (step.arrayState?.length) return step.arrayState.map((e) => String(e.value));
  if (step.treeNodes?.length) return step.treeNodes.map((n) => String(n.val));
  if (step.listState?.length) return step.listState.map((n) => String(n.val));
  if (step.gridState?.length) {
    return step.gridState.map((row) => String(row.filter((c) => c !== 0).length));
  }
  return rowStates(step).map(() => '');
}

/** A grid row collapses to whichever single state best describes it. */
function gridRowState(row) {
  if (row.some((c) => c === 2)) return 'visited';
  if (row.some((c) => c === 1)) return 'default';
  return 'unvisited';
}

/**
 * Backend state vocabularies map onto Bench's two hues plus two neutrals. Anything
 * unrecognised lands on `known`, which renders as "has a value" — the safe default,
 * because inventing a fifth colour for an unknown string is how a palette rots.
 */
const PROBE = new Set(['active', 'current', 'comparing', 'swapping', 'pivot', 'visiting', 'curr', 'probe']);
const SETTLED = new Set(['sorted', 'visited', 'done', 'match', 'end', 'inserted', 'settled']);
const VOID = new Set(['unvisited', 'empty']);

export function benchState(state) {
  if (PROBE.has(state)) return 'probe';
  if (SETTLED.has(state)) return 'settled';
  if (VOID.has(state) || state == null) return 'void';
  return 'known';
}

/** Shape, so state never rides on colour alone. */
const GLYPH = { probe: '▼', settled: '✓', known: '', void: '' };

// Spelled out, not interpolated: designTokens.test.js reads the JSX statically to catch a
// className index.css does not define, and `cs-${kind}` is invisible to it.
const CELL_CLASS = {
  probe: 'cs-cell cs-probe',
  settled: 'cs-cell cs-settled',
  known: 'cs-cell cs-known',
  void: 'cs-cell cs-void'
};

/**
 * Density thresholds. Past the first, per-cell labels stop being readable; past the
 * second, DOM stops being viable at all — 5000 columns x 40 rows is 200,000 nodes and
 * will hang the browser, so the band is painted on a canvas instead.
 */
const LABELLED_UP_TO = 40;
const DOM_UP_TO = 400;

export default function CaptureStrip({ steps = [], current = 0, onSeek }) {
  const columns = steps.length;
  const mode = columns <= LABELLED_UP_TO ? 'labelled' : columns <= DOM_UP_TO ? 'dense' : 'band';

  const grid = useMemo(() => steps.map(rowStates), [steps]);
  const labels = useMemo(() => rowLabels(steps[0]), [steps]);
  const values = useMemo(() => steps.map(rowValues), [steps]);
  const rows = labels.length;

  if (!columns || !rows) return null;

  const seek = (index) => onSeek?.(Math.max(0, Math.min(columns - 1, index)));

  return (
    <section className="cs-wrap" aria-label="Execution capture">
      <div className="cs-head">
        <span className="cs-label">
          Execution capture &mdash; {columns} step{columns === 1 ? '' : 's'}, {rows} row
          {rows === 1 ? '' : 's'}
        </span>
        {mode !== 'labelled' && (
          <span className="cs-mode">{mode === 'dense' ? 'compressed' : 'density band'}</span>
        )}
      </div>

      {mode === 'band' ? (
        <BandStrip grid={grid} rows={rows} current={current} onSeek={seek} />
      ) : (
        <div className="cs-grid">
          <div className="cs-rows">
            {labels.map((label, r) => (
              <span key={r} className="cs-row-label">
                {mode === 'labelled' ? label : ''}
              </span>
            ))}
          </div>
          <div className="cs-cols">
            {grid.map((column, c) => (
              <button
                type="button"
                key={c}
                className={`cs-col${c === current ? ' cs-col-now' : ''}`}
                onClick={() => seek(c)}
                aria-label={`Step ${c + 1} of ${columns}`}
                aria-current={c === current ? 'true' : undefined}
              >
                {column.map((state, r) => {
                  const kind = benchState(state);
                  return (
                    <span key={r} className={CELL_CLASS[kind]} title={state ?? ''}>
                      {mode === 'labelled' ? (
                        <>
                          <i className="cs-glyph">{GLYPH[kind]}</i>
                          {values[c]?.[r]}
                        </>
                      ) : null}
                    </span>
                  );
                })}
                {mode === 'labelled' && <span className="cs-tick">{c + 1}</span>}
              </button>
            ))}
          </div>
        </div>
      )}
    </section>
  );
}

/**
 * Past a few hundred steps the strip becomes a painted band: same rows, same
 * left-to-right time, one thin column per step and no text.
 *
 * Columns are BUCKETED rather than sampled once there are more steps than pixels.
 * Plain downsampling drops whole columns, and what it drops is exactly the sweeps that
 * make the band worth having — so each pixel takes the most significant state in its
 * range, with probe winning over settled winning over known.
 */
function BandStrip({ grid, rows, current, onSeek }) {
  const canvasRef = useRef(null);
  const columns = grid.length;

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const width = canvas.clientWidth || 600;
    const height = Math.max(60, Math.min(200, rows * 3));
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    canvas.width = width * dpr;
    canvas.height = height * dpr;

    let ctx = null;
    try {
      ctx = canvas.getContext('2d');
    } catch {
      // Some environments (jsdom without the canvas package, hardened webviews)
      // throw rather than return null. The strip stays an interactive scrubber;
      // it simply paints nothing.
    }
    if (!ctx) return;
    ctx.scale(dpr, dpr);

    const read = (name) =>
      getComputedStyle(canvas).getPropertyValue(name).trim() || '#000';
    const paint = {
      probe: read('--probe'),
      settled: read('--settled'),
      known: read('--bench-fill'),
      void: read('--bench-recessed')
    };
    const RANK = { probe: 3, settled: 2, known: 1, void: 0 };

    ctx.fillStyle = read('--bench-recessed');
    ctx.fillRect(0, 0, width, height);

    const perPixel = Math.max(1, Math.ceil(columns / width));
    const colWidth = Math.max(1, width / Math.ceil(columns / perPixel));
    const rowHeight = height / rows;

    for (let bucket = 0; bucket * perPixel < columns; bucket += 1) {
      const from = bucket * perPixel;
      const to = Math.min(columns, from + perPixel);

      for (let r = 0; r < rows; r += 1) {
        let best = 'void';
        for (let c = from; c < to; c += 1) {
          const kind = benchState(grid[c]?.[r]);
          if (RANK[kind] > RANK[best]) best = kind;
        }
        ctx.fillStyle = paint[best];
        ctx.fillRect(bucket * colWidth, r * rowHeight, Math.ceil(colWidth), Math.ceil(rowHeight));
      }
    }

    // The playhead stays the highest-contrast mark, whatever is behind it.
    const x = (current / Math.max(1, columns - 1)) * (width - 2);
    ctx.fillStyle = read('--probe');
    ctx.fillRect(x, 0, 2, height);
  }, [grid, rows, columns, current]);

  const seekFromEvent = (event) => {
    const box = event.currentTarget.getBoundingClientRect();
    const ratio = (event.clientX - box.left) / box.width;
    onSeek(Math.round(ratio * (columns - 1)));
  };

  return (
    <div
      className="cs-band"
      role="slider"
      tabIndex={0}
      aria-label="Execution capture: seek through the trace"
      aria-valuemin={1}
      aria-valuemax={columns}
      aria-valuenow={current + 1}
      onClick={seekFromEvent}
      onKeyDown={(event) => {
        if (event.key === 'ArrowLeft') onSeek(current - 1);
        if (event.key === 'ArrowRight') onSeek(current + 1);
        if (event.key === 'Home') onSeek(0);
        if (event.key === 'End') onSeek(columns - 1);
      }}
    >
      <canvas ref={canvasRef} className="cs-canvas" />
    </div>
  );
}
